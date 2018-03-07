package benchly.remote;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.JobDao;
import benchly.database.JobMessageDao;
import benchly.error.ServerAccessError;
import benchly.model.Job;
import benchly.model.JobMessage;
import benchly.model.ServerContact;

/**
 * Given a job, this will query the remote for it's status and update our
 * database accordingly.
 */
public class JobUpdateTask implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(JobUpdateTask.class);

	private final Job job;

	public JobUpdateTask(Job job) {
		this.job = job;
	}

	@Override
	public void run() {
		RemoteJob remote = fetchRemoteVersion(job);

		boolean jobEnded = handleJobEnd(job, remote);
		saveJobMessages(job, remote);
		
		try {
			job.setLastCheckedNow();
			JobDao.update(job);
		} catch (SQLException e) {
			LOG.error("Unable to save newly ended job, got: " + e.getMessage());
			e.printStackTrace();
		}

		if (jobEnded) {
			signalToDeleteJobData(job);
		}
	}

	private void saveJobMessages(Job job, RemoteJob remote) {
		for (RemoteJobMessage remoteMsg : remote.events) {
			// simply transform each message and save it if it doesn't exist
			try {
				JobMessage msg = ModelTransformer.transform(remoteMsg);
				msg.setJob(job);
				msg.setOrigin(job.getExecutingServer());
				msg.setWorkflow(job.getWorkflow());
				JobMessageDao.createIfNotExist(msg);
			} catch (Exception e) {
				LOG.error("Unable to save job message. Got: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private boolean handleJobEnd(Job job, RemoteJob remote) {
		// the job did finish. we need to mark that
		if (remote.endedAt != null) {
			Timestamp end = Timestamp.from(Instant.ofEpochSecond(remote.endedAt));

			if (remote.failed) {
				LOG.debug("Setting job failed: " + job.getId());
				job.setFailedOn(end);
			} else {
				LOG.debug("Setting job succeeded: " + job.getId());
				job.setSucceededOn(end);
			}
			return true;
		} else {
			LOG.debug("No job status change: " + +job.getId());
			return false;
		}
	}

	private RemoteJob fetchRemoteVersion(Job job) {
		try {
			ServerContact contact = job.getExecutingServer();
			return ServerAccess.fetchJobStatus(contact, job);
		} catch (Exception e) {
			LOG.error("Unable to fetch remote job version. Got: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	private void signalToDeleteJobData(Job job) {
		try {
			ServerContact contact = job.getExecutingServer();
			ServerAccess.deleteJobData(contact, job);
		} catch (ServerAccessError e) {
			LOG.error("Attempt to delete job data failed, got: " + e.getMessage());
		}
	}

}
