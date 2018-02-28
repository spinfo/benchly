package benchly.controller;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.UserDao;
import benchly.error.ResourceNotFoundError;
import benchly.model.User;
import benchly.util.RequestUtil;
import benchly.util.RequestUtil.PaginationParams;
import benchly.util.SessionUtil;
import spark.Request;
import spark.Response;
import spark.Route;

public class UserController extends Controller {

	private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

	public static Route create = (request, response) -> {
		ensureAdminUser(request, "Only an admin may create additional users.");

		User user = JsonTransformer.readRequestBody(request.body(), User.class);
		user.initializeHashAndSalt();
		ensureValidModel(user, request);

		if (UserDao.userWithEmailExists(user.getEmail())) {
			SessionUtil.addErrorMessage(request, "A user with that email already exists.");
			response.status(400);
			return emptyRoute.handle(request, response);
		}

		UserDao.create(user);

		return JsonTransformer.render(user, request);
	};

	public static Route index = (request, response) -> {
		ensureLoggedInUser(request, "Only logged in users may view other users.");

		PaginationParams pagination = RequestUtil.parsePaginationParams(request);
		List<User> users = UserDao.fetchAll(pagination);
		long amount = UserDao.count();

		return JsonTransformer.renderPaginatedResult(users, request, pagination.limit, pagination.offset, amount);
	};
	
	public static Route show = (request, response) -> {
		ensureLoggedInUser(request, "Only logged in users may view other users information.");
		User user = ensureUserFromRequesParams(request);
		return JsonTransformer.render(user, request);
	};

	public static Route update = (request, response) -> {
		User executing = ensureLoggedInUser(request, "Only logged in users may update other users.");
		User input = JsonTransformer.readRequestBody(request.body(), User.class);
		User target = ensureUserFromRequesParams(request);

		// check that permissions are ok
		if (!userMayEdit(executing, target)) {
			haltForbbiden(request, "Insufficient permissions to edit this user.");
		}

		// set all changeable values
		target.setName(input.getName());
		target.setEmail(input.getEmail());
		if (input.getPassword() != null) {
			target.setPassword(input.getPassword());
			target.initializeHashAndSalt();
		}
		if (executing.isAdmin()) {
			target.setAdmin(input.isAdmin());
		}

		// attempt the update
		long rowCount = UserDao.update(target);
		ensureRowCountIsOne(rowCount, "update user");

		return JsonTransformer.render(target, request);
	};

	public static Route delete = (request, response) -> {
		User executing = ensureLoggedInUser(request, "Only logged-in users may delete a user.");

		User target = ensureUserFromRequesParams(request);

		// check that permissions are ok
		if (!userMayEdit(executing, target)) {
			haltForbbiden(request, "Insufficient permissions to delete user.");
		}

		// actually delete, check on ids match before they might be modified
		boolean userDeletesSelf = target.getId() == executing.getId();
		int rowCount = UserDao.setDeleted(target);
		ensureRowCountIsOne(rowCount, "user delete");

		LOG.debug("Deleted user: " + target.getName());

		// delegate to logout if the user deleted him- or herself
		if (userDeletesSelf) {
			return SessionController.logout.handle(request, response);
		} else {
			return JsonTransformer.render(target, request);
		}
	};

	private static User ensureUserFromRequesParams(Request request) throws SQLException, ResourceNotFoundError {
		long userId = RequestUtil.parseIdParam(request);
		User user = UserDao.fetchById(userId);

		// check that the target exists
		if (user == null) {
			throw new ResourceNotFoundError("No user with id: " + userId);
		}

		return user;
	}

	private static boolean userMayEdit(User editing, User toEdit) {
		return (editing.getId() == toEdit.getId()) || editing.isAdmin();
	}

}
