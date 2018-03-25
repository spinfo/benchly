package benchly.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import benchly.BenchlyScheduler;
import benchly.database.JobDao;
import benchly.database.WorkflowDao;
import benchly.error.ResourceNotFoundError;
import benchly.model.Job;
import benchly.model.User;
import benchly.model.Workflow;
import benchly.remote.JobCancelTask;
import benchly.util.RequestUtil;
import benchly.util.SessionUtil;
import benchly.util.RequestUtil.PaginationParams;
import spark.Request;
import spark.Route;

public class JobController extends Controller {

	public static Route show = (request, response) -> {
		ensureLoggedInUser(request, "Only logged in users may view josb.");
		
		Job job = ensureSingleJobByIdFromRoute(request);

		return JsonTransformer.render(job, request);
	};

	public static Route index = (request, response) -> {
		User user = ensureLoggedInUser(request, "Only registered users may view jobs.");

		PaginationParams pagination = RequestUtil.parsePaginationParams(request);

		List<Job> jobs;
		long max;
		if (user.isAdmin()) {
			jobs = JobDao.fetchAll(pagination);
			max = JobDao.count();
		} else {
			jobs = JobDao.fetchAllBelongigTo(user, pagination);
			max = JobDao.countBelongingTo(user);
		}

		return JsonTransformer.renderPaginatedResult(jobs, request, pagination.limit, pagination.offset, max);
	};

	public static Route create = (request, response) -> {
		Job job = JsonTransformer.readRequestBody(request.body(), Job.class);

		User owner = ensureLoggedInUser(request, "Only register users may create new jobs.");
		job.setOwner(owner);

		// ensure that the workflow referenced by this job actually exists and the user
		// may use it
		Workflow workflow = WorkflowDao.fetchById(job.getWorkflow().getId());
		if (workflow == null) {
			throw new ResourceNotFoundError("Could not retrieve workflow for job creation.");
		}
		ensureUserMayExecuteWorkflow(owner, workflow, request);
		// set our databse workflow on the job, not the version parsed from request
		job.setWorkflow(workflow);
		job.setState(Job.State.PENDING);

		long rowsCreated = JobDao.create(job);
		ensureRowCountIsOne(rowsCreated, "create job");

		return JsonTransformer.render(job, request);
	};

	public static Route cancel = (request, response) -> {
		User user = ensureLoggedInUser(request, "Only registered users may cancel jobs.");
		Job job = ensureSingleJobByIdFromRoute(request);

		ensureUserMayCancelJob(user, job, request);

		ScheduledExecutorService executor = BenchlyScheduler.get();
		executor.submit(new JobCancelTask(job, executor));

		SessionUtil.addOkMessage(request, "The job is scheduled to be canceled.");
		return emptyRoute.handle(request, response);
	};

	private static Job ensureSingleJobByIdFromRoute(Request request) throws ResourceNotFoundError, SQLException {
		long id = RequestUtil.parseIdParam(request);
		Job job = JobDao.fetchById(id);

		if (job == null) {
			throw new ResourceNotFoundError("No job for id: '" + id + "'");
		}
		return job;
	}

	private static void ensureUserMayCancelJob(User user, Job job, Request request) {
		if (!user.isAdmin() && !(user.getId() == job.getOwner().getId())) {
			haltForbbiden(request, "Insufficient permissins to cancel this job.");
		}
	}

	private static void ensureUserMayExecuteWorkflow(User user, Workflow workflow, Request request) {
		if (!user.isAdmin() && !(workflow.getAuthor().getId() == user.getId())) {
			haltForbbiden(request, "Insufficient workflow permissions to start a job for it.");
		}
	}
}
