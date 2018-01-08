package benchly.database;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import benchly.model.User;
import benchly.model.Workflow;

/**
 * A singleton class providing access to the sqlite database.
 */
class DatabaseHelper {
	
	private static final User[] users = {
			new User("bob", "bob@example.com", "secret"),
			new User("alice", "alice@example.com", "secret")
	};
	
	private static final Workflow[] workflows = new Workflow[4];
	static {
		for (int i = 1; i <= 4; i++) {
			workflows[i-1] = new Workflow("test-name" + i, "{ testdefinition: " + i + " }", users[0]);
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(DatabaseHelper.class);

	private Dao<Workflow, Long> workflowDao = null;
	private Dao<User, Long> userDao = null;
	
	private ConnectionSource connectionSource;

	private static DatabaseHelper instance = null;
	
	private DatabaseHelper(final String jdbcUrl) {
		try {
			connectionSource = new JdbcConnectionSource(jdbcUrl);
			setupTables(connectionSource);
		} catch (SQLException e) {
			LOG.error("Could not setup db: " + e.getMessage());
		}
	}
	
	public static DatabaseHelper getInstance() {
		if (DatabaseHelper.instance == null) {
			instance = new DatabaseHelper("jdbc:sqlite:/tmp/benchly.db");
		}
		return instance;
	}

	public Dao<Workflow, Long> getWorkflowDao() {
		if (this.workflowDao == null) {
			this.workflowDao = this.getMyDaoRuntimeExcept(connectionSource, Workflow.class);
		}
		return this.workflowDao;
	}
	
	public Dao<User, Long> getUserDao() {
		if (this.userDao == null) {
			this.userDao = this.getMyDaoRuntimeExcept(connectionSource, User.class);
		}
		return this.userDao;
	}
	
	private void setupTables(final ConnectionSource connectionSource) throws SQLException {
		// TODO: Remove next two lines
		TableUtils.dropTable(connectionSource, User.class, true);
		TableUtils.dropTable(connectionSource, Workflow.class, true);
		TableUtils.createTableIfNotExists(connectionSource, Workflow.class);
		TableUtils.createTableIfNotExists(connectionSource, User.class);
		
		// Test stuff, TODO: Remove
		TableUtils.clearTable(connectionSource, User.class);
		Dao<User, Long> userDao = getUserDao();
		for (final User u : users) {
			int amount = userDao.create(u);
			LOG.debug("Created users in db: " + amount);
		}
		
		TableUtils.clearTable(connectionSource, Workflow.class);
		Dao<Workflow, Long> dao = getWorkflowDao();
		for (final Workflow w : workflows) {
			dao.create(w);
		}
	}

	// convenience method that wraps the SQL Exception on Dao Creation
	private <D extends Dao<T, ?>, T> D getMyDaoRuntimeExcept(ConnectionSource connectionSource, Class<T> clazz) {
		D result = null;
		try {
			result = DaoManager.createDao(connectionSource, clazz);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	

}
