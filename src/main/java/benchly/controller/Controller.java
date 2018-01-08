package benchly.controller;

import static spark.Spark.halt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.error.InternalServerError;
import benchly.model.User;
import benchly.util.Path;
import benchly.util.SessionUtil;
import benchly.util.Views;
import spark.Request;

public class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

	static void ensureRowCountIsOne(int rowCount, String verb) throws InternalServerError {
		if (rowCount != 1) {
			throw new InternalServerError("Wrong row count on workflow " + verb + ": " + rowCount);
		}
	}

	static User ensureLoggedInUser(Request request, String errorMessage) {
		User user = SessionUtil.getCurrentUser(request);
		LOG.debug("Got user from session: " + user);
		if (user == null) {
			SessionUtil.addErrorMessage(request, errorMessage);
			halt(403, Views.render(request, Views.newContext(), Path.Template.STATUS_403));
		}
		return user;
	}

}
