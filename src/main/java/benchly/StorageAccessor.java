package benchly;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.MultipartPart;
import org.jclouds.blobstore.domain.MultipartUpload;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.blobstore.options.PutOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.error.StorageAccessError;
import benchly.model.StorageConfig;
import benchly.model.StorageFileMeta;

// TODO: This is messy and lends itself to some refactoring
// TODO: Should storage access be synchronised such that each storage config may only be accessed on one thread?
public class StorageAccessor {

	private static final Logger LOG = LoggerFactory.getLogger(StorageAccessor.class);

	private static StorageAccessor instance = null;

	private Map<Long, BlobStoreContext> createdContexts;

	// private constructor for the singleton pattern
	private StorageAccessor() {
		createdContexts = Collections.synchronizedMap(new HashMap<>());
	}

	// return the singleton instance
	public static StorageAccessor getInstance() {
		if (instance == null) {
			instance = new StorageAccessor();
		}
		return instance;
	}

	public boolean streamFileToResponse(StorageConfig config, StorageFileMeta fileMeta, HttpServletResponse response) {
		BlobStoreContext context = getOrCreateBlobStoreContext(config);

		BlobStore blobStore = context.getBlobStore();

		BlobMetadata blobMeta = blobStore.blobMetadata(config.getContainer(), fileMeta.getName());
		try {
			int contentSize = Math.toIntExact(blobMeta.getSize());
			response.setContentLength(contentSize);
		} catch (ArithmeticException e) {
			// do nothing, just leave the response content length empty,
		}

		// Note: Blob.getBlob() does connect to the storage provider, but it will not
		// download all of the data.
		long beginBlob = System.currentTimeMillis();
		Blob blob = blobStore.getBlob(config.getContainer(), fileMeta.getName());
		LOG.debug("getBlob() took: " + (System.currentTimeMillis() - beginBlob) + " ms");

		long beginStream = System.currentTimeMillis();
		try (InputStream in = blob.getPayload().openStream(); OutputStream out = response.getOutputStream();) {
			byte[] buffer = new byte[2048];
			int count = in.read(buffer);
			while (count > 0) {
				out.write(Arrays.copyOf(buffer, count));
				count = in.read(buffer);
			}
			LOG.debug("streaming took: " + (System.currentTimeMillis() - beginStream) + " ms");
		} catch (IOException e) {
			LOG.error("IOException: " + e.getMessage());
			e.printStackTrace();
		}

		return true;
	}

	public StorageFileMeta streamToNewFile(StorageConfig config, String fileName, InputStream in)
			throws StorageAccessError {
		BlobStore blobStore = getOrCreateBlobStoreContext(config).getBlobStore();

		// setup a blob to use for the upload
		Blob blob = blobStore.blobBuilder(fileName).build();

		// TODO: find out how we can catch exceptions like
		// "org.jclouds.aws.AWSResponseException"

		int chunkSize = determineUploadChunkSize(blobStore, 5000000);
		try {
			byte[] part1 = tryToReadAtLeastNBytes(in, chunkSize);
			byte[] part2 = tryToReadAtLeastNBytes(in, chunkSize);

			// no multipart upload is needed, push everything in one go.
			if (part2.length == 0) {
				blob.setPayload(part1);
				blobStore.putBlob(config.getContainer(), blob);
				return new StorageFileMeta(config, fileName, part1.length);
			}

			// a multipart upload is indeed needed, so let's do that
			LOG.debug("Beginning multipart upload with chunk size: " + chunkSize);
			MultipartUpload mpu = blobStore.initiateMultipartUpload(config.getContainer(), blob.getMetadata(),
					new PutOptions(true));

			// the list of uploaded payload parts
			List<MultipartPart> parts = new ArrayList<>();
			parts.add(uploadChunk(part1, 1, blob, blobStore, mpu));
			parts.add(uploadChunk(part2, 2, blob, blobStore, mpu));

			// read the next input and upload any additional parts
			long maxParts = blobStore.getMaximumNumberOfParts();
			long totalSize = part1.length + part2.length;
			while (true) {
				if (parts.size() >= maxParts) {
					blobStore.abortMultipartUpload(mpu);
					throw new StorageAccessError("Aborting upload. Maximum number of parts reached: " + maxParts);
				}
				byte[] nextPayload = tryToReadAtLeastNBytes(in, chunkSize);
				if (nextPayload.length == 0) {
					break;
				}
				MultipartPart part = uploadChunk(nextPayload, parts.size() + 1, blob, blobStore, mpu);
				parts.add(part);
				totalSize += part.partSize();
			}
			String eTag = blobStore.completeMultipartUpload(mpu, parts);
			LOG.debug("Multipart upload finished, eTag: " + eTag);
			return new StorageFileMeta(config, fileName, totalSize);
		} catch (IOException e) {
			throw new StorageAccessError(e);
		}
	}

	private BlobStoreContext getOrCreateBlobStoreContext(StorageConfig config) {
		// We need the synchronisation on the (already synchronised) map here, because
		// the access is non-atomic. This is potentially long and blocking, but has to
		// happen only a small number of times
		synchronized (this.createdContexts) {
			if (!this.createdContexts.containsKey(config.getId())) {
				ContextBuilder builder = ContextBuilder.newBuilder(config.getProvider().toString());
				builder.credentials(config.getIdentity(), config.getCredential());
				if (!StringUtils.isBlank(config.getEndpoint())) {
					builder.endpoint(config.getEndpoint());
				}

				BlobStoreContext context = builder.buildView(BlobStoreContext.class);
				this.createdContexts.put(config.getId(), context);
			}
			return this.createdContexts.get(config.getId());
		}
	}

	public synchronized Set<StorageFileMeta> getFilesMeta(StorageConfig config) {
		BlobStoreContext context = getOrCreateBlobStoreContext(config);
		BlobStore store = context.getBlobStore();

		PageSet<? extends StorageMetadata> pages = store.list(config.getContainer());
		Set<StorageFileMeta> ourMetadata = new HashSet<>();
		while (true) {
			LOG.info(pages.getClass().getName());
			for (StorageMetadata page : pages) {
				LOG.info(page.toString());
				ourMetadata.add(StorageFileMeta.from(config, page));
			}

			// retrieve pages from the next marker
			String marker = pages.getNextMarker();
			if (marker != null) {
				pages = store.list(config.getContainer(), ListContainerOptions.Builder.afterMarker(marker));
			} else {
				break;
			}
		}
		context.close();
		return ourMetadata;
	}

	public void deleteFile(StorageConfig config, StorageFileMeta fileMeta) throws StorageAccessError {
		BlobStoreContext context = getOrCreateBlobStoreContext(config);
		try {
			context.getBlobStore().removeBlob(config.getContainer(), fileMeta.getName());
		} catch (ContainerNotFoundException e) {
			throw new StorageAccessError(e);
		}
	}

	private int determineUploadChunkSize(BlobStore blobStore, int suggestedSize) throws StorageAccessError {
		long result = (long) suggestedSize;

		long min = blobStore.getMinimumMultipartPartSize();
		if (suggestedSize < min) {
			result = min;
		} else {
			long max = blobStore.getMaximumMultipartPartSize();
			if (suggestedSize > max) {
				result = max;
			}
		}

		if (result <= 0 || result > Integer.MAX_VALUE) {
			throw new StorageAccessError("Cannot safely determine the multipart chunk size for file upload.");
		} else {
			return (int) result;
		}
	}

	private byte[] tryToReadAtLeastNBytes(InputStream in, int n) throws IOException {
		int singleReadMax = 1000;
		byte[] result = new byte[n + singleReadMax];
		int total = 0;

		while (true) {
			int count = in.read(result, total, singleReadMax);

			if (count == -1) {
				break;
			}

			total += count;

			if (total >= n) {
				break;
			}
		}

		return Arrays.copyOf(result, total);
	}

	private MultipartPart uploadChunk(byte[] nextPayload, int partNr, Blob blob, BlobStore blobStore,
			MultipartUpload mpu) {
		blob.setPayload(nextPayload);
		// attempt the multipart upload
		MultipartPart part = blobStore.uploadMultipartPart(mpu, partNr, blob.getPayload());
		return part;
	}

}
