package benchly.error;

public class StorageAccessError extends Exception {

	private static final long serialVersionUID = 9178845768954180481L;

	public StorageAccessError() {
		super();
	}
	
	public StorageAccessError(String message) {
		super(message);
	}
	
	public StorageAccessError(Throwable cause) {
		super(cause);
	}
	
}
