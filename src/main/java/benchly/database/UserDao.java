package benchly.database;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.Dao;

import benchly.model.User;

public class UserDao {

	private static final Logger LOG = LoggerFactory.getLogger(UserDao.class);

	public static User fetchByName(String name) {
		return queryForFirstWhereEq("name", name);
	}

	public static User fetchByNameOrEmail(String nameOrEmail) {
		User user = fetchByName(nameOrEmail);
		if (user == null) {
			user = fetchByEmail(nameOrEmail);
		}
		return user;
	}

	public static User fetchByEmail(String email) {
		return queryForFirstWhereEq("email", email);
	}

	public static User fetchByHash(String hash) {
		return queryForFirstWhereEq("passwordHash", hash);
	}

	private static User queryForFirstWhereEq(String attribute, Object value) {
		try {
			return dao().queryForFirst(dao().queryBuilder().where().eq(attribute, value).prepare());
		} catch (SQLException e) {
			e.printStackTrace();
			LOG.error(e.getMessage());
			return null;
		}
	}

	private static Dao<User, Long> dao() {
		return DatabaseHelper.getInstance().getUserDao();
	}
}
