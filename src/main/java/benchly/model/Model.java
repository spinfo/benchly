package benchly.model;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.error.InternalServerError;
import spark.QueryParamsMap;

/**
 * A base class for all models that sets up some basic functionality like
 * validation and instantiation by parameters.
 */
public abstract class Model {

	private static final Logger LOG = LoggerFactory.getLogger(Model.class);

	// a Set where implementing classes may put error messages disallowing
	// duplicates
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

	/**
	 * @return A list of strings representing class members that may be initialised
	 *         from query parameters with the same name.
	 */
	protected abstract List<String> getParameterWhitelist();

	/**
	 * Automatically convert the supplied params into an object of type type. The
	 * parameters to be converted must be defined in the implementing classes
	 * whitelist.
	 * 
	 * @param params
	 *            A parameter map to use, field names must be on the top level.
	 * @param type
	 *            The type to instantiate.
	 * @return A new instance of the given type.
	 */
	public static <T extends Model> T fromParams(QueryParamsMap params, Class<T> type) throws InternalServerError {
		try {
			T result = type.getConstructor().newInstance();
			for (String fieldName : result.getParameterWhitelist()) {
				// Lookup the field in params
				if (params.get(fieldName) == null || StringUtils.isBlank(params.get(fieldName).value())) {
					continue;
				}
				String valueToSet = params.get(fieldName).value();

				// get the field to set and determine its class
				Field field = type.getDeclaredField(fieldName);
				Class<?> targetClass = field.getType();
				LOG.debug("Attempting to set field '" + fieldName + "' with value '" + valueToSet + "'.");

				// save the access status to restore it later
				boolean accessStatus = field.isAccessible();
				field.setAccessible(true);
				
				// actually set the value parsing it dependent on the field's class
				if (targetClass == String.class) {
					field.set(result, valueToSet);
				} else if (targetClass == Long.class || targetClass == Long.TYPE) {
					field.set(result, Long.parseLong(valueToSet));
				} else if (targetClass == Integer.class || targetClass == Integer.TYPE) {
					field.set(result, Integer.parseInt(valueToSet));
				} else {
					LOG.warn("Unrecognized class for field '" + fieldName + "': " + targetClass.getName());
				}
				field.setAccessible(accessStatus);
			}

			return result;
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException | NoSuchFieldException e) {
			e.printStackTrace();
			LOG.error(e.getMessage());
			throw new InternalServerError(
					"Cannot instantiate object of type " + type.getName() + ", got " + e.getClass().getSimpleName());
		}
	}

}
