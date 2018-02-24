package benchly.error;

import benchly.model.Model;

public class InvalidModelException extends InvalidRequestException {

	private static final long serialVersionUID = -230657123565051971L;

	private Model model;

	public InvalidModelException(Model model) {
		super("Invalid model: " + model.getClass().getSimpleName());
		this.model = model;
	}

	public Model getModel() {
		return this.model;
	}
}
