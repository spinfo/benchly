package benchly.database;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;

import benchly.model.StorageConfig;
import benchly.model.StorageFileMeta;
import benchly.model.StoragePermission;
import benchly.model.User;

public class StorageDao {

	private static final Logger LOG = LoggerFactory.getLogger(StorageDao.class);

	public static StorageConfig fetchConfig(long id) throws SQLException {
		return dao().queryForId(id);
	}

	public static int create(StorageConfig config) throws SQLException {
		// adjust the timestamp to either be the creation date or now
		config.setUpdatedAt(config.getCreatedAt());
		return dao().create(config);
	}

	public static int update(StorageConfig config) throws SQLException {
		config.setUpdatedAtNow();
		return dao().update(config);
	}

	public static StorageConfig fetchOneWhereLastRefreshIsLongerAgoThan(long seconds) throws SQLException {
		Timestamp threshold = Timestamp.from(Instant.now().minusSeconds(seconds));

		QueryBuilder<StorageConfig, Long> builder = dao().queryBuilder();
		builder.where().isNull("refreshedAt").or().le("refreshedAt", threshold);
		return builder.queryForFirst();
	}

	public static List<StorageFileMeta> fetchFilesMeta(StorageConfig config) throws SQLException {
		return fileMetaDao().queryForEq("storageConfig", config);
	}

	public static boolean userHasAccess(User user, StorageConfig config) throws SQLException {
		return !permissionDao().queryBuilder().where().eq("storageConfig", config).and().eq("user", user).query()
				.isEmpty();
	}

	public static List<StorageConfig> fetchAcessible(User user) throws SQLException {
		// get a query builder that retrieves config ids for a user
		QueryBuilder<StoragePermission, Long> permissionQb = permissionDao().queryBuilder();
		permissionQb.selectColumns("storageConfig").where().eq("user", user);

		// query for all configs that are either owned by the user or she has permission
		// to access
		return dao().queryBuilder().where().eq("owner", user).or().in("id", permissionQb).query();
	}

	public static List<StorageConfig> fetchWhereOwner(User owner) throws SQLException {
		return dao().queryBuilder().where().eq("owner", owner).query();
	}

	public static int delete(List<StorageConfig> configs) throws SQLException {
		// collect the ids to delete by
		Set<Long> ids = configs.stream().map(c -> c.getId()).collect(Collectors.toSet());

		int result = TransactionManager.callInTransaction(dao().getConnectionSource(), new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {
				if (!ids.isEmpty()) {
					// prepare the permissions and file meta deletes
					DeleteBuilder<StoragePermission, Long> permissionDelete = permissionDao().deleteBuilder();
					DeleteBuilder<StorageFileMeta, Long> fileMetaDelete = fileMetaDao().deleteBuilder();
					permissionDelete.where().in("storageConfig", ids);
					fileMetaDelete.where().in("storageConfig", ids);
					permissionDelete.delete();
					fileMetaDelete.delete();
				}
				return dao().delete(configs);
			}
		});
		return result;
	}

	public static int delete(StorageConfig config) throws SQLException {
		return delete(Arrays.asList(config));
	}

	public static int deleteAllWithOwner(User owner) throws SQLException {
		// this is inefficient, but seldom used
		return delete(fetchWhereOwner(owner));
	}

	public static int updateAccessPermissions(StorageConfig config, Set<StoragePermission> permissions)
			throws SQLException {
		// make sure that the right config id is set
		Set<StoragePermission> toPersist = permissions.stream()
				.filter(p -> p.getStorageConfig().getId() == config.getId()).collect(Collectors.toSet());
		if (toPersist.size() != permissions.size()) {
			LOG.warn("Config ids do not match on bulk update of access permissions. Ignoring invalid ids");
		}
		// actually update
		return bulkUpdateForStorageConfig(permissionDao(), config, toPersist);
	}

	// Return the file meta information given by the id, but only if it is linked to
	// the given config
	public static StorageFileMeta fetchFileMeta(StorageConfig config, long fileId) throws SQLException {
		return fileMetaDao().queryBuilder().where().eq("storageConfig", config).and().eq("id", fileId).queryForFirst();
	}

	public static int create(StorageFileMeta fileMeta) throws SQLException {
		return fileMetaDao().create(fileMeta);
	}

	public static int update(StorageFileMeta fileMeta) throws SQLException {
		return fileMetaDao().update(fileMeta);
	}

	public static int delete(StorageFileMeta fileMeta) throws SQLException {
		return fileMetaDao().delete(fileMeta);
	}

	public static int updateStorageFileMeta(StorageConfig config, Set<StorageFileMeta> fileInfos) throws SQLException {
		// make sure that the right config id is set
		Set<StorageFileMeta> toPersist = fileInfos.stream()
				.filter(meta -> meta.getStorageConfig().getId() == config.getId()).collect(Collectors.toSet());
		if (toPersist.size() != fileInfos.size()) {
			LOG.warn("Config ids do not match on bulk update of file meta information. Ignoring invalid ids");
		}
		// actually update
		int result = bulkUpdateForStorageConfig(fileMetaDao(), config, toPersist);
	
		// set refresh date on the config
		UpdateBuilder<StorageConfig, Long> builder = dao().updateBuilder();
		builder.where().idEq(config.getId());
		builder.updateColumnValue("refreshedAt", Timestamp.from(Instant.now()));
		builder.update();
		
		return result;
	}

	// bulk updates the dao's table by deleting all objects referring to the storage
	// config and then inserting the new objects
	private static <T extends Object> int bulkUpdateForStorageConfig(Dao<T, Long> dao, StorageConfig config,
			Set<T> newObjects) throws SQLException {
		DeleteBuilder<?, Long> delete = dao.deleteBuilder();
		delete.where().eq("storageConfig", config);
		int created = TransactionManager.callInTransaction(dao.getConnectionSource(), new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				delete.delete();
				return dao.create(newObjects);
			}
		});
		return created;
	}

	private static Dao<StorageConfig, Long> dao() {
		return DatabaseHelper.getInstance().getStorageConfigDao();
	}

	private static final Dao<StoragePermission, Long> permissionDao() {
		return DatabaseHelper.getInstance().getStoragePermissionDao();
	}

	private static final Dao<StorageFileMeta, Long> fileMetaDao() {
		return DatabaseHelper.getInstance().getStorageFileMetaDao();
	}

}
