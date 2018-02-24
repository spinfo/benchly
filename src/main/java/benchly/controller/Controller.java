package benchly.controller;

import static spark.Spark.halt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.error.InternalServerError;
import benchly.model.User;
import benchly.util.SessionUtil;
import spark.Request;
import spark.Route;

abstract class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

	static void ensureRowCountIsOne(long rowsCreated, String verb) throws InternalServerError {
		if (rowsCreated != 1) {
			throw new InternalServerError("Wrong row count on workflow " + verb + ": " + rowsCreated);
		}
	}

	static User ensureLoggedInUser(Request request, String errorMessage) {
		User user = SessionUtil.getCurrentUser(request);
		LOG.debug("Got user from session: " + user);
		if (user == null) {
			SessionUtil.addErrorMessage(request, errorMessage);
			halt(403, JsonTransformer.renderWithoutContent(request));
		}
		return user;
	}

	/**
	 * This is a route that cen be used if no special content is returned besides
	 * those objects contained in any response.
	 */
	static Route emptyRoute = (request, response) -> {
		return JsonTransformer.renderWithoutContent(request);
	};

}
