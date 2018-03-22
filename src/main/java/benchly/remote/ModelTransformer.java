package benchly.remote;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import benchly.database.ServerContactDao;
import benchly.error.ServerAccessError;
import benchly.model.Job;
import benchly.model.JobMessage;
import benchly.model.ServerContact;
import benchly.model.StatusReport;

class ModelTransformer {

	private static final Logger LOG = LoggerFactory.getLogger(ModelTransformer.class);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation()
			.create();

	protected static RemoteJob transform(Job job) {
		RemoteJob result = new RemoteJob();

		result.id = job.getId();
		result.maxMemory = job.getEstimatedMemory();
		result.maxTime = job.getEstimatedTime();
		result.workflowDefinition = job.getWorkflow().getDefinition();
		return result;
	}
	
	protected static JobMessage transform(RemoteJobMessage remoteMsg) {
		JobMessage msg = new JobMessage(remoteMsg.id);
		msg.setContent(remoteMsg.message);
		msg.setRecordedAt(Timestamp.from(Instant.ofEpochSecond(remoteMsg.recordedAt)));
		return msg;
	}

	protected static String readSimpleMessage(String json) throws ServerAccessError {
		try {
			RemoteSimpleMessage msg = GSON.fromJson(json, RemoteSimpleMessage.class);
			return msg.message;
		} catch (Exception e) {
			throw new ServerAccessError("Unable to parse message from Server response. Got error: " + e.getMessage());
		}
	}

	protected static RemoteJob readRemoteJob(String json) throws ServerAccessError {
		try {
			return GSON.fromJson(json, RemoteJob.class);
		} catch (Exception e) {
			throw new ServerAccessError("Unable to parse job from Server response. Got error: " + e.getMessage());
		}
	}

	protected static StatusReport readStatusReport(String json) throws ServerAccessError {
		RemoteStatusReport remote;
		try {
			remote = GSON.fromJson(json, RemoteStatusReport.class);
		} catch (JsonSyntaxException e) {
			throw new ServerAccessError("Unable to parse json response for status report from server.");
		}

		// try to determine the Server Contact and fail if none is accessible
		ServerContact contact = null;
		boolean fetchError = false;
		try {
			contact = ServerContactDao.fetchByName(remote.name);
		} catch (SQLException e) {
			LOG.error("SQL Error during retrieval of contact with name: " + remote.name + ". Says: " + e.getMessage());
			fetchError = true;
		}
		if (contact == null || fetchError) {
			LOG.warn("No server contact in the db with a name returned by a status report response");
		}

		// initialize our version of the status report
		StatusReport result = new StatusReport();
		result.setContact(contact);
		result.setName(remote.name);
		result.setMaxMemory(remote.maxMemory);
		result.setTotalMemory(remote.totalMemory);
		result.setFreeMemory(remote.freeMemory);
		result.setUsedMemory(remote.usedMemory);
		result.setUsableMemory(remote.usableMemory);
		result.setLongTermUsableMemory(remote.longTermUsableMemory);
		result.setRunningJobs(remote.runningJobs);
		try {
			result.setCollectedAt(Timestamp.from(Instant.ofEpochSecond(remote.collectedAt)));
		} catch (NullPointerException | IllegalArgumentException | DateTimeException e) {
			throw new ServerAccessError(
					"Unable to parse timestamp returned by server for status report: " + remote.collectedAt);
		}
		return result;
	}

	protected static String render(Job job) {
		return doRender(transform(job));
	}

	private static String doRender(Object objet) {
		return GSON.toJson(objet);
	}
}
