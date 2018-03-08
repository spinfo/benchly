package benchly.model;

import java.sql.Timestamp;
import java.time.Instant;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "job")
public class Job extends Model {

	public static enum State {
		PENDING, SUBMITTED, FAILED, SUCCEEDED
	}

	@DatabaseField(columnName = "id", generatedId = true)
	@Expose(deserialize = false)
	private long id;

	@DatabaseField(columnName = "state", canBeNull = false, index = true)
	@Expose(deserialize = false)
	private State state;

	@DatabaseField(columnName = "owner", canBeNull = false, foreign = true, foreignAutoRefresh = true, index = true)
	@Expose(deserialize = false)
	private User owner;

	@DatabaseField(columnName = "workflow", canBeNull = false, foreign = true, foreignAutoRefresh = true, index = true)
	@Expose
	private Workflow workflow;

	@DatabaseField(columnName = "executingServer", canBeNull = true, foreign = true, foreignAutoRefresh = true, index = true)
	@Expose(deserialize = false)
	private ServerContact executingServer;

	// the time estimated by the user in seconds
	@DatabaseField(columnName = "estimatedTime", index = true)
	@Expose
	private long estimatedTime;

	// the time the job actually took up until now
	@DatabaseField(columnName = "estimatedMemory", index = true)
	@Expose
	private long estimatedMemory;

	// how often a try was made to submit the job to multiple servers and all
	// attempts failed
	@DatabaseField(columnName = "failedSubmittalAttempts", canBeNull = false)
	@Expose(deserialize = false)
	private int failedSubmittalAttempts = 0;

	// when the job was successfully submitted to a server for processing
	@DatabaseField(columnName = "submittedAt", canBeNull = true)
	@Expose(deserialize = false)
	private Timestamp submittedAt;

	@DatabaseField(columnName = "endedAt", canBeNull = true)
	@Expose(deserialize = false)
	private Timestamp endedAt;

	@DatabaseField(columnName = "createdAt", canBeNull = false, index = true)
	@Expose(deserialize = false)
	private Timestamp createdAt;

	// when the remote was last asked for information about this job
	@DatabaseField(columnName = "lastChecked", canBeNull = true)
	@Expose(deserialize = false)
	private Timestamp lastChecked;

	public Job() {
		this.state = State.PENDING;
		this.createdAt = Timestamp.from(Instant.now());
	}

	public Job(User owner, Workflow workflow) {
		this();
		this.owner = owner;
		this.workflow = workflow;
	}

	public long getId() {
		return id;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public Workflow getWorkflow() {
		return workflow;
	}

	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}

	public ServerContact getExecutingServer() {
		return executingServer;
	}

	public long getEstimatedTime() {
		return estimatedTime;
	}

	public void setEstimatedTime(long estimatedTime) {
		this.estimatedTime = estimatedTime;
	}

	public long getEstimatedMemory() {
		return estimatedMemory;
	}

	public void setEstimatedMemory(long estimatedMemory) {
		this.estimatedMemory = estimatedMemory;
	}

	public int incrementFailedSubmittalAttempts() {
		failedSubmittalAttempts += 1;
		return getFailedSubmittalAttempts();
	}

	public int getFailedSubmittalAttempts() {
		return failedSubmittalAttempts;
	}

	public Timestamp getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedNow(ServerContact executingContact) {
		this.executingServer = executingContact;
		this.state = State.SUBMITTED;
		this.submittedAt = Timestamp.from(Instant.now());
	}

	public void setFailedOn(Timestamp timestamp) {
		this.state = State.FAILED;
		this.endedAt = timestamp;
	}

	public void setFailedNow() {
		setFailedOn(Timestamp.from(Instant.now()));
	}

	public void setSucceededOn(Timestamp timestamp) {
		this.state = State.SUCCEEDED;
		this.endedAt = timestamp;
	}

	public Timestamp getLastChecked() {
		return lastChecked;
	}

	public void setLastCheckedNow() {
		this.lastChecked = Timestamp.from(Instant.now());
	}

	@Override
	public boolean validate() {
		// TODO Auto-generated method stub
		return false;
	}

}
