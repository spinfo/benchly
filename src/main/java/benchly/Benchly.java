package benchly;

import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.staticFiles;

import java.sql.SQLException;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.controller.ErrorHandlers;
import benchly.controller.JobController;
import benchly.controller.SessionController;
import benchly.controller.WorkflowController;
import benchly.database.TestEntrySetup;
import benchly.error.InternalServerError;
import benchly.error.InvalidModelException;
import benchly.error.InvalidRequestException;
import benchly.error.ResourceNotFoundError;

public class Benchly {

	private static final Logger LOG = LoggerFactory.getLogger(Benchly.class);

	public static void main(String[] args) {

		// start a single JobQueue responsible for starting jobs
		Thread jobQueueThread = new Thread(new JobQueue());
		jobQueueThread.start();

		// let spark serve the folder: src/main/resources/public
		// TODO: Set caching policies for this
		staticFiles.location("/public");

		// Initialise the Shiro security manager
		final SecurityManager securityManager = (new IniSecurityManagerFactory("classpath:shiro.ini")).createInstance();
		SecurityUtils.setSecurityManager(securityManager);
		
		// TODO: Remove
		TestEntrySetup.setup();

		path("/api/v1", () -> {

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
			});
			
			path("/jobs", () -> {
				post("", JobController.create);
				get("/:id", JobController.show);
			});

		});

		// Exceptions that are caught from the routes trigger redirecting or other
		// behaviour that is mapped here
		exception(ResourceNotFoundError.class, ErrorHandlers.resourceNotFound);
		exception(InternalServerError.class, ErrorHandlers.internalError);
		exception(InvalidModelException.class, ErrorHandlers.invalidModel);
		exception(InvalidRequestException.class, ErrorHandlers.invalidRequest);
		exception(SQLException.class, ErrorHandlers.sqlException);

		LOG.debug("Startup finished.");
	}

}
