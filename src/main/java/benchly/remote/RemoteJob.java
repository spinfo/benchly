package benchly.remote;

import java.util.List;

import com.google.gson.annotations.Expose;

class RemoteJob {

	@Expose
	protected long id;

	// the time estimated by the user in seconds
	@Expose
	protected long maxTime;

	// the time the job actually took up until now
	@Expose
	protected long maxMemory;

	// the workflow definition linked to this job
	@Expose
	protected String workflowDefinition;

	// used by the server to indicate if the job has failed
	@Expose(serialize = false)
	protected boolean failed;

	// used by the server to indicate when the job was received on it's end
	@Expose(serialize = false)
	protected Long createdAt;

	// used by the server to indicate when the job was actually started
	@Expose(serialize = false)
	protected Long startedAt;

	// used by the server to indicate when the job ended (successfully or not)
	@Expose(serialize = false)
	protected Long endedAt;

	// events recorded for this job by the server
	@Expose(serialize = false)
	protected List<RemoteJobMessage> events;

}
