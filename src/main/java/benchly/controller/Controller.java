package benchly.controller;

import static spark.Spark.halt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.error.InternalServerError;
import benchly.error.InvalidModelException;
import benchly.model.Model;
import benchly.model.User;
import benchly.util.SessionUtil;
import spark.Request;
import spark.Route;

abstract class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

	static User ensureLoggedInUser(Request request, String errorMessage) {
		User user = SessionUtil.getCurrentUser(request);
		if (user == null) {
			LOG.warn("Returning 403 Forbidden on request: " + request.pathInfo() + " (not a logged in user)");
			haltForbbiden(request, errorMessage);
		}
		return user;
	}

	static User ensureAdminUser(Request request, String errorMessage) {
		User user = ensureLoggedInUser(request, errorMessage);
		if (!user.isAdmin()) {
			LOG.warn("Returning 403 Forbidden on request: " + request.pathInfo() + " (not an admin user)");
			haltForbbiden(request, errorMessage);
		}
		return user;
	}

	static void ensureValidModel(Model model, Request request) throws InvalidModelException {
		if (!model.validate()) {
			for (String message : model.getErrorMessages()) {
				SessionUtil.addErrorMessage(request, message);
			}
			throw new InvalidModelException(model);
		}
	}

	static void ensureRowCountIsOne(long rowsAffected, String action) throws InternalServerError {
		if (rowsAffected != 1) {
			throw new InternalServerError("Wrong row count on " + action + ": " + rowsAffected);
		}
	}

	// do not throw an Exception to be caught by a handler, but halt immediately
	protected static void haltForbbiden(Request request, String errorMessage) {
		SessionUtil.addErrorMessage(request, errorMessage);
		halt(403, JsonTransformer.renderWithoutContent(request));
	}

	/**
	 * This is a route that can be used if no special content is returned besides
	 * those objects contained in any response.
	 */
	static Route emptyRoute = (request, response) -> {
		String content = JsonTransformer.renderWithoutContent(request);
		response.body(content);
		return content;
	};

}
