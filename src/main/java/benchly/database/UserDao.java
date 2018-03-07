package benchly.database;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import benchly.model.User;
import benchly.util.RequestUtil.PaginationParams;

public class UserDao {

	public static long count() throws SQLException {
		return dao().countOf();
	}

	public static int create(User user) throws SQLException {
		user.setUpdatedAt(user.getCreatedAt());
		return dao().create(user);
	}

	public static int update(User user) throws SQLException {
		user.setUpdatedAtNow();
		return dao().update(user);
	}

	public static boolean userWithEmailExists(String email) throws SQLException {
		return (fetchByEmail(email) != null);
	}

	public static List<User> fetchAll(PaginationParams pagination) throws SQLException {
		QueryBuilder<User, Long> builder = dao().queryBuilder();
		builder.limit(pagination.limit).offset(pagination.offset);
		builder.setWhere(whereNonDeleted());
		return builder.query();
	}
	
	public static User fetchById(long id) throws SQLException {
		return queryForFirstWhereEq("id", id);
	}

	public static User fetchByEmail(String email) throws SQLException {
		return queryForFirstWhereEq("email", email);
	}

	public static User fetchByHash(String hash) throws SQLException {
		return queryForFirstWhereEq("passwordHash", hash);
	}
	
	public static int setDeleted(User user) throws SQLException {
		int result = TransactionManager.callInTransaction(dao().getConnectionSource(), new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				// delete all information about the users storage for good
				StorageDao.deleteAllWithOwner(user);
				WorkflowDao.setDeletedWhereAuthorIs(user);
				
				// remove personal information from the user and set the deletion flag
				user.setEmail(null);
				user.setPasswordHash("deleted-users-hash");
				user.setDeleted(true);
				
				// actually update the user
				return update(user);
			}
		});
		return result;
	}

	// used as the default where to exclude users that are set to deleted
	private static Where<User, Long> whereNonDeleted() throws SQLException {
		return dao().queryBuilder().where().eq("isDeleted", false);
	}

	private static User queryForFirstWhereEq(String attribute, Object value) throws SQLException {
		return whereNonDeleted().and().eq(attribute, value).queryForFirst();
	}

	private static Dao<User, Long> dao() {
		return DatabaseHelper.getInstance().getUserDao();
	}
}
