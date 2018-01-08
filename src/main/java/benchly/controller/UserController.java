package benchly.controller;

import java.util.Map;

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

import benchly.database.UserDao;
import benchly.model.User;
import benchly.util.Path;
import benchly.util.SessionUtil;
import benchly.util.Views;
import spark.Request;
import spark.Response;
import spark.Route;

public class UserController extends Controller {

	private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

	public static Route showLogin = (Request request, Response response) -> {
		String redirectTarget = SessionUtil.getLastLocationOrDefault(request, Path.Web.INDEX);
		Map<String, Object> viewContext = Views.newContextWith("redirectTarget", redirectTarget);
		return Views.render(request, viewContext, Path.Template.LOGIN);
	};

	public static Route login = (Request request, Response response) -> {

		String nameOrEmail = request.queryParams("nameOrEmail");
		LOG.debug("Login attempt with name or email: " + nameOrEmail);

		try {
			User user = UserDao.fetchByNameOrEmail(nameOrEmail);
			if (user == null) {
				throw new UnknownAccountException();
			}
			doLogin(user, request.queryParams("password"));
			LOG.debug("Established a new session for user: " + user.getName());
			// Add the authenticated user to our session
			SessionUtil.setCurrentUser(request, user);
		} catch (UnknownAccountException | IncorrectCredentialsException e) {
			logGeneralLoginError(e);
			SessionUtil.addWarningMessage(request, "Incorrect username or password.");
		} catch (LockedAccountException e) {
			logGeneralLoginError(e);
			SessionUtil.addWarningMessage(request, "Your account is locked");
		} catch (ExcessiveAttemptsException e) {
			logGeneralLoginError(e);
			SessionUtil.addWarningMessage(request, "Excessive login attempts. Pleas try again later.");
		} catch (AuthenticationException e) {
			String message = "Unexpected authentication exception: " + e.getMessage();
			LOG.error(message);
			SessionUtil.addErrorMessage(request, message);
		}

		String redirectTarget = request.queryParamOrDefault("redirectTarget", Path.Web.INDEX);
		response.redirect(redirectTarget);
		return Views.EMPTY_REPSONSE;
	};

	public static Route logout = (Request request, Response response) -> {
		SessionUtil.removeCurrentUser(request);
		response.redirect(Path.Web.LOGIN);
		return Views.render(request, Views.newContext(), Path.Template.LOGIN);
	};

	private static void logGeneralLoginError(AuthenticationException e) {
		LOG.warn("Not logging in user (" + e.getClass() + "): " + e.getMessage());
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
