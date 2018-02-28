package benchly.controller;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.WorkflowDao;
import benchly.error.InternalServerError;
import benchly.error.InvalidModelException;
import benchly.error.InvalidRequestException;
import benchly.error.ResourceNotFoundError;
import benchly.model.User;
import benchly.model.Workflow;
import benchly.util.RequestUtil;
import benchly.util.RequestUtil.PaginationParams;
import benchly.util.SessionUtil;
import spark.Request;
import spark.Route;

public class WorkflowController extends Controller {

	private static final Logger LOG = LoggerFactory.getLogger(WorkflowController.class);

	public static Route index = (request, response) -> {
		PaginationParams pagination = RequestUtil.parsePaginationParams(request);

		List<Workflow> workflows = WorkflowDao.fetchLatestVersions(pagination);
		long amount = WorkflowDao.getCountOfLatestVersions();

		return JsonTransformer.renderPaginatedResult(workflows, request, pagination.limit, pagination.offset, amount);
	};

	public static Route create = (request, response) -> {
		Workflow workflow = JsonTransformer.readRequestBody(request.body(), Workflow.class);
		Workflow created = createNewWorkflow(workflow, request);

		SessionUtil.addOkMessage(request, "Workflow created.");
		return JsonTransformer.render(created, request);
	};

	public static Route show = (request, response) -> {
		Workflow workflow = ensureSingleWorkflowByVersionIdFromRoute(request);
		return JsonTransformer.render(workflow, request);
	};

	public static Route showVersion = (request, response) -> {
		long id = RequestUtil.parseIdParam(request);
		String versionId = RequestUtil.parseUuidParam(request);

		Workflow workflow = WorkflowDao.fetchSpecificVersion(id, versionId);
		if (workflow == null) {
			throw new ResourceNotFoundError("No workflow for id: '" + id + "' being a version of: " + versionId);
		}

		return JsonTransformer.render(workflow, request);
	};

	public static Route update = (request, response) -> {
		Workflow newWorkflow = JsonTransformer.readRequestBody(request.body(), Workflow.class);

		// ensure that there is an older version of the workflow and that it matches the
		// version id that will be persisted
		Workflow oldWorkflow = ensureSingleWorkflowByVersionIdFromRoute(request);
		if (!oldWorkflow.getVersionId().equals(newWorkflow.getVersionId())) {
			// this is a bit paranoid, because we just retrieved the old version by id, but
			// let's check anyway
			throw new InvalidRequestException("Version ids of old and new workflows do not match");
		}

		// updating a workflow simply creates a new version
		Workflow created = createNewWorkflow(newWorkflow, request);
		SessionUtil.addOkMessage(request, "Workflow updated.");
		return JsonTransformer.render(created, request);
	};

	public static Route destroy = (request, response) -> {
		Workflow workflow = ensureSingleWorkflowByVersionIdFromRoute(request);

		int deletedRows = WorkflowDao.setDeleted(workflow);
		LOG.debug("Deleted " + deletedRows + " versions of workflow " + workflow.getVersionId());

		SessionUtil.addOkMessage(request, "Workflow deleted.");
		response.status(200);
		return JsonTransformer.render(workflow, request);
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

	private static Workflow createNewWorkflow(Workflow workflow, Request request)
			throws SQLException, InvalidModelException, InternalServerError {

		User author = ensureLoggedInUser(request, "Only registered users may create or update workflows.");
		workflow.setAuthor(author);

		if (workflow.validate()) {
			int count = WorkflowDao.save(workflow);
			ensureRowCountIsOne(count, "create workfow");
		} else {
			throw new InvalidModelException(workflow);
		}

		return workflow;
	}
}
