package benchly;

import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

import java.sql.SQLException;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.controller.IndexController;
import benchly.controller.JobController;
import benchly.controller.UserController;
import benchly.controller.WorkflowController;
import benchly.error.Handlers;
import benchly.error.InternalServerError;
import benchly.error.InvalidModelException;
import benchly.error.ResourceNotFoundError;
import benchly.util.Filters;
import benchly.util.Path;

public class Benchly {

	private static final Logger LOG = LoggerFactory.getLogger(Benchly.class);

	public static void main(String[] args) {

		// start a single JobQueue responsible for starting jobs
		Thread jobQueueThread = new Thread(new JobQueue());
		jobQueueThread.start();

		// let spark serve the folder: src/main/resources/public
		// TODO: Set caching policies for this
		staticFiles.location("/public");

		// DebugScreen.enableDebugScreen();

		// Initialise the Shiro security manager
		final SecurityManager securityManager = (new IniSecurityManagerFactory("classpath:shiro.ini")).createInstance();
		SecurityUtils.setSecurityManager(securityManager);

		before("/*/", Filters.removeTrailingSlashes);

		get("/", IndexController.serveIndexPage);
		get("/index/", IndexController.serveIndexPage);

		get(Path.Web.WORKFLOWS, WorkflowController.index);
		get(Path.Web.NEW_WORKLFOW, WorkflowController.newWorkflow);
		get(Path.Web.SHOW_WORKFLOW, WorkflowController.show);
		get(Path.Web.EDIT_WORKFLOW, WorkflowController.edit);
		get(Path.Web.DELETE_WORKFLOW, WorkflowController.delete);
		get(Path.Web.SHOW_WORKFLOW_VERSION, WorkflowController.showVersion);
		post(Path.Web.WORKFLOWS, WorkflowController.create);
		post(Path.Web.UPDATE_WORKFLOW, WorkflowController.update);
		post(Path.Web.DESTROY_WORKFLOW, WorkflowController.destroy);

		get(Path.Web.NEW_JOB, JobController.newJob);
		get(Path.Web.SHOW_JOB, JobController.show);
		post(Path.Web.JOBS, JobController.create);

		get(Path.Web.LOGIN, UserController.showLogin);
		post(Path.Web.LOGIN, UserController.login);
		get(Path.Web.LOGOUT, UserController.logout);

		// after a page visit that location is logged in the session
		after("*", Filters.markGetRequestLocationInSession);

		// Exceptions that are caught from the routes trigger redirecting or other
		// behaviour that is mapped here
		exception(ResourceNotFoundError.class, Handlers.resourceNotFound);
		exception(InternalServerError.class, Handlers.internalError);
		exception(InvalidModelException.class, Handlers.invalidModel);
		exception(SQLException.class, Handlers.sqlException);

		LOG.debug("Startup finished.");
	}

}
