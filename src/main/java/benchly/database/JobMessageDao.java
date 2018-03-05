package benchly.database;

import java.sql.SQLException;
import java.util.List;

import com.j256.ormlite.dao.Dao;

import benchly.model.Job;
import benchly.model.JobMessage;
import benchly.model.ServerContact;
import benchly.model.Workflow;

public class JobMessageDao {

	public static int create(JobMessage message) throws SQLException {
		return dao().create(message);
	}
	
	public static void createIfNotExist(JobMessage message) throws SQLException {
		dao().createIfNotExists(message);
	}

	public static List<JobMessage> fetch(Job job, long limit, long offset) throws SQLException {
		return dao().queryBuilder().limit(limit).offset(offset).where().eq("job", job.getId()).query();
	}

	public static List<JobMessage> fetch(Workflow workflow, long limit, long offset) throws SQLException {
		return dao().queryBuilder().limit(limit).offset(offset).where().eq("workflow", workflow).query();
	}

	public static List<JobMessage> fetch(ServerContact origin, long limit, long offset) throws SQLException {
		return dao().queryBuilder().limit(limit).offset(offset).where().eq("origin", origin).query();
	}

	public static int delete(JobMessage message) throws SQLException {
		return dao().delete(message);
	}

	private static Dao<JobMessage, String> dao() {
		return DatabaseHelper.getInstance().getJobMessageDao();
	}

}
