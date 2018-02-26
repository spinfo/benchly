package benchly.model;

import java.sql.Timestamp;
import java.time.Instant;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "storage_permission")
public class StoragePermission extends Model {

	@DatabaseField(columnName = "id", generatedId = true)
	@Expose(deserialize = false)
	private long id;

	@DatabaseField(columnName = "storageConfig", canBeNull = false, foreign = true, foreignAutoRefresh = true, uniqueIndexName = "configToUser")
	@Expose
	private StorageConfig storageConfig;

	@DatabaseField(columnName = "user", canBeNull = false, foreign = true, foreignAutoRefresh = true, uniqueIndexName = "configToUser")
	@Expose
	private User user;

	@DatabaseField(columnName = "createdAt", canBeNull = false, index = true)
	@Expose(serialize = false)
	private Timestamp createdAt;

	protected StoragePermission() {
		// empty constructor mainly for ormlite
		this.createdAt = Timestamp.from(Instant.now());
	}
	
	protected StoragePermission(StorageConfig config, User user) {
		this();
		this.storageConfig = config;
		this.user = user;
	}
	
	public long getId() {
		return id;
	}

	public StorageConfig getStorageConfig() {
		return storageConfig;
	}

	public User getUser() {
		return user;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean validate() {
		valid = true;

		if (storageConfig == null) {
			addError("Need a storage config to set up a storage permission.");
		}
		if (user == null) {
			addError("Need a user to set up a storage permission");
		}

		return false;
	}

}
