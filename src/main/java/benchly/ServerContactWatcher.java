package benchly;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.ServerContactDao;
import benchly.model.ServerContact;
import benchly.remote.ServerStatusReportTask;

/**
 * On each run selects up to one server from the database and checks the
 * server's status.
 */
class ServerContactWatcher implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ServerContactWatcher.class);

	private final int secondsTillCheck;

	private final ExecutorService executor;

	protected ServerContactWatcher(ExecutorService executor, int secondsTillCheckup) {
		this.secondsTillCheck = secondsTillCheckup;
		this.executor = executor;
	}

	@Override
	public void run() {

		ServerContact contact = null;
		try {
			contact = ServerContactDao.pickOneReachableWhereLastCheckIsLongerAgoThan(secondsTillCheck);

			if (contact == null) {
				return;
			}

			executor.submit(new ServerStatusReportTask(contact));
		} catch (Exception e) {
			LOG.error("Unexpected error in server status check: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
