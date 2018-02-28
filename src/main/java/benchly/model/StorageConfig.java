package benchly.model;

import java.sql.Timestamp;
import java.time.Instant;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "storage_config")
public class StorageConfig extends Model {

	// an enum to identify different provider types
	public static enum Provider {

		@SerializedName("aws-s3")
		S3("aws-s3"),

		@SerializedName("b2")
		B2("b2"),

		@SerializedName("openstack-swift")
		SWIFT("openstack-swift");

		private final String val;

		private Provider(String val) {
			this.val = val;
		}

		@Override
		public String toString() {
			return val;
		}
	}

	@DatabaseField(columnName = "id", generatedId = true)
	@Expose(deserialize = false)
	private long id;

	@DatabaseField(columnName = "provider", canBeNull = false)
	@Expose
	private Provider provider;

	// The user responsible for this configuration
	@DatabaseField(columnName = "owner", canBeNull = false, foreign = true, foreignAutoRefresh = true, index = true)
	private User owner;

	@DatabaseField(columnName = "endpoint")
	@Expose
	private String endpoint;

	// A string identifying the user with which to query the storage provider
	@DatabaseField(columnName = "identity", canBeNull = false)
	@Expose
	private String identity;

	// An API key or password used to log in to the database
	// TODO: Can we store this encrypted?
	@DatabaseField(columnName = "credential", canBeNull = false)
	@Expose(serialize = false)
	private String credential;

	// The encrypte credential that is actually shown to the user
	@Expose(deserialize = false)
	private StorageCredential encryptedCredential;

	// A bucket or container name to identify the type of resource to look for
	@DatabaseField(columnName = "container")
	@Expose
	private String container;

	// defines which users have access to this storage config (n to n)
	@ForeignCollectionField(foreignFieldName = "storageConfig", eager = true)
	@Expose(deserialize = false)
	private ForeignCollection<StoragePermission> accessPermissions;

	// locally stored information about the files accessible via this config
	@ForeignCollectionField(foreignFieldName = "storageConfig", eager = true)
	@Expose(deserialize = false)
	private ForeignCollection<StorageFileMeta> filesMeta;

	@DatabaseField(columnName = "createdAt", canBeNull = false, index = true)
	@Expose(deserialize = false)
	private Timestamp createdAt;

	@DatabaseField(columnName = "updatedAt", canBeNull = false, index = true)
	@Expose(deserialize = false)
	private Timestamp updatedAt;

	@DatabaseField(columnName = "refreshedAt", canBeNull = true, index = true)
	@Expose(deserialize = false)
	private Timestamp refreshedAt;

	public StorageConfig() {
		this.createdAt = Timestamp.from(Instant.now());
		;
	}

	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public String getCredential() {
		return credential;
	}

	public void setCredential(String credential) {
		this.credential = credential;
	}

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	public long getId() {
		return id;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp timestamp) {
		this.updatedAt = timestamp;
	}

	public void setUpdatedAtNow() {
		setUpdatedAt(Timestamp.from(Instant.now()));
	}

	public ForeignCollection<StoragePermission> getAccessPermissions() {
		return this.accessPermissions;
	}

	public ForeignCollection<StorageFileMeta> getFilesMeta() {
		return this.filesMeta;
	}

	public StorageCredential generateNewEncryptedCredential() {
		this.encryptedCredential = new StorageCredential(this.credential);
		return this.encryptedCredential;
	}

	public StorageCredential getEncryptedCredential() {
		if (this.encryptedCredential == null) {
			return this.generateNewEncryptedCredential();
		} else {
			return this.encryptedCredential;
		}
	}

	@Override
	public boolean validate() {
		valid = true;

		if (owner == null) {
			addError("Owner must not be empty.");
		}
		if (provider == null) {
			addError("No provider specified.");
		}
		if (provider == Provider.SWIFT && StringUtils.isBlank(endpoint)) {
			addError("An endpoint has to be specified for Openstack Swift access.");
		}
		if (StringUtils.isBlank(identity)) {
			addError("No user identity specified.");
		}
		if (StringUtils.isBlank(credential)) {
			addError("No user credential given.");
		}
		if (StringUtils.isBlank(container)) {
			addError("No container specified.");
		}
		return isValid();
	}

}
