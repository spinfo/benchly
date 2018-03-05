package benchly.remote;

import com.google.gson.annotations.Expose;

class RemoteStatusReport {

	@Expose(serialize = false)
	String name;

	@Expose(serialize = false)
	int availableProcessors;

	@Expose(serialize = false)
	long maxMemory;

	@Expose(serialize = false)
	long totalMemory;

	@Expose(serialize = false)
	long freeMemory;

	@Expose(serialize = false)
	long usedMemory;

	@Expose(serialize = false)
	long usableMemory;

	@Expose(serialize = false)
	long longTermUsableMemory;

	@Expose(serialize = false)
	int runningJobs;

	@Expose(serialize = false)
	long collectedAt;

}
