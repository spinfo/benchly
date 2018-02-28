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
		ensureUserMayViewWorkflows(request);
		PaginationParams pagination = RequestUtil.parsePaginationParams(request);

		List<Workflow> workflows = WorkflowDao.fetchLatestVersions(pagination);
		long amount = WorkflowDao.getCountOfLatestVersions();

		return JsonTransformer.renderPaginatedResult(workflows, request, pagination.limit, pagination.offset, amount);
	};

	public static Route create = (request, response) -> {
		Workflow workflow = JsonTransformer.readRequestBody(request.body(), Workflow.class);
		User user = ensureLoggedInUser(request, "Only registered users may create workflows.");

		// overwrite any version id on the received to workflow to prevent users from
		// overwriting by creating a new version of another users workflow
		workflow.generateNewVersionId();
		Workflow created = createNewWorkflow(workflow, user, request);

		SessionUtil.addOkMessage(request, "Workflow created.");
		return JsonTransformer.render(created, request);
	};

	public static Route show = (request, response) -> {
		ensureLoggedInUser(request, "Only logged-in users may view workflows.");
		Workflow workflow = ensureSingleWorkflowByVersionIdFromRoute(request);
		return JsonTransformer.render(workflow, request);
	};

	public static Route showVersion = (request, response) -> {
		ensureLoggedInUser(request, "Only logged-in users may view workflows.");
		long id = RequestUtil.parseIdParam(request);
		String versionId = RequestUtil.parseUuidParam(request);

		Workflow workflow = WorkflowDao.fetchSpecificVersion(id, versionId);
		if (workflow == null) {
			throw new ResourceNotFoundError("No workflow for id: '" + id + "' being a version of: " + versionId);
		}

		return JsonTransformer.render(workflow, request);
	};

	public static Route update = (request, response) -> {
		User user = ensureLoggedInUser(request, "Only logged in users may update workflows.");
		Workflow newWorkflow = JsonTransformer.readRequestBody(request.body(), Workflow.class);
		Workflow oldWorkflow = ensureSingleWorkflowByVersionIdFromRoute(request);
		ensureUserMayEditWorkflow(user, oldWorkflow, request);

		// ensure that there is an older version of the workflow and that it matches the
		// version id that will be persisted
		if (!oldWorkflow.getVersionId().equals(newWorkflow.getVersionId())) {
			// this is a bit paranoid, because we just retrieved the old version by id, but
			// let's check anyway
			throw new InvalidRequestException("Version ids of old and new workflows do not match.");
		}

		// updating a workflow simply creates a new version
		Workflow created = createNewWorkflow(newWorkflow, user, request);
		SessionUtil.addOkMessage(request, "Workflow updated.");
		return JsonTransformer.render(created, request);
	};

	public static Route destroy = (request, response) -> {
		User user = ensureLoggedInUser(request, "Only logged in users may delete workflows.");
		Workflow workflow = ensureSingleWorkflowByVersionIdFromRoute(request);
		ensureUserMayEditWorkflow(user, workflow, request);

		int deletedRows = WorkflowDao.setDeleted(workflow);
		LOG.debug("Deleted " + deletedRows + " versions of workflow " + workflow.getVersionId());

		SessionUtil.addOkMessage(request, "Workflow deleted.");
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

	private static void ensureUserMayEditWorkflow(User user, Workflow workflow, Request request) {
		if (user.isAdmin() || workflow.getAuthor().getId() == user.getId()) {
			return;
		}
		haltForbbiden(request, "You have insufficient permissions to edit this workflow.");
	}

	private static User ensureUserMayViewWorkflows(Request request) {
		return ensureLoggedInUser(request, "Only registered users may create or update workflows.");
	}

	private static Workflow createNewWorkflow(Workflow workflow, User author, Request request)
			throws SQLException, InvalidModelException, InternalServerError {
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
