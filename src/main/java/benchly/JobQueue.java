package benchly;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.JobDao;
import benchly.model.Job;

public class JobQueue implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(JobQueue.class);

	private List<Job> jobs;

	private long lastWakeup = 0L;

	private long wakeupInterval = 2000;

	public JobQueue() {
		setLastWakeupToNow();
	}

	// pick up pending jobs from the database and start them if possible
	public void processPending() throws SQLException, InterruptedException {
		setLastWakeupToNow();
		jobs = JobDao.getPendingJobs();
		for (Job job : jobs) {
			LOG.debug("pending job " + job.getId() + ": " + job.getWorkflow().getName() + " by: " + job.getOwner());
		}
	}

	// sleep for some time until the next wakeup interval is reached
	private void sleepSomeTime() throws InterruptedException {
		long diff = currentTimeMillis() - lastWakeup;
		long timeToSleep = wakeupInterval - diff;
		if (timeToSleep > 0L) {
			Thread.sleep(timeToSleep);
		}
	}

	private void setLastWakeupToNow() {
		this.lastWakeup = currentTimeMillis();
	}
	
	private long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	@Override
	public void run() {
		while (true) {
			try {
				processPending();
				sleepSomeTime();
			} catch (SQLException e) {
				LOG.error("Unable to process pending jobs: " + e.getMessage());
			} catch (InterruptedException e) {
				e.printStackTrace();
				LOG.error("Unexpected interrupt during job processing: " + e.getMessage());
			}
		}
	}

}
