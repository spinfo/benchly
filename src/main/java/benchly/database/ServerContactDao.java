package benchly.database;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;

import benchly.model.ServerContact;
import benchly.model.StatusReport;
import benchly.model.ServerContact.Reachability;

public class ServerContactDao {

	public static ServerContact fetchByName(String name) throws SQLException {
		return dao().queryBuilder().where().eq("name", name).queryForFirst();
	}

	public static ServerContact pickOneReachableWhereLastCheckIsLongerAgoThan(long seconds) throws SQLException {
		Timestamp threshold = Timestamp.from(Instant.now().minusSeconds(seconds));

		QueryBuilder<ServerContact, Long> builder = dao().queryBuilder();
		builder.where().isNull("lastChecked").or().le("lastChecked", threshold).and().eq("reachability",
				Reachability.DEFAULT);
		return builder.queryForFirst();
	}

	public static int create(ServerContact contact) throws SQLException {
		return dao().create(contact);
	}

	public static int update(ServerContact contact) throws SQLException {
		return dao().update(contact);
	}

	public static int delete(ServerContact contact) throws SQLException {
		return dao().delete(contact);
	}

	public static List<ServerContact> fetchReachable() throws SQLException {
		return dao().queryBuilder().where().eq("reachability", Reachability.DEFAULT).query();
	}

	public static int createReport(ServerContact contact, StatusReport report) throws SQLException {
		if (report.getContact() == null || contact.getId() != report.getContact().getId()) {
			throw new SQLException("Invalid contact/report pair for status report createion.");
		}

		UpdateBuilder<ServerContact, Long> updateBuider = dao().updateBuilder();
		updateBuider.updateColumnValue("approximateUsableMemory", report.getLongTermUsableMemory());
		updateBuider.updateColumnValue("approximateRunningJobs", report.getRunningJobs());
		updateBuider.updateColumnValue("lastChecked", Timestamp.from(Instant.now()));
		updateBuider.where().eq("id", contact.getId());

		return TransactionManager.callInTransaction(dao().getConnectionSource(), new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				updateBuider.update();
				return reportDao().create(report);
			}
		});
	}

	public static int deleteReportsOlderThan(long seconds) throws SQLException {
		Timestamp threshold = Timestamp.from(Instant.now().minusSeconds(seconds));

		DeleteBuilder<StatusReport, Long> builder = reportDao().deleteBuilder();
		builder.where().le("collectedAt", threshold);

		return builder.delete();
	}

	/**
	 * Fetches list of server contacts, that presumably meet the required memory
	 * demand. Tries to order suitable candidates first.
	 * 
	 * TODO: Return an Iterator?
	 * 
	 * @return An iterator over ServerContact objects
	 * @throws SQLException
	 */
	public static CloseableIterator<ServerContact> fetchJobSubmittalCandidates(long memoryDemand) throws SQLException {
		QueryBuilder<ServerContact, Long> builder = dao().queryBuilder();

		// sort those with few jobs and best memory first, return only those that meet the memory demand
		builder.orderBy("approximateRunningJobs", true);
		builder.orderBy("approximateUsableMemory", false);
		builder.where().eq("reachability", Reachability.DEFAULT).and().ge("approximateUsableMemory", memoryDemand);

		return builder.iterator();
	}

	private static Dao<ServerContact, Long> dao() {
		return DatabaseHelper.getInstance().getServerContactDao();
	}

	private static Dao<StatusReport, Long> reportDao() {
		return DatabaseHelper.getInstance().getStatusReportDao();
	}

}
