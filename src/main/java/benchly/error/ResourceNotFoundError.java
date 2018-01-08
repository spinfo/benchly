package benchly.error;

public class ResourceNotFoundError extends Exception {
	private static final long serialVersionUID = -4393513133311770362L;

	public ResourceNotFoundError(String message) {
		super(message);
	}
}
