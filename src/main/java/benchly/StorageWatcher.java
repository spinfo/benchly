package benchly;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.database.StorageDao;
import benchly.model.StorageConfig;
import benchly.remote.StorageConfigRefreshTask;

/**
 * On each run selects up to one storage config from the database and refreshes
 * the file meta if it is too old.
 */
class StorageWatcher implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(StorageWatcher.class);

	private final int secondsTillCheck;

	private final ExecutorService executor;

	protected StorageWatcher(ExecutorService executor, int secondsTillCheckup) {
		this.secondsTillCheck = secondsTillCheckup;
		this.executor = executor;
	}

	@Override
	public void run() {
		try {
			StorageConfig config = StorageDao.fetchOneWhereLastRefreshIsLongerAgoThan(secondsTillCheck);

			if (config == null) {
				return;
			}

			LOG.debug("Scheduling check for config: " + config.getId());

			executor.submit(new StorageConfigRefreshTask(config));
		} catch (Exception e) {
			LOG.error("Unexpected error in storag file meta check: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
