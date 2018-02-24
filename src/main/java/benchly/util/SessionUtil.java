package benchly.util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.model.User;
import benchly.model.UserMessage;
import benchly.model.UserMessage.Type;
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

	public static List<UserMessage> getOrCreateMessages(Request request) {
		List<UserMessage> messages = request.session().attribute("userMessages");
		if (messages == null) {
			messages = new ArrayList<>();
			request.session().attribute("userMessages", messages);
		}
		return messages;
	}

	public static List<UserMessage> clearUserMessages(Request request) {
		List<UserMessage> messages = getOrCreateMessages(request);
		request.session().removeAttribute("userMessages");
		return messages;
	}

	private static void addUserMessage(Request request, UserMessage.Type type, String message) {
		LOG.debug("Adding message (" + type.name() + "): " + message);
		getOrCreateMessages(request).add(new UserMessage(type, message));
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

}
