package benchly.remote;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.CloseableIterator;

import benchly.database.JobDao;
import benchly.database.JobMessageDao;
import benchly.database.ServerContactDao;
import benchly.error.ServerAccessError;
import benchly.model.Job;
import benchly.model.JobMessage;
import benchly.model.ServerContact;

public class JobSubmitTask implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(JobSubmitTask.class); 
	
	private static final int MAX_SUBMITTAL_ATTEMPTS = 3;
	
	private final Job job;

	public JobSubmitTask(Job job) {
		this.job = job;
	}

	@Override
	public void run() {
		boolean wasSubmitted = false;
		try (CloseableIterator<ServerContact> contacts = fetchSuitableContacts(job)) {
			if (contacts == null || !contacts.hasNext()) {
				recordJobMessage(job, "No suitable server contact found for job. Delaying execution for later.");
				// Return. The job's submission attempts are not incremented as none were made
				return;
			}

			// try to submit the job
			while (contacts.hasNext()) {
				ServerContact contact = contacts.next();
				LOG.debug("Trying contact with resources: " + (contact.getApproximateUsableMemory() / 1000000)
						+ " MB (" + contact.getApproximateRunningJobs() + " running jobs).");

				wasSubmitted = attemptSubmit(job, contact);
				if (wasSubmitted) {
					String msg = "Successfully submitted job " + job.getId() + " for processing on: "
							+ contact.getName();
					recordJobMessage(job, msg);
					saveSubmissionStatus(job, contact);
					break;
				}
			}
		} catch (Exception e) {
			String msg = "Unexpected error during job submission.";
			LOG.error(msg + " Got: " + e.getMessage());
			e.printStackTrace();
			// do not return here, we still want to increment the submittal attempts to
			// prevent infinitely to get this error
		}

		// if the job was not accepted by any contact, handle its further status
		if (!wasSubmitted) {
			incrementSubmittalAttempts(job);
			int remainingAttempts = MAX_SUBMITTAL_ATTEMPTS - job.getFailedSubmittalAttempts();
			if (remainingAttempts == 0) {
				setJobCanceled(job);
				recordJobMessage(job, "Job canceled due to too many failed submissions.");
			} else {
				recordJobMessage(job, "Job submission deferred. Remaining attempts: " + remainingAttempts);
			}
		}

	}

	private CloseableIterator<ServerContact> fetchSuitableContacts(Job job) {
		try {
			return ServerContactDao.fetchJobSubmittalCandidates(job.getEstimatedMemory());
		} catch (Exception e) {
			LOG.error("SQL error when retrieving suitable contacts for pending job: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	private boolean attemptSubmit(Job job, ServerContact target) {
		try {
			return ServerAccess.submitJob(target, job);
		} catch (ServerAccessError e) {
			recordJobMessage(job, e.getMessage());
			return false;
		}
	}

	private void recordJobMessage(Job job, String message) {
		try {
			LOG.debug("Message for Job (" + job.getId() + "): " + message);
			JobMessage msg = new JobMessage(job, message);
			JobMessageDao.create(msg);
		} catch (Exception e) {
			LOG.error("Unable to record job message for job.");
			e.printStackTrace();
		}
	}

	private void saveSubmissionStatus(Job job, ServerContact contact) {
		try {
			job.setSubmittedNow(contact);
			JobDao.update(job);
		} catch (Exception e) {
			LOG.error("Unable to save submission information on newly submitted job: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void incrementSubmittalAttempts(Job job) {
		try {
			job.incrementFailedSubmittalAttempts();
			JobDao.update(job);
		} catch (Exception e) {
			LOG.error("Unable to increment submittal attempty on job: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void setJobCanceled(Job job) {
		try {
			job.setFailedNow();
			JobDao.update(job);
		} catch (SQLException e) {
			LOG.error("Unable to set job status canceled: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
}
