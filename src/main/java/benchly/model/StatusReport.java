package benchly.model;

import java.sql.Timestamp;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "status_report")
public class StatusReport {

	@DatabaseField(columnName = "id", generatedId = true)
	long id;

	@DatabaseField(columnName = "serverContact", foreign = true, foreignAutoRefresh = false)
	private ServerContact contact;

	@DatabaseField(columnName = "name")
	@Expose
	private String name;

	@DatabaseField(columnName = "maxMemory")
	@Expose
	private long maxMemory;

	@DatabaseField(columnName = "totalMemory")
	@Expose
	private long totalMemory;

	@DatabaseField(columnName = "freeMemory")
	@Expose
	private long freeMemory;

	@DatabaseField(columnName = "usedMemory")
	@Expose
	private long usedMemory;

	@DatabaseField(columnName = "usableMemory")
	@Expose
	private long usableMemory;

	@DatabaseField(columnName = "longTermUsableMemory")
	@Expose
	private long longTermUsableMemory;

	@DatabaseField(columnName = "runningJobs")
	@Expose
	private int runningJobs;

	@DatabaseField(columnName = "collectedAt")
	@Expose
	private Timestamp collectedAt;

	public StatusReport() {
		// empty constructor for ormlite
	}

	public long getId() {
		return id;
	}

	public ServerContact getContact() {
		return contact;
	}

	public void setContact(ServerContact contact) {
		this.contact = contact;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getMaxMemory() {
		return maxMemory;
	}

	public void setMaxMemory(long maxMemory) {
		this.maxMemory = maxMemory;
	}

	public long getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(long totalMemory) {
		this.totalMemory = totalMemory;
	}

	public long getFreeMemory() {
		return freeMemory;
	}

	public void setFreeMemory(long freeMemory) {
		this.freeMemory = freeMemory;
	}

	public long getUsedMemory() {
		return usedMemory;
	}

	public void setUsedMemory(long usedMemory) {
		this.usedMemory = usedMemory;
	}

	public long getUsableMemory() {
		return usableMemory;
	}

	public void setUsableMemory(long usableMemory) {
		this.usableMemory = usableMemory;
	}

	public long getLongTermUsableMemory() {
		return longTermUsableMemory;
	}

	public void setLongTermUsableMemory(long longTermUsableMemory) {
		this.longTermUsableMemory = longTermUsableMemory;
	}

	public int getRunningJobs() {
		return runningJobs;
	}

	public void setRunningJobs(int runningJobs) {
		this.runningJobs = runningJobs;
	}

	public Timestamp getCollectedAt() {
		return collectedAt;
	}

	public void setCollectedAt(Timestamp collectedAt) {
		this.collectedAt = collectedAt;
	}

}
