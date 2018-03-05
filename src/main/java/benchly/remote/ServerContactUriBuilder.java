package benchly.remote;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;

import benchly.error.ServerAccessError;
import benchly.model.ServerContact;

/**
 * An URI builder, that may be initialised with a server contact and is aware of
 * the path component, that may be defined in the contacts endpoint. Plus
 * convenience methods for the paths defined for the server.
 * 
 * This will also wrap any URISyntaxException into a ServerAccessError for
 * easier handling.
 * 
 * NOTE: This is somewhat of a ridiculous solution for the few URIs that we have
 * at the moment. It was planned when things looked different, so feel free to
 * change it.
 */
class ServerContactUriBuilder {

	private static final String STATUS = "status";
	private static final String JOBS = "jobs";
	private static final String CANCEL_ACTION = "cancel";

	private static final String DELIM = "/";

	private URIBuilder builder;

	private String endpointPath;

	// NOTE: We could simply extend URIBuilder, but that would make it impossible to
	// wrap any URISyntaxExceptions in overriding methods
	ServerContactUriBuilder(ServerContact contact) throws ServerAccessError {
		try {
			this.builder = new URIBuilder(contact.getEndpoint());
			this.endpointPath = builder.getPath();
		} catch (URISyntaxException e) {
			throw new ServerAccessError("Unable to build the URI from contact: " + contact.getEndpoint(), e);
		}
	}

	protected ServerContactUriBuilder setPath(String path) {
		builder.setPath(combine(this.endpointPath, path));
		return this;
	}

	protected URI build() throws ServerAccessError {
		try {
			return builder.build();
		} catch (URISyntaxException e) {
			throw new ServerAccessError("Unable to construct an URI for server access", e);
		}
	}

	protected ServerContactUriBuilder setJobsPath() {
		return this.setPath(JOBS);
	}

	protected ServerContactUriBuilder setJobsPath(long jobId) {
		return this.setPath(combine(JOBS, Long.toString(jobId)));
	}

	protected ServerContactUriBuilder setJobCancelPath(long jobId) {
		return this.setPath(combine(JOBS, Long.toString(jobId), CANCEL_ACTION));
	}

	protected ServerContactUriBuilder setStatusPath() {
		return this.setPath(STATUS);
	}

	private static String combine(String... pathComponents) {
		return String.join(DELIM, pathComponents);
	}

}
