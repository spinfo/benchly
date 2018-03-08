package benchly;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * A ScheduledExecutorService accessible as a singleton.
 */
public class BenchlyScheduler extends ScheduledThreadPoolExecutor implements ScheduledExecutorService {

	private static BenchlyScheduler instance = null;

	private BenchlyScheduler(int corePoolSize) {
		super(corePoolSize);
	}

	public static BenchlyScheduler get() {
		if (instance == null) {
			instance = new BenchlyScheduler(50);
		}
		return instance;
	}

}
