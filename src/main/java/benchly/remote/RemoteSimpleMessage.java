package benchly.remote;

import com.google.gson.annotations.Expose;

class RemoteSimpleMessage {

	@Expose(serialize = false)
	String message;

}
