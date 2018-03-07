package benchly.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "job_message")
public class JobMessage {

	// a message from a remote server may have a uuid set
	@DatabaseField(id = true, columnName = "uuid", canBeNull = false)
	private String uuid;

	@DatabaseField(columnName = "job", foreign = true, foreignAutoRefresh = false, index = true, canBeNull = false)
	@Expose(deserialize = false)
	private Job job;

	@DatabaseField(columnName = "workflow", foreign = true, foreignAutoRefresh = false, index = true, canBeNull = false)
	@Expose(deserialize = false)
	private Workflow workflow;

	@DatabaseField(columnName = "origin", foreign = true, foreignAutoRefresh = false, index = true, canBeNull = true)
	private ServerContact origin;

	@DatabaseField(columnName = "content", dataType = DataType.LONG_STRING)
	@Expose(deserialize = false)
	private String content;

	// the remote or local time of recording
	@DatabaseField(columnName = "createdAt", canBeNull = false)
	private Timestamp recordedAt;

	public JobMessage() {
		this.uuid = UUID.randomUUID().toString();
	}
	
	public JobMessage(String uuid) {
		this.uuid = uuid;
	}

	// constructor for a local message, originating from this server
	public JobMessage(Job job, String content) {
		this();
		this.job = job;
		this.workflow = job.getWorkflow();
		this.content = content;
		this.recordedAt = Timestamp.from(Instant.now());
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public Workflow getWorkflow() {
		return workflow;
	}

	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}

	public ServerContact getOrigin() {
		return origin;
	}

	public void setOrigin(ServerContact origin) {
		this.origin = origin;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Timestamp getRecordedAt() {
		return recordedAt;
	}

	public void setRecordedAt(Timestamp recordedAt) {
		this.recordedAt = recordedAt;
	}

}
