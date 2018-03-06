package benchly;

import static spark.Spark.afterAfter;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.post;
import static spark.Spark.put;

import java.sql.SQLException;
import java.util.concurrent.Executors;
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
import benchly.controller.SessionController;
import benchly.controller.StorageController;
import benchly.controller.UserController;
import benchly.controller.WorkflowController;
import benchly.database.TestEntrySetup;
import benchly.error.InternalServerError;
import benchly.error.InvalidModelException;
import benchly.error.InvalidRequestException;
import benchly.error.ResourceNotFoundError;
import spark.Route;

public class Benchly {

	private static final Logger LOG = LoggerFactory.getLogger(Benchly.class);

	// a route that can be mapped to all request not handled by other Routes
	private static Route notFoundRoute = (request, response) -> {
		String msg = String.format("404 Not found: %s %s", request.requestMethod(), request.pathInfo());
		throw new ResourceNotFoundError(msg);
	};

	// an executor service that will run periodic tasks for us
	private static ScheduledExecutorService taskScheduler;

	public static void main(String[] args) {

		// initialize the scheduler with the periodic tasks
		taskScheduler = Executors.newScheduledThreadPool(50);

		// schedule a warcher to regularly check up on the servers connected to us
		taskScheduler.scheduleAtFixedRate(new ServerContactWatcher(taskScheduler, 120), 5, 10, TimeUnit.SECONDS);

		// schedule watchers to periodically check up on runninh jobs or jobs that
		// should be started
		taskScheduler.scheduleAtFixedRate(new JobScheduler(taskScheduler, 500), 10, 10, TimeUnit.SECONDS);
		taskScheduler.scheduleAtFixedRate(new JobWatcher(taskScheduler, 60), 15, 10, TimeUnit.SECONDS);

		// Initialise the Shiro security manager
		final SecurityManager securityManager = (new IniSecurityManagerFactory("classpath:shiro.ini")).createInstance();
		SecurityUtils.setSecurityManager(securityManager);

		// TODO: Remove
		TestEntrySetup.setup();

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
				post("", JobController.create);
				get("/:id", JobController.show);
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
					post("/:fileId", StorageController.replaceFile);
					delete("/:fileId", StorageController.destroyFile);
				});
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

}
