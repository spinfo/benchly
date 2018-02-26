package benchly.database;

import java.sql.SQLException;
import java.util.List;

import com.j256.ormlite.dao.Dao;

import benchly.model.Job;

public class JobDao {

	public static List<Job> getPendingJobs() throws SQLException {
		return dao().queryBuilder().orderBy("createdAt", false).where().eq("state", Job.State.PENDING).query();
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
