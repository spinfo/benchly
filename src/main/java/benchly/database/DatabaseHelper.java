package benchly.database;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import benchly.TestEntrySetup;
import benchly.model.Job;
import benchly.model.StorageConfig;
import benchly.model.User;
import benchly.model.Workflow;

/**
 * A singleton class providing access to the sqlite database.
 * 
 * TODO: Does this need a close() or exit() method?...
 */
class DatabaseHelper {

	private static final Logger LOG = LoggerFactory.getLogger(DatabaseHelper.class);

	private Dao<Workflow, Long> workflowDao = null;
	private Dao<User, Long> userDao = null;
	private Dao<Job, Long> jobDao = null;
	
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
	
	static DatabaseHelper getInstance() {
		if (DatabaseHelper.instance == null) {
			instance = new DatabaseHelper("jdbc:sqlite:/tmp/benchly.db");
		}
		return instance;
	}

	protected Dao<Workflow, Long> getWorkflowDao() {
		if (this.workflowDao == null) {
			this.workflowDao = this.getMyDaoRuntimeExcept(connectionSource, Workflow.class);
		}
		return this.workflowDao;
	}
	
	protected Dao<User, Long> getUserDao() {
		if (this.userDao == null) {
			this.userDao = this.getMyDaoRuntimeExcept(connectionSource, User.class);
		}
		return this.userDao;
	}
	
	protected Dao<Job, Long> getJobDao() {
		if (this.jobDao == null) {
			this.jobDao = this.getMyDaoRuntimeExcept(connectionSource, Job.class);
		}
		return this.jobDao;
	}
	
	
	private void setupTables(final ConnectionSource connectionSource) throws SQLException {
		// TODO: Remove next lines
		TableUtils.dropTable(connectionSource, User.class, true);
		TableUtils.dropTable(connectionSource, Workflow.class, true);
		TableUtils.dropTable(connectionSource, Job.class, true);
		TableUtils.dropTable(connectionSource, StorageConfig.class, true);

		TableUtils.createTableIfNotExists(connectionSource, Workflow.class);
		TableUtils.createTableIfNotExists(connectionSource, User.class);
		TableUtils.createTableIfNotExists(connectionSource, Job.class);
		
		// TODO: Remove
		TestEntrySetup.setup();
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
