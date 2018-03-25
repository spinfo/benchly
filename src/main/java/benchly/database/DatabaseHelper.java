package benchly.database;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import benchly.model.AdminMessage;
import benchly.model.Job;
import benchly.model.JobMessage;
import benchly.model.ServerContact;
import benchly.model.StatusReport;
import benchly.model.StorageConfig;
import benchly.model.StorageFileMeta;
import benchly.model.StoragePermission;
import benchly.model.User;
import benchly.model.Workflow;

/**
 * A singleton class providing access to the sqlite database.
 */
class DatabaseHelper {

	private static final Logger LOG = LoggerFactory.getLogger(DatabaseHelper.class);

	private Dao<Workflow, Long> workflowDao = null;
	private Dao<User, Long> userDao = null;
	private Dao<Job, Long> jobDao = null;
	private Dao<StorageConfig, Long> storageConfigDao = null;
	private Dao<StoragePermission, Long> storagePermissionDao = null;
	private Dao<StorageFileMeta, Long> storageFileMetaDao = null;
	private Dao<ServerContact, Long> serverContactDao = null;
	private Dao<StatusReport, Long> statusReportDao = null;
	private Dao<AdminMessage, Long> adminMessageDao = null;
	private Dao<JobMessage, String> jobMessageDao = null;

	private ConnectionSource connectionSource;

	private static DatabaseHelper instance = null;

	private DatabaseHelper(final String jdbcUrl) {
		try {
			connectionSource = new JdbcConnectionSource(jdbcUrl);
			InitialSetup.setupIfNecessary(connectionSource);
		} catch (SQLException e) {
			LOG.error("Could not setup db: " + e.getMessage());
		}
	}

	static DatabaseHelper getInstance() {
		if (DatabaseHelper.instance == null) {
			instance = new DatabaseHelper(
					"jdbc:mysql://benchly:secret@localhost:3306/benchly?serverTimezone=Europe/Berlin");
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

	protected Dao<StorageConfig, Long> getStorageConfigDao() {
		if (this.storageConfigDao == null) {
			this.storageConfigDao = this.getMyDaoRuntimeExcept(connectionSource, StorageConfig.class);
		}
		return this.storageConfigDao;
	}

	protected Dao<StoragePermission, Long> getStoragePermissionDao() {
		if (this.storagePermissionDao == null) {
			this.storagePermissionDao = this.getMyDaoRuntimeExcept(connectionSource, StoragePermission.class);
		}
		return this.storagePermissionDao;
	}

	protected Dao<StorageFileMeta, Long> getStorageFileMetaDao() {
		if (this.storageFileMetaDao == null) {
			this.storageFileMetaDao = this.getMyDaoRuntimeExcept(connectionSource, StorageFileMeta.class);
		}
		return this.storageFileMetaDao;
	}

	protected Dao<ServerContact, Long> getServerContactDao() {
		if (this.serverContactDao == null) {
			this.serverContactDao = this.getMyDaoRuntimeExcept(connectionSource, ServerContact.class);
		}
		return this.serverContactDao;
	}

	protected Dao<StatusReport, Long> getStatusReportDao() {
		if (this.statusReportDao == null) {
			this.statusReportDao = this.getMyDaoRuntimeExcept(connectionSource, StatusReport.class);
		}
		return this.statusReportDao;
	}

	protected Dao<AdminMessage, Long> getAdminMessageDao() {
		if (this.adminMessageDao == null) {
			this.adminMessageDao = this.getMyDaoRuntimeExcept(connectionSource, AdminMessage.class);
		}
		return this.adminMessageDao;
	}

	protected Dao<JobMessage, String> getJobMessageDao() {
		if (this.jobMessageDao == null) {
			this.jobMessageDao = this.getMyDaoRuntimeExcept(connectionSource, JobMessage.class);
		}
		return this.jobMessageDao;
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
