package benchly;

import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import base.workbench.ModuleWorkbenchController;
import benchly.controller.IndexController;
import benchly.controller.UserController;
import benchly.controller.WorkflowController;
import benchly.error.Handlers;
import benchly.error.InternalServerError;
import benchly.error.InvalidModelException;
import benchly.error.ResourceNotFoundError;
import benchly.util.Filters;
import benchly.util.Path;
import modules.Module;

public class Benchly {

	private static final Logger LOG = LoggerFactory.getLogger(Benchly.class);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static void main(String[] args) {

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

		get(Path.Web.LOGIN, UserController.showLogin);
		post(Path.Web.LOGIN, UserController.login);
		get(Path.Web.LOGOUT, UserController.logout);

		get("modules", "application/json", (request, response) -> {

			ModuleWorkbenchController controller = new ModuleWorkbenchController();
			Map<String, Module> modules = controller.getAvailableModules();

			List<ModuleProfile> profiles = modules.values().stream().map((Module m) -> new ModuleProfile(m))
					.collect(Collectors.toList());

			response.type("application/json");
			if (profiles == null || profiles.isEmpty()) {
				LOG.error("No modules could be loaded.");
				return null;
			} else {
				return profiles;
			}
		}, GSON::toJson);
		
		// after a page visit that location is logged in the session
		after("*", Filters.markGetRequestLocationInSession);

		// Exceptions that are caught from the routes trigger redirecting or other
		// behaviour that is mapped here
		exception(ResourceNotFoundError.class, Handlers.resourceNotFound);
		exception(InternalServerError.class, Handlers.internalError);
		exception(InvalidModelException.class, Handlers.invalidModel);
		exception(SQLException.class, Handlers.sqlException);
	}

}
