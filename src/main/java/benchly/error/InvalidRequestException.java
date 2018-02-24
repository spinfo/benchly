package benchly.error;

public class InvalidRequestException extends Exception {

	private static final long serialVersionUID = -3022218633130293784L;

	public InvalidRequestException(String message) {
		super(message);
	}

	public InvalidRequestException(Throwable t) {
		super(t);
	}

	public InvalidRequestException(String message, Throwable t) {
		super(message, t);
	}
}
