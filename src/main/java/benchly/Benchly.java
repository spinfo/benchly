package benchly;

import static spark.Spark.afterAfter;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.staticFiles;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.controller.ErrorHandlers;
import benchly.controller.JobController;
import benchly.controller.ServerContactController;
import benchly.controller.SessionController;
import benchly.controller.StorageController;
import benchly.controller.UserController;
import benchly.controller.WorkflowController;
import benchly.error.InternalServerError;
import benchly.error.InvalidModelException;
import benchly.error.InvalidRequestException;
import benchly.error.ResourceNotFoundError;
import spark.Route;

public class Benchly {

	private static final Logger LOG = LoggerFactory.getLogger(Benchly.class);

	private static final String SHARED_SECRET_ENV_NAME = "BENCHLY_SHARED_SECRET";

	// a route that can be mapped to all request not handled by other Routes
	private static Route notFoundRoute = (request, response) -> {
		String msg = String.format("404 Not found: %s %s", request.requestMethod(), request.pathInfo());
		throw new ResourceNotFoundError(msg);
	};

	// an executor service that will run periodic tasks for us
	private static ScheduledExecutorService taskScheduler;

	// the url used for database connection
	private static String jdbcUrl;

	public static void main(String[] args) {

		// read options from the command line, fail if any option is invalid
		Config config = Config.from(args);
		List<String> configErrors = config.checkForErrors();
		if (!configErrors.isEmpty()) {
			for (String error : configErrors) {
				LOG.error(error);
			}
			System.exit(1);
		}

		// set the database url for later use
		jdbcUrl = config.getJdbcUrl();

		// initialise the scheduler with periodic tasks
		taskScheduler = BenchlyScheduler.get();

		// schedule a watcher to regularly check up on the servers connected to us
		taskScheduler.scheduleAtFixedRate(new ServerContactWatcher(taskScheduler, 120), 5, 10, TimeUnit.SECONDS);

		// schedule a watcher to periodically check up on storage and refresh it
		taskScheduler.scheduleAtFixedRate(new StorageWatcher(taskScheduler, (30 * 60)), 3, 5, TimeUnit.SECONDS);

		// schedule watchers to periodically check up on running jobs or jobs that
		// should be started
		taskScheduler.scheduleAtFixedRate(new JobScheduler(taskScheduler, 500), 10, 10, TimeUnit.SECONDS);
		taskScheduler.scheduleAtFixedRate(new JobWatcher(taskScheduler, 30), 15, 10, TimeUnit.SECONDS);

		// Initialise the Shiro security manager
		final SecurityManager securityManager = (new IniSecurityManagerFactory("classpath:shiro.ini")).createInstance();
		SecurityUtils.setSecurityManager(securityManager);

		// Setup serving the frontend if requested to do so
		if (config.frontendWasRequested()) {
			LOG.info("Serving the frontend from: " + config.getFrontendPath());

			// NOTE: Apparently this has to be in front of any other call to spark.Spark.*;
			staticFiles.externalLocation(config.getFrontendPath());
		}

		path("/api/v1", () -> {

			path("/users", () -> {
				post("", UserController.create);
				get("", UserController.index);
				get("/:id", UserController.show);
				put("/:id", UserController.update);
				delete("/:id", UserController.destroy);
			});

			path("/session", () -> {
				get("", SessionController.showLoggedInUser);
				post("", SessionController.login);
				delete("", SessionController.logout);
			});

			path("/workflows", () -> {
				get("", WorkflowController.index);
				post("", WorkflowController.create);
				get("/:uuid", WorkflowController.show);
				put("/:uuid", WorkflowController.update);
				get("/:uuid/version/:id", WorkflowController.showVersion);
				delete("/:uuid", WorkflowController.destroy);
			});

			path("/jobs", () -> {
				get("", JobController.index);
				post("", JobController.create);
				get("/:id", JobController.show);
				delete("/:id", JobController.cancel);
			});

			path("/storage", () -> {
				post("", StorageController.create);
				get("", StorageController.index);
				get("/:id", StorageController.show);
				put("/:id", StorageController.update);
				delete("/:id", StorageController.destroy);

				path("/:id/files", () -> {
					post("", StorageController.uploadFile);
					get("/:fileId", StorageController.showFileMeta);
					get("/:fileId/download", StorageController.downloadFile);
					delete("/:fileId", StorageController.destroyFile);
				});
			});

			path("/server_contacts", () -> {
				post("", ServerContactController.create);
				get("", ServerContactController.index);
				get("/:id", ServerContactController.show);
				put("/:id", ServerContactController.update);
				get("/:id/reports", ServerContactController.indexReports);
			});
		});

		get("*", notFoundRoute);
		put("*", notFoundRoute);
		post("*", notFoundRoute);
		delete("*", notFoundRoute);

		// All the api routes return json (even on exception) except for the files
		// download route which might return different results based on the
		afterAfter("*", (request, response) -> {
			if (StringUtils.isBlank(response.type())) {
				response.type("application/json");
			}
		});

		// Exceptions that are caught from the routes trigger redirecting or other
		// behaviour that is mapped here
		exception(ResourceNotFoundError.class, ErrorHandlers.resourceNotFound);
		exception(InternalServerError.class, ErrorHandlers.internalError);
		exception(InvalidModelException.class, ErrorHandlers.invalidModel);
		exception(InvalidRequestException.class, ErrorHandlers.invalidRequest);
		exception(SQLException.class, ErrorHandlers.sqlException);

		exception(Exception.class, ErrorHandlers.internalError);

		LOG.debug("Startup finished.");
	}

	public static boolean sharedSecretIsAvailable() {
		return StringUtils.isNotEmpty(readSharedSecret());
	}

	public static String readSharedSecret() {
		return System.getenv(SHARED_SECRET_ENV_NAME);
	}

	public static String getJdbcUrl() {
		return jdbcUrl;
	}

}
