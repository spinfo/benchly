package benchly.remote;

import java.sql.SQLException;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import benchly.database.StorageDao;
import benchly.model.StorageConfig;
import benchly.model.StorageFileMeta;

/**
 * Updates file information for a storage config
 * @author david
 *
 */
public class StorageConfigRefreshTask implements Runnable {
	
	private static final Logger LOG = LoggerFactory.getLogger(StorageConfigRefreshTask.class);

	private final StorageConfig config;
	
	public StorageConfigRefreshTask(StorageConfig config) {
		this.config = config;
	}
	
	@Override
	public void run() {
		Set<StorageFileMeta> fileMeta = StorageAccess.getInstance().getFilesMeta(config);
		try {
			StorageDao.updateStorageFileMeta(config, fileMeta);
		} catch (SQLException e) {
			LOG.error("Unable to persist Storage information for storage config on refresh: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
