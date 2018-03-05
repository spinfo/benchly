package benchly.database;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import benchly.model.Job;

public class JobDao {

	public static List<Job> fetchPendingJobs() throws SQLException {
		return dao().queryBuilder().orderBy("createdAt", false).where().eq("state", Job.State.PENDING).query();
	}

	public static List<Job> fetchSubmittedJobs() throws SQLException {
		return dao().queryBuilder().orderBy("estimatedTime", true).where().eq("state", Job.State.SUBMITTED).query();
	}

	public static Job pickSubmittedJobWhereLastCheckIsLongerAgoThan(long seconds) throws SQLException {
		Timestamp threshold = Timestamp.from(Instant.now().minusSeconds(seconds));

		QueryBuilder<Job, Long> builder = dao().queryBuilder();
		builder.orderBy("lastChecked", false);
		builder.where().isNull("lastChecked").or().le("lastChecked", threshold).and().eq("state", Job.State.SUBMITTED);
		return builder.queryForFirst();
	}

	public static Job fetchById(long jobId) throws SQLException {
		return dao().queryForId(jobId);
	}

	public static int create(Job job) throws SQLException {
		return dao().create(job);
	}

	public static int update(Job job) throws SQLException {
		return dao().update(job);
	}

	private static Dao<Job, Long> dao() {
		return DatabaseHelper.getInstance().getJobDao();
	}

}
