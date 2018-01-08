package benchly.database;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;

import benchly.model.Workflow;
import benchly.util.RequestUtil.PaginationParams;

public class WorkflowDao {

	private static final Logger LOG = LoggerFactory.getLogger(WorkflowDao.class);

	public static long getCountOfLatestVersions() throws SQLException {
		return dao().queryBuilder().where().eq("latestVersion", true).countOf();
	}

	public static Workflow fetchById(long id) {
		return queryForFirstWhereEq("id", id);
	}

	public static Workflow fetchLatestVersion(String versionId) throws SQLException {
		return dao().queryBuilder().where().eq("latestVersion", true).and().eq("versionId", versionId).queryForFirst();
	}
	
	public static Workflow fetchSpecificVersion(long id, String versionId) throws SQLException {
		// querying for id only would suffice but we want to make sure that the version in question exists
		return dao().queryBuilder().where().eq("versionId", versionId).and().eq("id", id).queryForFirst();
	}

	public static List<Workflow> fetchLatestVersions(PaginationParams pagination) throws SQLException {
		return dao().queryBuilder().limit(pagination.limit).offset(pagination.offset).where().eq("latestVersion", true)
				.query();
	}

	public static List<Workflow> fetchVersionsOf(Workflow workflow) throws SQLException {
		return dao().queryBuilder().orderBy("createdAt", false).where().eq("versionId", workflow.getVersionId()).query();
	}

	public static int save(final Workflow workflow) throws SQLException {
		if (!workflow.isLatestVersion()) {
			throw new SQLException("Cannot persist workflow that is not the latest version.");
		}

		final Dao<Workflow, Long> dao = dao();
		int insertedRows = TransactionManager.callInTransaction(dao().getConnectionSource(), new Callable<Integer>() {
			public Integer call() throws SQLException {
				// update all existing versions of this workflow to not be the latest
				UpdateBuilder<Workflow, Long> updateBuilder = dao.updateBuilder();
				updateBuilder.updateColumnValue("latestVersion", false).where().eq("versionId",
						workflow.getVersionId());
				int rows = updateBuilder.update();
				LOG.debug("Updated " + rows + " rows when inserting new workflow.");

				// insert the new workflow
				rows = dao.create(workflow);
				LOG.debug("Inserted " + rows + " new workflow(s).");

				return rows;
			}
		});
		return insertedRows;
	}

	public static int destroy(Workflow workflow) throws SQLException {
		DeleteBuilder<Workflow, Long> delete = dao().deleteBuilder();
		delete.where().eq("versionId", workflow.getVersionId());
		return delete.delete();
	}

	private static Workflow queryForFirstWhereEq(String attribute, Object value) {
		try {
			return dao().queryForFirst(dao().queryBuilder().where().eq(attribute, value).prepare());
		} catch (SQLException e) {
			e.printStackTrace();
			LOG.error(e.getMessage());
			return null;
		}
	}

	private static Dao<Workflow, Long> dao() {
		return DatabaseHelper.getInstance().getWorkflowDao();
	}
}
