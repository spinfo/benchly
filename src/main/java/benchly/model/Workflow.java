package benchly.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "workflow")
public class Workflow extends Model {

	// The primary id of this workflow in the database
	@DatabaseField(columnName = "id", generatedId = true)
	@Expose(deserialize = false)
	private long id;

	// All versions of a workflow refer to the same versionId, a UUID string
	// TODO: Rename to 'versionOf'
	@DatabaseField(columnName = "versionId", canBeNull = false, index = true, width = 36)
	@Expose
	private String versionId;

	@DatabaseField(columnName = "name", dataType = DataType.LONG_STRING)
	@Expose
	private String name;

	@DatabaseField(columnName = "definition", dataType = DataType.LONG_STRING)
	@Expose
	private String definition;

	@DatabaseField(columnName = "author", canBeNull = false, foreign = true, foreignAutoRefresh = true)
	@Expose
	private User author;

	@DatabaseField(columnName = "isDeleted", canBeNull = false, index = true)
	@Expose(deserialize = false)
	private boolean isDeleted = false;

	@DatabaseField(columnName = "latestVersion", canBeNull = false, index = true)
	@Expose
	private Boolean latestVersion;

	@DatabaseField(columnName = "createdAt", canBeNull = false, index = true)
	@Expose
	private Timestamp createdAt;

	public Workflow() {
		this.generateNewVersionId();
		this.latestVersion = true;
		this.createdAt = Timestamp.from(Instant.now());
	}

	// create a new workflow not referring to an existing version
	public Workflow(String name, String definition, User author) {
		this();
		this.name = name;
		this.definition = definition;
		this.author = author;
	}

	// create a new workflow, that is a new version of an existing workflow
	public Workflow(Workflow previousVersion) {
		this(previousVersion.name, previousVersion.definition, previousVersion.author);

		// immediately overwrite the versionId from the default constructor
		this.versionId = previousVersion.versionId;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDefinition() {
		return definition;
	}

	public User getAuthor() {
		return author;
	}

	public void setAuthor(User author) {
		this.author = author;
	}

	public String getVersionId() {
		return versionId;
	}

	// instead of a setter, the version Id is never set directly
	public String generateNewVersionId() {
		this.versionId = UUID.randomUUID().toString();
		return this.versionId;
	}

	public boolean isLatestVersion() {
		return latestVersion;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean validate() {
		valid = true;

		if (StringUtils.isBlank(definition)) {
			addError("Definition cannot be empty");
		}
		if (author == null) {
			addError("Author must not be empty");
		}
		return isValid();
	}
}
