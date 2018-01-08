package benchly.error;

public class NoLoggedInUser extends Exception {

	private static final long serialVersionUID = 5427194799765314193L;

	public NoLoggedInUser() {
		super("Only registered users may do this.");
	}
}
