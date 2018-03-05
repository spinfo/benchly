package benchly;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.JobDao;
import benchly.model.Job;
import benchly.remote.JobUpdateTask;

/**
 * On each run selects up to one job from the database and checks the job's
 * status.
 */
class JobWatcher implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(JobWatcher.class);

	private final int secondsTillCheck;

	private final ExecutorService executor;

	protected JobWatcher(ExecutorService executor, int secondsTillCheckup) {
		this.secondsTillCheck = secondsTillCheckup;
		this.executor = executor;
	}

	@Override
	public void run() {
		Job job = pickAJobToCheck();
		if (job == null) {
			return;
		} else {
			executor.submit(new JobUpdateTask(job));
		}
	}

	private Job pickAJobToCheck() {
		try {
			return JobDao.pickSubmittedJobWhereLastCheckIsLongerAgoThan(secondsTillCheck);
		} catch (Exception e) {
			LOG.error("Error while attempting to fetch a job for checkup from the database: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

}
