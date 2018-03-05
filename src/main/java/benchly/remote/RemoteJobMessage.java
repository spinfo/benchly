package benchly.remote;

import com.google.gson.annotations.Expose;

class RemoteJobMessage {

	// the uuid assigned by the server
	@Expose(serialize = false)
	String id;
	
	@Expose(serialize = false)
	String message;
	
	// the timestamp referring to the creation date
	@Expose(serialize = false)
	long recordedAt;
	
}
