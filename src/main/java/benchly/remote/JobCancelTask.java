package benchly.remote;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.JobMessageDao;
import benchly.error.ServerAccessError;
import benchly.model.Job;
import benchly.model.JobMessage;

public class JobCancelTask implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(JobCancelTask.class);

	private final ScheduledExecutorService executor;

	private final Job job;

	/**
	 * Builds Runnable Task that cancels a job on it's executing host and initiates
	 * a deferred check on the job's status;
	 * 
	 * @param job
	 *            The job to cancel.
	 * @param executor
	 *            An executor that may be used to schedule an update of the jobs'
	 *            status
	 */
	public JobCancelTask(Job job, ScheduledExecutorService executor) {
		this.job = job;
		this.executor = executor;
	}

	@Override
	public void run() {
		try {
			LOG.debug("Starting the cancelling...");
			boolean wasCancelled = ServerAccess.cancelJob(job.getExecutingServer(), job);
			LOG.debug("Returned from cancelling...");
			
			if (wasCancelled) {
				// the remote may defer cancelling the job on it's side, so give it some time to react
				executor.schedule(new JobUpdateTask(job), 1500L, TimeUnit.MILLISECONDS);
			} else {
				// this should not actually happen, but let's make sure it would get logged anyway
				throw new ServerAccessError("Unexpected result for remote job cancel without further message.");
			}
		} catch (ServerAccessError e) {
			String msg = "Received an error while attempting to cancel the remote job. Got: " + e.getMessage();
			LOG.error(msg);
			safeSubmitJobMessage(job, msg);
		}
	}

	private void safeSubmitJobMessage(Job job, String message) {
		try {
			JobMessageDao.create(new JobMessage(job, message));
		} catch (Exception e) {
			LOG.error("Unable to save a new job message to the database. Got: " + e.getMessage());
			LOG.error("Potentially unpersisted message: " + message);
		}
	}

}
