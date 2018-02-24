package benchly.model;

import java.sql.Timestamp;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "workflow")
public class Workflow extends Model {

	// The primary id of this workflow in the database
	@DatabaseField(columnName = "id", generatedId = true)
	@Expose
	private long id;

	// All versions of a workflow refer to the same versionId, a UUID string
	@DatabaseField(columnName = "versionId", canBeNull = false, index = true, width = 36)
	@Expose
	private String versionId;
	
	@DatabaseField(columnName = "name")
	@Expose
	private String name;

	@DatabaseField(columnName = "definition")
	@Expose
	private String definition;

	@DatabaseField(columnName = "author", canBeNull = false, foreign = true, foreignAutoRefresh = true)
	@Expose
	private User author;

	@DatabaseField(columnName = "latestVersion", canBeNull = false)
	@Expose
	private Boolean latestVersion;
	
	@DatabaseField(columnName = "createdAt", canBeNull = false)
	@Expose
	private Timestamp createdAt;

	public Workflow() {
		this.versionId = UUID.randomUUID().toString();
		this.latestVersion = true;
		this.createdAt = new Timestamp(System.currentTimeMillis());
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

	public boolean isLatestVersion() {
		return latestVersion;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}
	
	public String getCreatedAtReadable() {
		return createdAt.toLocalDateTime().toString();
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
