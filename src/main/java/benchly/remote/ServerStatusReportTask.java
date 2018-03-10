package benchly.remote;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.AdminMessageDao;
import benchly.database.ServerContactDao;
import benchly.error.ServerAccessError;
import benchly.model.AdminMessage;
import benchly.model.ServerContact;
import benchly.model.StatusReport;

/**
 * A task to query a remote server for it's status and save the report to the
 * database.
 */
public class ServerStatusReportTask implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ServerStatusReportTask.class);

	private final ServerContact contact;

	public ServerStatusReportTask(ServerContact contact) {
		this.contact = contact;
	}

	@Override
	public void run() {
		try {
			StatusReport report = ServerAccess.fetchStatus(contact);
			ServerContactDao.createReport(contact, report);
			LOG.debug("Logged status for server: " + contact.getEndpoint() + ", reports running jobs: "
					+ report.getRunningJobs());
		} catch (ServerAccessError e) {
			String message = "Unable to contact Server for report on: %s, got: %s";
			reportServerUnreachable(contact, String.format(message, contact.getEndpoint(), e.getMessage()));
		} catch (SQLException e) {
			LOG.error("Unexpected error on querying the database for a server contact to check: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void reportServerUnreachable(ServerContact contact, String message) {
		try {
			contact.setUnreachableNow();
			ServerContactDao.update(contact);
		} catch (SQLException e) {
			LOG.error("Unable to set server as unreachable: " + e.getMessage());
			e.printStackTrace();
		}
		AdminMessageDao.createOrLogOnFailure(new AdminMessage(contact, message));
	}

}
