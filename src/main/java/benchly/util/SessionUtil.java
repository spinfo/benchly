package benchly.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.model.User;
import benchly.util.UserMessages.Type;
import io.mikael.urlbuilder.UrlBuilder;
import spark.Request;

public class SessionUtil {

	private static final Logger LOG = LoggerFactory.getLogger(SessionUtil.class);

	public static User getCurrentUser(Request request) {
		return request.session().attribute("user");
	}

	public static void setCurrentUser(Request request, User user) {
		request.session().attribute("user", user);
	}

	public static void removeCurrentUser(Request request) {
		request.session().removeAttribute("user");
	}

	public static UserMessages getOrCreateMessages(Request request) {
		UserMessages messages = request.session().attribute("userMessages");
		if (messages == null) {
			messages = new UserMessages();
			request.session().attribute("userMessages", messages);
		}
		return messages;
	}

	public static UserMessages clearUserMessages(Request request) {
		UserMessages messages = getOrCreateMessages(request);
		request.session().removeAttribute("userMessages");
		return messages;
	}

	public static void addUserMessage(Request request, Type type, String message) {
		LOG.debug("Adding message (" + type.name() + "): " + message);
		getOrCreateMessages(request).addMessage(type, message);
	}

	public static void addOkMessage(Request request, String message) {
		addUserMessage(request, Type.OK, message);
	}

	public static void addInfoMessage(Request request, String message) {
		addUserMessage(request, Type.INFO, message);
	}

	public static void addWarningMessage(Request request, String message) {
		addUserMessage(request, Type.WARN, message);
	}

	public static void addErrorMessage(Request request, String message) {
		addUserMessage(request, Type.ERROR, message);
	}

	public static void addLastLocation(Request request) {
		String location = UrlBuilder.fromString("").withPath(request.pathInfo()).withQuery(request.queryString())
				.toString();
		request.session().attribute("lastLocation", location);
	}

	public static String getLastLocation(Request request) {
		return getLastLocationOrDefault(request, Path.Web.INDEX);
	}

	public static String getLastLocationOrDefault(Request request, String defaultPath) {
		String location = request.session().attribute("lastLocation");
		if (location == null) {
			location = defaultPath;
		}
		return location;
	}

}
