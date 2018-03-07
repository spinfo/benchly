package benchly.model;

import java.sql.Timestamp;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "server_contact")
public class ServerContact {

	// an enum and not a boolean because we might later add values like "do not
	// contact"
	public static enum Reachability {
		DEFAULT, REPORTED_REACHABLE
	};

	@DatabaseField(columnName = "id", generatedId = true)
	@Expose(deserialize = false)
	private long id;

	@DatabaseField(columnName ="reachability", canBeNull = false, index = true)
	@Expose(deserialize = false)
	private Reachability reachbility;

	// a unique name by which the server identifies itself
	@DatabaseField(columnName = "name", canBeNull = false, unique = true, width=1024)
	@Expose
	private String name;

	// the url by which the server is currently contacted
	@DatabaseField(columnName = "endpoint", canBeNull = false, dataType = DataType.LONG_STRING)
	@Expose
	private String endpoint;
	
	@DatabaseField(columnName = "approximateUsableMemory", canBeNull = true, index = true)
	@Expose(deserialize = false)
	private long approximateUsableMemory;

	@DatabaseField(columnName = "approximateRunningJobs", canBeNull = true, index = true)
	@Expose(deserialize = false)
	private int approximateRunningJobs;
	
	@DatabaseField(columnName = "lastChecked", canBeNull = true, index = true)
	@Expose(deserialize = false)
	private Timestamp lastChecked;

	protected ServerContact() {
		this.reachbility = Reachability.DEFAULT;
	}

	public ServerContact(String name, String endpoint) {
		this();
		this.name = name;
		this.endpoint = endpoint;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Reachability getReachbility() {
		return reachbility;
	}

	public void setReachbility(Reachability reachbility) {
		this.reachbility = reachbility;
	}

	public long getApproximateUsableMemory() {
		return approximateUsableMemory;
	}

	public int getApproximateRunningJobs() {
		return approximateRunningJobs;
	}

}
