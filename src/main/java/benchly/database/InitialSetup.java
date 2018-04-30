package benchly.database;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

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

class InitialSetup {

	private static final Logger LOG = LoggerFactory.getLogger(InitialSetup.class);

	protected static void setupTables(final ConnectionSource connectionSource) throws SQLException {
		TableUtils.createTableIfNotExists(connectionSource, Workflow.class);
		TableUtils.createTableIfNotExists(connectionSource, User.class);
		TableUtils.createTableIfNotExists(connectionSource, Job.class);
		TableUtils.createTableIfNotExists(connectionSource, StorageConfig.class);
		TableUtils.createTableIfNotExists(connectionSource, StoragePermission.class);
		TableUtils.createTableIfNotExists(connectionSource, StorageFileMeta.class);
		TableUtils.createTableIfNotExists(connectionSource, ServerContact.class);
		TableUtils.createTableIfNotExists(connectionSource, StatusReport.class);
		TableUtils.createTableIfNotExists(connectionSource, AdminMessage.class);
		TableUtils.createTableIfNotExists(connectionSource, JobMessage.class);
	}

	protected static void insertDefaultAdminUser() {
		try {
			long userCount = UserDao.count();
			if (userCount < 1) {
				User defaultAdmin = new User("benchly-admin", "mail@example.com", "change-this-password");
				defaultAdmin.setAdmin(true);
				UserDao.create(defaultAdmin);
			}
		} catch (SQLException e) {
			LOG.error("Unable to check for or enable the default admin user.");
			e.printStackTrace();
		}
	}

}
