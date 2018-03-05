package benchly;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.JobDao;
import benchly.model.Job;
import benchly.remote.JobSubmitTask;

/**
 * A periodic task that fetches pending jobs from the database, and starts of
 * the submittal process for them.
 */
class JobScheduler implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);

	private final ScheduledExecutorService executor;
	
	private final int delayEachMillis;

	public JobScheduler(ScheduledExecutorService executor, int delayEachInMillis) {
		this.executor = executor;
		this.delayEachMillis = delayEachInMillis;
	}

	@Override
	public void run() {
		List<Job> pending = fetchPendingJobs();

		LOG.debug("Pending jobs: " + pending.size());

		long delay = 0;
		for (Job job : pending) {
			executor.schedule(new JobSubmitTask(job), delay, TimeUnit.MILLISECONDS);
			delay += delayEachMillis;
		}
	}

	private List<Job> fetchPendingJobs() {
		try {
			return JobDao.fetchPendingJobs();
		} catch (SQLException e) {
			LOG.error("SQL error when retrieving pending jobs for processing: " + e.getMessage());
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

}
