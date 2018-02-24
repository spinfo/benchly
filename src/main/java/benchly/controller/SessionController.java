package benchly.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import benchly.database.UserDao;
import benchly.error.InternalServerError;
import benchly.error.InvalidRequestException;
import benchly.error.ResourceNotFoundError;
import benchly.model.User;
import benchly.util.SessionUtil;
import spark.Request;
import spark.Response;
import spark.Route;

public class SessionController extends Controller {

	private static final Logger LOG = LoggerFactory.getLogger(SessionController.class);

	public static Route login = (Request request, Response response) -> {
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(request.body()).getAsJsonObject();
		
		String nameOrEmail = "";
		String password = "";
		try {
			nameOrEmail = json.get("nameOrEmail").getAsString();
			password = json.get("password").getAsString();
		} catch (Exception e) {
			throw new InvalidRequestException(e);
		}

		try {
			User user = UserDao.fetchByNameOrEmail(nameOrEmail);
			if (user == null) {
				throw new UnknownAccountException();
			}
			doLogin(user, password);
			LOG.debug("Established a new session for user: " + user.getName());
			// Add the authenticated user to our session
			SessionUtil.setCurrentUser(request, user);
		} catch (UnknownAccountException | IncorrectCredentialsException e) {
			handleFailedLogin(e, request, response, "Incorrect username or password.");
		} catch (LockedAccountException e) {
			handleFailedLogin(e, request, response, "Your account is locked");
		} catch (ExcessiveAttemptsException e) {
			handleFailedLogin(e, request, response, "Excessive login attempts. Pleas try again later.");
		} catch (AuthenticationException e) {
			throw new InternalServerError("Unexpected authentication exception on login.", e);
		}

		return emptyRoute.handle(request, response);
	};

	public static Route logout = (Request request, Response response) -> {
		SessionUtil.removeCurrentUser(request);
		return emptyRoute.handle(request, response);
	};
	
	public static Route showLoggedInUser = (request, response) -> {
		User user = SessionUtil.getCurrentUser(request);
		if (user == null) {
			throw new ResourceNotFoundError("No user for the current session.");
		} else {
			return JsonTransformer.render(user, request);
		}
	};
	
	private static void handleFailedLogin(AuthenticationException e, Request request, Response response, String userMessage) {
		LOG.warn("Not logging in user (" + e.getClass() + "): " + e.getMessage());
		SessionUtil.addWarningMessage(request, userMessage);
		response.status(403);
	}

	private static void doLogin(User user, String password) throws AuthenticationException {
		// Create a new subject
		// We could create a shiro session at this point, but our application
		// uses spark's session, so do not use "subject.getSession(true)"
		Subject subject = SecurityUtils.getSubject();

		// Create an authentication token
		UsernamePasswordToken token = new UsernamePasswordToken(user.getName(), password);
		token.setRememberMe(true);

		// Authenticate the subject, this throws on failure
		subject.login(token);
	}

}