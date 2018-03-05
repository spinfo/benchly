package benchly.error;

// represents an error during contact of a workbench server
public class ServerAccessError extends Exception {

	private static final long serialVersionUID = -4606027626890748353L;

	Integer httpStatusCode = null;

	public ServerAccessError() {
		super();
	}

	public ServerAccessError(String message) {
		super(message);
	}

	public ServerAccessError(String message, Throwable cause) {
		super(message, cause);
	}

	public ServerAccessError(int httpCode, String message) {
		super(message);
		this.httpStatusCode = httpCode;
	}

	/**
	 * @return The http status code if the error resulted from an unexpected server
	 *         response or null if this is not the case
	 */
	public Integer getStatusCode() {
		return this.httpStatusCode;
	}

}
