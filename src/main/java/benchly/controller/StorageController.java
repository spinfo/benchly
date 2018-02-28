package benchly.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.StorageWatcher;
import benchly.database.StorageConfigDao;
import benchly.error.InvalidModelException;
import benchly.error.ResourceNotFoundError;
import benchly.model.StorageConfig;
import benchly.model.StorageFileMeta;
import benchly.model.User;
import benchly.util.RequestUtil;
import spark.Request;
import spark.Route;

public class StorageController extends Controller {

	private static Logger LOG = LoggerFactory.getLogger(StorageController.class);

	public static Route index = (request, response) -> {
		// no pagination option on purpose as we do not expect users to possess that
		// many storage configurations and this only shows an index on a per-user basis
		User user = ensureLoggedInUser(request, "Only registered users may view storage configurations.");
		List<StorageConfig> configs = StorageConfigDao.fetchAcessible(user);
		return JsonTransformer.render(configs, request);
	};

	public static Route show = (request, response) -> {
		User user = ensureLoggedInUser(request, "Only registered users may view storage configurations.");
		StorageConfig config = ensureStorageConfigFromRequest(request);
		ensureUserMayViewCofnig(user, config, request);

		// if the refresh param is set to "true", fetch file information for the storage
		// configuration
		if (Boolean.parseBoolean(request.queryParams("refresh"))) {
			LOG.info("Explicit request for file meta registered for config: " + config.getId());
			Set<StorageFileMeta> fileMeta = StorageWatcher.getInstance().getFilesMeta(config);
			StorageConfigDao.updateStorageFileMeta(config, fileMeta);
			config = StorageConfigDao.fetchConfig(config.getId());
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
		long rowCount = StorageConfigDao.create(config);
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
		long rowCount = StorageConfigDao.update(target);
		ensureRowCountIsOne(rowCount, "update storage configuration");

		return JsonTransformer.render(target, request);
	};

	public static Route destroy = (request, response) -> {
		User user = ensureLoggedInUser(request, "Only registered users may delete storage configurations.");
		StorageConfig config = ensureStorageConfigFromRequest(request);

		ensureUserMayEditConfig(user, config, request);

		int rowCount = StorageConfigDao.delete(config);
		ensureRowCountIsOne(rowCount, "delete storage configuration");

		return JsonTransformer.render(config, request);
	};

	private static StorageConfig ensureStorageConfigFromRequest(Request request)
			throws SQLException, ResourceNotFoundError {
		long id = RequestUtil.parseIdParam(request);
		StorageConfig config = StorageConfigDao.fetchConfig(id);

		if (config == null) {
			throw new ResourceNotFoundError("No storage configuration with id: '" + id + "'");
		}
		return config;
	}

	private static void ensureUserMayViewCofnig(User user, StorageConfig config, Request request) throws SQLException {
		if (!user.isAdmin() && !userOwnsConfig(user, config) && !StorageConfigDao.userHasAccess(user, config)) {
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
