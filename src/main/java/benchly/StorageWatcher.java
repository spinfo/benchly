package benchly;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.model.StorageConfig;
import benchly.model.StorageFileMeta;

public class StorageWatcher {

	private static final Logger LOG = LoggerFactory.getLogger(StorageWatcher.class);

	private static StorageWatcher instance = null;

	// private constructor for the singleton pattern
	private StorageWatcher() {
	}

	// return the singleton instance
	public static StorageWatcher getInstance() {
		if (instance == null) {
			instance = new StorageWatcher();
		}
		return instance;
	}

	private synchronized BlobStoreContext createBlobStoreContext(StorageConfig config) {
		ContextBuilder builder = ContextBuilder.newBuilder(config.getProvider().toString());
		builder.credentials(config.getIdentity(), config.getCredential());
		if (!StringUtils.isBlank(config.getEndpoint())) {
			builder.endpoint(config.getEndpoint());
		}
		return builder.buildView(BlobStoreContext.class);
	}

	public synchronized Set<StorageFileMeta> getFilesMeta(StorageConfig config) {
		BlobStoreContext context = createBlobStoreContext(config);
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

}
