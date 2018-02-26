package benchly.model;

import java.sql.Timestamp;
import java.time.Instant;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "job")
public class Job extends Model {

	public static enum State {
		PENDING, PROCESSING, CANCELED, FINISHED
	}

	@DatabaseField(columnName = "id", generatedId = true)
	@Expose(deserialize = false)
	private long id;
	
	@DatabaseField(columnName = "state")
	private State state;
	
	@DatabaseField(columnName = "owner", canBeNull = false, foreign = true, foreignAutoRefresh = true, index = true)
	private User owner;

	@DatabaseField(columnName = "workflow", canBeNull = false, foreign = true, foreignAutoRefresh = true, index = true)
	private Workflow workflow;

	@DatabaseField
	private int priority;

	// the time estimated by the user in seconds
	@DatabaseField
	private long estimatedTime;

	// the time the job actually took up until now
	@DatabaseField
	private long timeProcessing;

	// the output size estimated by the user in bytes
	@DatabaseField
	private long estimatedOutputSize;

	// the last output size actually reported for this workflow
	@DatabaseField
	private long currentOutputSize;
	
	@DatabaseField(columnName = "createdAt", canBeNull = false, index = true)
	private Timestamp createdAt;
	
	public Job() {
		this.createdAt = Timestamp.from(Instant.now());
	}
	
	public Job(User owner, Workflow workflow) {
		this();
		this.owner = owner;
		this.workflow = workflow;
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

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public long getEstimatedTime() {
		return estimatedTime;
	}

	public void setEstimatedTime(long estimatedTime) {
		this.estimatedTime = estimatedTime;
	}

	public long getTimeProcessing() {
		return timeProcessing;
	}

	public void setTimeProcessing(long timeProcessing) {
		this.timeProcessing = timeProcessing;
	}

	public long getEstimatedOutputSize() {
		return estimatedOutputSize;
	}

	public void setEstimatedOutputSize(long estimatedOutputSize) {
		this.estimatedOutputSize = estimatedOutputSize;
	}

	public long getCurrentOutputSize() {
		return currentOutputSize;
	}

	public void setCurrentOutputSize(long currentOutputSize) {
		this.currentOutputSize = currentOutputSize;
	}

	public long getId() {
		return id;
	}

	@Override
	public boolean validate() {
		// TODO Auto-generated method stub
		return false;
	}

}
