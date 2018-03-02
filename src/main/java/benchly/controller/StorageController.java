package benchly.controller;

import java.io.InputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.StorageAccessor;
import benchly.database.StorageDao;
import benchly.error.InvalidModelException;
import benchly.error.ResourceNotFoundError;
import benchly.model.StorageConfig;
import benchly.model.StorageFileMeta;
import benchly.model.User;
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

	public static Route show = (request, response) -> {
		User user = ensureLoggedInUser(request, "Only registered users may view storage configurations.");
		StorageConfig config = ensureStorageConfigFromRequest(request);
		ensureUserMayAccessConfig(user, config, request);

		// if the refresh param is set to "true", fetch file information for the storage
		// configuration
		if (Boolean.parseBoolean(request.queryParams("refresh"))) {
			LOG.info("Explicit request for file meta registered for config: " + config.getId());
			Set<StorageFileMeta> fileMeta = StorageAccessor.getInstance().getFilesMeta(config);
			StorageDao.updateStorageFileMeta(config, fileMeta);
			config = StorageDao.fetchConfig(config.getId());
		}
		// only if the parameter credential is set, generate an encrypted credential to
		// show
		if (Boolean.parseBoolean(request.queryParams("credential"))) {
			config.generateNewEncryptedCredential();
		}

		return JsonTransformer.render(config, request);
	};

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

		StorageAccessor.getInstance().streamFileToResponse(config, fileMeta, response.raw());
		return 200;
	};

	public static Route uploadFile = (request, response) -> {
		StorageConfig config = ensureStorageConfigWithFileAccess(request);

		// the filename should be given in the query params
		String fileName = request.queryParams("fileName");
		if (StringUtils.isBlank(fileName)) {
			fileName = "new-file";
		}

		InputStream in = request.raw().getInputStream();
		StorageFileMeta fileMeta = StorageAccessor.getInstance().streamToNewFile(config, fileName, in);
		
		// TODO: Initialise a deferred refresh of the config's files instead?
		fileMeta.setLastModified(Date.from(Instant.now()));
		StorageDao.create(fileMeta);

		return JsonTransformer.render(fileMeta, request);
	};
	
	public static Route replaceFile = (request, response) -> {
		StorageConfig config = ensureStorageConfigWithFileAccess(request);
		StorageFileMeta fileMeta = ensureFileMetaWithConfigFromRequest(config, request);
		
		InputStream in = request.raw().getInputStream();
		StorageFileMeta newMeta = StorageAccessor.getInstance().streamToNewFile(config, fileMeta.getName(), in);
		
		// TODO: Initialise a deferred refresh of the config's files instead?
		fileMeta.setSize(newMeta.getSize());
		fileMeta.setRetrievedAt(newMeta.getRetrievedAt());
		fileMeta.setLastModified(Date.from(Instant.now()));
		StorageDao.update(fileMeta);
		
		return JsonTransformer.render(fileMeta, request);
	};
	
	public static Route destroyFile = (request, response) -> {
		StorageConfig config = ensureStorageConfigWithFileAccess(request);
		StorageFileMeta fileMeta = ensureFileMetaWithConfigFromRequest(config, request);
		
		StorageAccessor.getInstance().deleteFile(config, fileMeta);
		// TODO: Initialise a deferred refresh of the config's files instead?
		StorageDao.delete(config);
		
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
	
	private static StorageConfig ensureStorageConfigWithFileAccess(Request request) throws SQLException, ResourceNotFoundError {
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

}
