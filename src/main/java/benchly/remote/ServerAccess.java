package benchly.remote;

import java.net.URI;

import benchly.error.ServerAccessError;
import benchly.model.Job;
import benchly.model.ServerContact;
import benchly.model.StatusReport;
import benchly.remote.ServerHttp.ResponseCallback;

public class ServerAccess {

	/**
	 * @return A StatusReport indicating the contact's state.
	 * @throws ServerAccessError On a response indicating failure or an unparseable response.
	 */
	public static StatusReport fetchStatus(ServerContact contact) throws ServerAccessError {
		URI uri = new ServerContactUriBuilder(contact).setStatusPath().build();

		return (StatusReport) ServerHttp.get(uri, new ResponseCallback() {
			@Override
			public Object call(int returnCode, String responseBody) throws ServerAccessError {
				if (returnCode == 200) {
					return ModelTransformer.readStatusReport(responseBody);
				} else {
					throw new ServerAccessError("Unable to get status from contact.");
				}
			}
		});
	}

	/**
	 * @return true if the remote server accepted the job, throw otherwise.
	 * @throws ServerAccessError
	 */
	public static Boolean submitJob(ServerContact contact, Job job) throws ServerAccessError {
		String payload = ModelTransformer.render(job);
		URI uri = new ServerContactUriBuilder(contact).setJobsPath().build();

		return (Boolean) ServerHttp.post(uri, payload, new ResponseCallback() {
			
			@Override
			public Object call(int returnCode, String responseBody) throws ServerAccessError {
				if (returnCode != 200) {
					String msg = ModelTransformer.readSimpleMessage(responseBody);
					if (returnCode == 400) {
						throw new ServerAccessError("Job was rejected for processing with message: " + msg);
					} else {
						throw new ServerAccessError("Unexpected response on job submittal, message: " + msg);
					}
				}
				return new Boolean(true);
			}
		});
	}

	/**
	 * @return A RemoteJob object with event messages representing the job's status on the server.
	 * @throws ServerAccessError
	 */
	public static RemoteJob fetchJobStatus(ServerContact contact, Job job) throws ServerAccessError {
		URI uri = new ServerContactUriBuilder(contact).setJobsPath(job.getId()).build();
		return (RemoteJob) ServerHttp.get(uri, new ResponseCallback() {

			@Override
			public Object call(int returnCode, String responseBody) throws ServerAccessError {
				if (returnCode == 200) {
					return ModelTransformer.readRemoteJob(responseBody);
				} else {
					String msg = ModelTransformer.readSimpleMessage(responseBody);
					throw new ServerAccessError("Unexpected response on job status check, message: " + msg);
				}
			}
		});
	}

	/**
	 * @return true if the remote server accepted the cancellation, throw otherwise
	 * @throws ServerAccessError
	 */
	public static Boolean cancelJob(ServerContact contact, Job job) throws ServerAccessError {
		URI uri = new ServerContactUriBuilder(contact).setJobCancelPath(job.getId()).build();

		return (Boolean) ServerHttp.post(uri, "", new ResponseCallback() {

			@Override
			public Object call(int returnCode, String responseBody) throws ServerAccessError {
				if (returnCode != 200) {
					String msg = ModelTransformer.readSimpleMessage(responseBody);
					throw new ServerAccessError("Unexpected response on job cancel, message: " + msg);
				}
				return new Boolean(true);
			}
		});
	}
	
	/**
	 * @return The deleted job if the server returned OK, throws otherwise.
	 * @throws ServerAccessError
	 */
	public static RemoteJob deleteJobData(ServerContact contact, Job job) throws ServerAccessError {
		URI uri = new ServerContactUriBuilder(contact).setJobsPath(job.getId()).build();

		return (RemoteJob) ServerHttp.delete(uri, new ResponseCallback() {
			
			@Override
			public Object call(int returnCode, String responseBody) throws ServerAccessError {
				if (returnCode == 200) {
					return ModelTransformer.readRemoteJob(responseBody);
				} else {
					String msg = ModelTransformer.readSimpleMessage(responseBody);
					throw new ServerAccessError("Unexpected response on job deletion request, message: " + msg);
				}
			}
		});
	}

}
