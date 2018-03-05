package benchly.database;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.Dao;

import benchly.model.AdminMessage;

public class AdminMessageDao {

	private static final Logger LOG = LoggerFactory.getLogger(AdminMessageDao.class);

	public static AdminMessage fetchById(long id) throws SQLException {
		return dao().queryForId(id);
	}
	
	public static List<AdminMessage> fetch(long limit, long offset) throws SQLException {
		return dao().queryBuilder().limit(limit).offset(offset).query();
	}

	public static int createOrLogOnFailure(AdminMessage message) {
		try {
			return dao().create(message);
		} catch (SQLException e) {
			// in case we are unable to save the message, the content should get logged
			// alongside the sql error
			LOG.error(e.getMessage());
			e.printStackTrace();
			LOG.error("Unable to save message to admin_message table. Content was: " + message.getContent());
			return 0;
		}
	}
	
	public static int delete(AdminMessage message) throws SQLException {
		return dao().delete(message);
	}

	private static Dao<AdminMessage, Long> dao() {
		return DatabaseHelper.getInstance().getAdminMessageDao();
	}

}
