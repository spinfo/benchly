package benchly.controller;

import java.sql.SQLException;
import java.util.Map;

import benchly.database.JobDao;
import benchly.database.WorkflowDao;
import benchly.error.ResourceNotFoundError;
import benchly.model.Job;
import benchly.model.Model;
import benchly.model.User;
import benchly.model.Workflow;
import benchly.util.Path;
import benchly.util.RequestUtil;
import benchly.util.Views;
import spark.Request;
import spark.Route;

public class JobController extends Controller {

	public static Route show = (request, response) -> {
		Job job = ensureSingleJobByIdFromRoute(request);

		Map<String, Object> context = Views.newContextWith("job", job);

		return Views.render(request, context, Path.Template.SHOW_JOB);
	};

	public static Route newJob = (request, response) -> {
		long workflowId = RequestUtil.parseIdParam(request);
		Workflow workflow = WorkflowDao.fetchById(workflowId);

		Map<String, Object> context = Views.newContextWith("workflow", workflow);

		return Views.render(request, context, Path.Template.NEW_JOB);
	};

	public static Route create = (request, response) -> {
		Job job = Model.fromParams(request.queryMap("job"), Job.class);

		User owner = ensureLoggedInUser(request, "only logged in users may create new jobs.");
		job.setOwner(owner);

		long workflowId = RequestUtil.parseNumberedQueryParamOrDefault(request, "workflowId", -1L);
		Workflow workflow = WorkflowDao.fetchById(workflowId);
		if (workflow == null) {
			throw new ResourceNotFoundError("Could not retrieve workflow for job creation, id: " + workflowId);
		}
		job.setWorkflow(workflow);
		
		job.setState(Job.State.PENDING);

		long rowsCreated = JobDao.create(job);
		ensureRowCountIsOne(rowsCreated, "create job");

		response.redirect(Path.Web.getShowJob(job));
		return Views.EMPTY_REPSONSE;
	};

	private static Job ensureSingleJobByIdFromRoute(Request request) throws ResourceNotFoundError, SQLException {
		long id = RequestUtil.parseIdParam(request);
		Job job = JobDao.fetchById(id);

		if (job == null) {
			throw new ResourceNotFoundError("No job for id: '" + id + "'");
		}
		return job;
	}
}
