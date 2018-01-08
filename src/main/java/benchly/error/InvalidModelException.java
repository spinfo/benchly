package benchly.error;

import benchly.model.Model;

public class InvalidModelException extends Exception {

	private static final long serialVersionUID = -230657123565051971L;

	private Model model;

	public InvalidModelException(Model model) {
		this.model = model;
	}

	public Model getModel() {
		return this.model;
	}
}
