package benchly.error;

public class InternalServerError extends Exception {

	private static final long serialVersionUID = 8108828749322025619L;

	public InternalServerError(String message) {
		super(message);
	}

	public InternalServerError(Throwable t) {
		super(t);
	}
	
	public InternalServerError(String message, Throwable t) {
		super(message, t);
	}
}