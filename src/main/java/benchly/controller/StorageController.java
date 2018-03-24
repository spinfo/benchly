package benchly.controller;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.BenchlyScheduler;
import benchly.database.StorageDao;
import benchly.error.InvalidModelException;
import benchly.error.InvalidRequestException;
import benchly.error.ResourceNotFoundError;
import benchly.error.StorageAccessError;
import benchly.model.StorageConfig;
import benchly.model.StorageFileMeta;
import benchly.model.User;
import benchly.remote.StorageAccess;
import benchly.remote.StorageConfigRefreshTask;
import benchly.util.RequestUtil;
import spark.Request;
import spark.Route;
import spark.utils.StringUtils;

public class StorageController extends Controller {

	private static Logger LOG = LoggerFactory.getLogger(StorageController.class);

	public static Route index = (request, response) -> {
		// no pagination option on purpose as we do not expect users to possess that
		// many storage configurations and this only shows an index on a per-user basis
		User user = ensureLoggedInUser(request, "Only registered users may view storage configurations.");
		List<StorageConfig> configs = StorageDao.fetchAcessible(user);
		return JsonTransformer.render(configs, request);
	};

	public static Route show=(request,response)->{User user=ensureLoggedInUser(request,"Only registered users may view storage configurations.");StorageConfig config=ensureStorageConfigFromRequest(request);ensureUserMayAccessConfig(user,config,request);

	// if the refresh param is set to "true", fetch file information for the storage
	// configuration
	if(Boolean.parseBoolean(request.queryParams("refresh"))){LOG.debug("Explicit request for file meta registered for config: "+config.getId());

	try{BenchlyScheduler.get().submit(new StorageConfigRefreshTask(config)).get();}catch(
	ExecutionException e)
	{
		throw new StorageAccessError("Unable to refresh storage file meta.", e.getCause());
	}

	config=StorageDao.fetchConfig(config.getId());}
	// only if the parameter credential is set, generate an encrypted credential to
	// show
	if(Boolean.parseBoolean(request.queryParams("credential"))){config.generateNewEncryptedCredential();}

	return JsonTransformer.render(config,request);};

	public static Route create = (request, response) -> {
		User user = ensureLoggedInUser(request, "Only registered users may create storage configurations.");

		StorageConfig config = JsonTransformer.readRequestBody(request.body(), StorageConfig.class);
		config.setOwner(user);

		if (!config.validate()) {
			throw new InvalidModelException(config);
		}
		long rowCount = StorageDao.create(config);
		ensureRowCountIsOne(rowCount, "create storage configuration");

		return JsonTransformer.render(config, request);
	};

	public static Route update = (request, response) -> {
		User user = ensureLoggedInUser(request, "Only registered users may modify storage configurations.");

		StorageConfig target = ensureStorageConfigFromRequest(request);
		ensureUserMayEditConfig(user, target, request);

		StorageConfig input = JsonTransformer.readRequestBody(request.body(), StorageConfig.class);

		// only the credentials may actually be changed on a storage config
		target.setCredential(input.getCredential());
		if (!target.validate()) {
			throw new InvalidModelException(target);
		}
		long rowCount = StorageDao.update(target);
		ensureRowCountIsOne(rowCount, "update storage configuration");

		return JsonTransformer.render(target, request);
	};

	public static Route destroy = (request, response) -> {
		User user = ensureLoggedInUser(request, "Only registered users may delete storage configurations.");
		StorageConfig config = ensureStorageConfigFromRequest(request);

		ensureUserMayEditConfig(user, config, request);

		int rowCount = StorageDao.delete(config);
		ensureRowCountIsOne(rowCount, "delete storage configuration");

		return JsonTransformer.render(config, request);
	};

	public static Route showFileMeta = (request, response) -> {
		StorageConfig config = ensureStorageConfigWithFileAccess(request);

		StorageFileMeta fileMeta = ensureFileMetaWithConfigFromRequest(config, request);
		return JsonTransformer.render(fileMeta, request);
	};

	public static Route downloadFile = (request, response) -> {
		StorageConfig config = ensureStorageConfigWithFileAccess(request);
		StorageFileMeta fileMeta = ensureFileMetaWithConfigFromRequest(config, request);

		response.raw().setHeader("Content-Disposition", "attachment; filename=" + fileMeta.getName());

		StorageAccess.getInstance().streamFileToResponse(config, fileMeta, response.raw());
		return response.raw();
	};

	public static Route uploadFile = (request, response) -> {
		StorageConfig config = ensureStorageConfigWithFileAccess(request);

		setupMultipartConfig(request);
		
		Part filePart = null;
		StorageFileMeta fileMeta = null;
		try {
			filePart = request.raw().getPart("upload");
			if (filePart == null) {
				throw new InvalidRequestException("No file part specified for expected mulitpart upload.");
			}
			String newFileName = filePart.getSubmittedFileName();
			if (StringUtils.isBlank(newFileName)) {
				newFileName = "unknown-file-name";
			}
			fileMeta = StorageAccess.getInstance().streamToNewFile(config, newFileName,
					filePart.getInputStream());
		} finally {
			if (filePart != null) {
				filePart.delete();
			}
		}

		fileMeta.setLastModified(Date.from(Instant.now()));
		StorageDao.create(fileMeta);

		return JsonTransformer.render(fileMeta, request);
	};

	public static Route destroyFile = (request, response) -> {
		StorageConfig config = ensureStorageConfigWithFileAccess(request);
		StorageFileMeta fileMeta = ensureFileMetaWithConfigFromRequest(config, request);

		StorageAccess.getInstance().deleteFile(config, fileMeta);

		// TODO: Initialise a deferred refresh of the config's files

		return JsonTransformer.render(fileMeta, request);
	};

	private static StorageConfig ensureStorageConfigFromRequest(Request request)
			throws SQLException, ResourceNotFoundError {
		long id = RequestUtil.parseIdParam(request);
		StorageConfig config = StorageDao.fetchConfig(id);

		if (config == null) {
			throw new ResourceNotFoundError("No storage configuration with id: '" + id + "'");
		}
		return config;
	}

	private static StorageFileMeta ensureFileMetaWithConfigFromRequest(StorageConfig config, Request request)
			throws SQLException, ResourceNotFoundError {
		long fileId = RequestUtil.parseIdParam(request, ":fileId");
		StorageFileMeta fileMeta = StorageDao.fetchFileMeta(config, fileId);
		if (fileMeta == null) {
			String msg = "No file information present for this storage configuration and fileId. (config: %d, file: %d)";
			throw new ResourceNotFoundError(String.format(msg, config.getId(), fileId));
		}
		return fileMeta;
	}

	private static StorageConfig ensureStorageConfigWithFileAccess(Request request)
			throws SQLException, ResourceNotFoundError {
		User user = ensureLoggedInUser(request, "Only registered users may access files.");
		StorageConfig config = ensureStorageConfigFromRequest(request);
		ensureUserMayAccessConfig(user, config, request);
		return config;
	}

	private static void ensureUserMayAccessConfig(User user, StorageConfig config, Request request)
			throws SQLException {
		if (!user.isAdmin() && !userOwnsConfig(user, config) && !StorageDao.userHasAccess(user, config)) {
			haltForbbiden(request, "You have insufficient permissions to view this storage configuration.");
		}
	}

	private static void ensureUserMayEditConfig(User user, StorageConfig config, Request request) {
		if (!user.isAdmin() && !userOwnsConfig(user, config)) {
			haltForbbiden(request, "You have insufficient permissions to modify this storage configuration.");
		}
	}

	private static boolean userOwnsConfig(User user, StorageConfig config) {
		return user.getId() == config.getOwner().getId();
	}

	private static void setupMultipartConfig(Request request) {
		// TODO: Change this to another directory under our control only
		String location = "/tmp/";
		long maxFileSize = 1000000000;
		long maxRequestSize = 1000000000;
		int fileSizeThreshold = 1024;

		MultipartConfigElement multipartConfigElement = new MultipartConfigElement(location, maxFileSize, maxRequestSize,
				fileSizeThreshold);
		request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
	}

}
