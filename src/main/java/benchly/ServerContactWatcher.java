package benchly;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.AdminMessageDao;
import benchly.database.ServerContactDao;
import benchly.error.ServerAccessError;
import benchly.model.AdminMessage;
import benchly.model.ServerContact;
import benchly.model.ServerContact.Reachability;
import benchly.model.StatusReport;
import benchly.remote.ServerAccess;

/**
 * On each run selects up to one server from the database and checks the
 * server's status.
 */
class ServerContactWatcher implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ServerContactWatcher.class);

	// set a threshold time of until a contact should be made
	private static final int TRESHOLD = 1 * 60;

	@Override
	public void run() {
		
		ServerContact contact = null;
		try {
			contact = ServerContactDao.pickOneReachableWhereLastCheckIsLongerAgoThan(TRESHOLD);
		} catch (Exception e) {
			LOG.error("Unable to pick a server for contact: " + e.getMessage());
			e.printStackTrace();
		}

		if (contact == null) {
			return;
		}

		try {
			StatusReport report = ServerAccess.fetchStatus(contact);
			ServerContactDao.createReport(contact, report);
			LOG.debug("Logged status for server: " + contact.getEndpoint() + ", reports running jobs: "
					+ report.getRunningJobs());
		} catch (ServerAccessError e) {
			String message = "Unable to contact Server for report on: %s, got: %s";
			reportServerUnreachable(contact, String.format(message, contact.getEndpoint(), e.getMessage()));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void reportServerUnreachable(ServerContact contact, String message) {
		try {
			contact.setReachbility(Reachability.REPORTED_REACHABLE);
			ServerContactDao.update(contact);
		} catch (SQLException e) {
			LOG.error("Unable to set server as unreachable: " + e.getMessage());
			e.printStackTrace();
		}
		AdminMessageDao.createOrLogOnFailure(new AdminMessage(contact, message));
	}

}
