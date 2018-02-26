package benchly.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;

import org.jclouds.blobstore.domain.StorageMetadata;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * A class to store file information retrieved from Storage providers.
 */
@DatabaseTable(tableName = "storage_file_meta")
public class StorageFileMeta extends Model {

	@DatabaseField(columnName = "id", generatedId = true)
	@Expose(deserialize = false)
	private long id;

	@DatabaseField(columnName = "storageConfig", canBeNull = false, foreign = true, foreignAutoRefresh = false, index = true)
	@Expose(deserialize = false)
	private StorageConfig storageConfig;

	// a file name
	@DatabaseField
	@Expose(deserialize = false)
	private String name;

	// the size in bytes
	@DatabaseField
	@Expose(deserialize = false)
	private long size;

	// when the file in question was last modified
	@DatabaseField
	@Expose(deserialize = false)
	private Date lastModified;

	// when this record was retrieved
	@DatabaseField(columnName = "retrievedAt", canBeNull = false, index = true)
	@Expose(deserialize = false)
	private Timestamp retrievedAt;

	public StorageFileMeta() {
		this.retrievedAt = Timestamp.from(Instant.now());
	}

	public static StorageFileMeta from(StorageConfig config, StorageMetadata storageMeta) {
		StorageFileMeta result = new StorageFileMeta();

		result.storageConfig = config;
		result.name = storageMeta.getName();
		result.size = storageMeta.getSize();
		result.lastModified = storageMeta.getLastModified();

		return result;
	}

	public StorageFileMeta(StorageConfig config) {
		this();
		this.storageConfig = config;
	}
	
	@Override
	public boolean validate() {
		valid = true;

		if (this.storageConfig == null) {
			addError("No storage config set on file info.");
		}

		return isValid();
	}

	public StorageConfig getStorageConfig() {
		return storageConfig;
	}

	public void setStorageConfig(StorageConfig storageConfig) {
		this.storageConfig = storageConfig;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public Timestamp getRetrievedAt() {
		return retrievedAt;
	}

	public void setRetrievedAt(Timestamp retrievedAt) {
		this.retrievedAt = retrievedAt;
	}

	public long getId() {
		return id;
	}

}
