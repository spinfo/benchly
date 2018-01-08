package benchly.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.WorkflowDao;
import benchly.error.InternalServerError;
import benchly.error.InvalidModelException;
import benchly.error.ResourceNotFoundError;
import benchly.model.Model;
import benchly.model.User;
import benchly.model.Workflow;
import benchly.util.PaginationLinksBuilder;
import benchly.util.Path;
import benchly.util.RequestUtil;
import benchly.util.RequestUtil.PaginationParams;
import benchly.util.SessionUtil;
import benchly.util.Views;
import spark.Request;
import spark.Route;

public class WorkflowController extends Controller {

	private static final Logger LOG = LoggerFactory.getLogger(WorkflowController.class);

	public static Route index = (request, response) -> {
		PaginationParams pagination = RequestUtil.parsePaginationParams(request);

		List<Workflow> workflows = WorkflowDao.fetchLatestVersions(pagination);
		Map<String, Object> context = Views.newContextWith("workflows", workflows);

		long amount = WorkflowDao.getCountOfLatestVersions();
		PaginationLinksBuilder paginationLinksBuilder = new PaginationLinksBuilder(request, pagination, amount);
		context.put("paginationLinksBuilder", paginationLinksBuilder);

		return Views.render(request, context, Path.Template.WORKFLOWS);
	};

	public static Route newWorkflow = (request, response) -> {
		return Views.render(request, Views.newContext(), Path.Template.NEW_WORKFLOW);
	};

	public static Route create = (request, response) -> {
		LOG.debug("Request for new workflow with params: " + request.queryParams());

		Workflow workflow = Model.fromParams(request.queryMap("workflow"), Workflow.class);

		createNewWorkflow(workflow, request);

		SessionUtil.addOkMessage(request, "Workflow created.");
		response.redirect(Path.Web.getShowWorkflow(workflow));
		return Views.EMPTY_REPSONSE;
	};

	public static Route show = (request, response) -> {
		Workflow workflow = ensureSingleWorkflowByVersionIdFromRoute(request);
		Map<String, Object> context = Views.newContextWith("workflow", workflow);

		List<Workflow> versions = WorkflowDao.fetchVersionsOf(workflow);
		context.put("versions", versions);

		return Views.render(request, context, Path.Template.SHOW_WORKFLOW);
	};
	
	public static Route showVersion = (request, response) -> {
		long id = RequestUtil.parseIdParam(request);
		String versionId = RequestUtil.parseUuidParam(request);
		
		Workflow workflow = WorkflowDao.fetchSpecificVersion(id, versionId);
		if (workflow == null) {
			throw new ResourceNotFoundError("No workflow for id: '" + id + "' being a version of: " + versionId);
		}

		Map<String, Object> context = Views.newContextWith("workflow", workflow);
		return Views.render(request, context, Path.Template.SHOW_WORKFLOW_VERSION);
	};

	public static Route edit = (request, response) -> {
		Workflow workflow = ensureSingleWorkflowByVersionIdFromRoute(request);
		Map<String, Object> context = Views.newContextWith("workflow", workflow);
		return Views.render(request, context, Path.Template.EDIT_WORKFLOW);
	};

	public static Route update = (request, response) -> {
		Workflow newWorkflow = Model.fromParams(request.queryMap("workflow"), Workflow.class);

		// ensure that there is an older version of the workflow and that it matches the
		// version id that will be persisted
		Workflow oldWorkflow = ensureSingleWorkflowByVersionIdFromRoute(request);
		if (!oldWorkflow.getVersionId().equals(newWorkflow.getVersionId())) {
			throw new ResourceNotFoundError("Version ids of old and new workflows do not match");
		}

		// updating a workflow simply creates a new version
		createNewWorkflow(newWorkflow, request);
		SessionUtil.addOkMessage(request, "Workflow updated.");
		response.redirect(Path.Web.getShowWorkflow(newWorkflow));
		return Views.EMPTY_REPSONSE;
	};

	public static Route delete = (request, response) -> {
		Workflow workflow = ensureSingleWorkflowByVersionIdFromRoute(request);
		Map<String, Object> context = Views.newContextWith("workflow", workflow);
		// the target for the redirect on destroy is the page visited before this delete
		context.put("redirectTarget", SessionUtil.getLastLocationOrDefault(request, Path.Web.WORKFLOWS));
		return Views.render(request, context, Path.Template.DELETE_WORKFLOW);
	};

	public static Route destroy = (request, response) -> {
		Workflow workflow = ensureSingleWorkflowByVersionIdFromRoute(request);

		int deletedRows = WorkflowDao.destroy(workflow);
		LOG.debug("Deleted " + deletedRows + " versions of workflow " + workflow.getVersionId());

		SessionUtil.addOkMessage(request, "Workflow gelöscht.");

		String redirectTarget = request.queryParamOrDefault("redirectTarget", Path.Web.WORKFLOWS);
		response.redirect(redirectTarget);
		return Views.EMPTY_REPSONSE;
	};

	// return a single workflow from an id supplied in the request route, throw an
	// exception that will trigger re-routing if no workflow can be retrieved
	private static Workflow ensureSingleWorkflowByVersionIdFromRoute(Request request)
			throws ResourceNotFoundError, SQLException {
		String uuid = RequestUtil.parseUuidParam(request);
		final Workflow workflow = WorkflowDao.fetchLatestVersion(uuid);

		if (workflow == null) {
			throw new ResourceNotFoundError("No workflow for version id: '" + uuid + "'");
		}
		return workflow;
	}

	private static void createNewWorkflow(Workflow workflow, Request request)
			throws SQLException, InvalidModelException, InternalServerError {
		User author = ensureLoggedInUser(request, "Only registered users may create or update workflows.");
		workflow.setAuthor(author);

		if (workflow.validate()) {
			int count = WorkflowDao.save(workflow);
			ensureRowCountIsOne(count, "create");
		} else {
			throw new InvalidModelException(workflow);
		}
	}
}
