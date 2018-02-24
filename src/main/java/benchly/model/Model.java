package benchly.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * A base class for all models that sets up some basic functionality like
 * validation and instantiation by parameters.
 */
public abstract class Model {

	// a Set where implementing classes may put error messages.
	// A model is valid when no error messages exist for it.
	private Set<String> errorMessages = new TreeSet<>();

	// models are false by default until validated
	protected boolean valid = false;

	/**
	 * Actively validate the model.
	 * 
	 * @return whether the model instance is valid or not.
	 */
	public abstract boolean validate();

	public boolean isValid() {
		return valid;
	}

	/**
	 * @return all error messages connected to this model. (read only)
	 */
	public Collection<String> getErrorMessages() {
		return Collections.unmodifiableCollection(errorMessages);
	}

	// convenience method for implementing classes
	protected void addError(String message) {
		errorMessages.add(message);
		valid = false;
	}
}
