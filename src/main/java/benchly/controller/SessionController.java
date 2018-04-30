package benchly.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.Expose;

import benchly.database.UserDao;
import benchly.error.InternalServerError;
import benchly.error.ResourceNotFoundError;
import benchly.model.User;
import benchly.util.SessionUtil;
import spark.Request;
import spark.Response;
import spark.Route;

public class SessionController extends Controller {

	private static final Logger LOG = LoggerFactory.getLogger(SessionController.class);

	// a mini POJO for the login request
	protected static class LoginRequest {
		@Expose(serialize = false)
		String email, password;
	}

	public static Route login = (Request request, Response response) -> {
		LoginRequest loginData = JsonTransformer.readRequestBody(request.body(), LoginRequest.class);

		try {
			User user = UserDao.fetchByEmail(loginData.email);
			if (user == null) {
				throw new UnknownAccountException();
			}
			doLogin(user, loginData.password);
			LOG.debug("Established a new session for user: " + user.getName());
			// Add the authenticated user to our session
			SessionUtil.setCurrentUser(request, user);
		} catch (UnknownAccountException | IncorrectCredentialsException e) {
			handleFailedLogin(e, request, response, "Incorrect email or password.");
		} catch (LockedAccountException e) {
			handleFailedLogin(e, request, response, "Your account is locked");
		} catch (ExcessiveAttemptsException e) {
			handleFailedLogin(e, request, response, "Excessive login attempts. Please try again later.");
		} catch (AuthenticationException e) {
			throw new InternalServerError("Unexpected authentication exception on login.", e);
		}

		return emptyRoute.handle(request, response);
	};

	public static Route logout = (Request request, Response response) -> {
		SessionUtil.removeCurrentUser(request);
		silentlyLogoutUser();
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

	private static void handleFailedLogin(AuthenticationException e, Request request, Response response,
			String userMessage) {
		LOG.warn("Not logging in user (" + e.getClass() + "): " + e.getMessage());
		SessionUtil.addWarningMessage(request, userMessage);
		response.status(403);
	}

	private static void doLogin(User user, String password) throws AuthenticationException {
		silentlyLogoutUser();

		// Create a new subject
		Subject subject = SecurityUtils.getSubject();
		subject.getSession(true);

		// Create an authentication token
		UsernamePasswordToken token = new UsernamePasswordToken(user.getEmail(), password);
		token.setRememberMe(false);

		// Authenticate the subject, this throws on failure
		subject.login(token);
	}

	// tries to logout the user without complaining if anything goes wrong
	private static void silentlyLogoutUser() {
		try {
			// Get the user if one is logged in.
			Subject subject = SecurityUtils.getSubject();
			if (subject == null) {
				LOG.debug("No user to logout.");
				return;
			}

			// Log the user out and kill their session if possible.
			subject.logout();
			LOG.debug("Logged out user: " + subject);
			Session session = subject.getSession(false);
			if (session != null) {
				LOG.debug("Stopping previous session.");
				session.stop();
			}
		} catch (Exception e) {
			LOG.warn("Warning, error when silently logging out user: " + e.getMessage());
		}
	}

}
