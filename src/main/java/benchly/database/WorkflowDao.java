package benchly.database;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;

import benchly.model.User;
import benchly.model.Workflow;
import benchly.util.RequestUtil.PaginationParams;

public class WorkflowDao {

	private static final Logger LOG = LoggerFactory.getLogger(WorkflowDao.class);

	public static long getCountOfLatestVersions() throws SQLException {
		return whereNotDeleted().and().eq("latestVersion", true).countOf();
	}

	public static Workflow fetchById(long workflowId) throws SQLException {
		return whereNotDeleted().and().idEq(workflowId).queryForFirst();
	}

	public static Workflow fetchLatestVersion(String versionId) throws SQLException {
		return whereNotDeleted().and().eq("latestVersion", true).and().eq("versionId", versionId).queryForFirst();
	}

	public static Workflow fetchSpecificVersion(long id, String versionId) throws SQLException {
		// querying for id only would suffice but we want to make sure that the version
		// in question exists
		return whereNotDeleted().and().eq("versionId", versionId).and().eq("id", id).queryForFirst();
	}

	public static List<Workflow> fetchLatestVersions(PaginationParams pagination) throws SQLException {
		QueryBuilder<Workflow, Long> builder = dao().queryBuilder();
		builder.setWhere(whereNotDeleted().and().eq("latestVersion", true));
		builder.limit(pagination.limit).offset(pagination.offset);
		return builder.query();
	}

	public static List<Workflow> fetchVersionsOf(Workflow workflow) throws SQLException {
		QueryBuilder<Workflow, Long> builder = dao().queryBuilder();
		builder.setWhere(whereNotDeleted().and().eq("versionId", workflow.getVersionId()));
		builder.orderBy("createdAt", false);
		return builder.query();
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
				LOG.debug("Updated " + rows + " other rows when inserting new workflow.");

				// insert the new workflow
				rows = dao.create(workflow);

				return rows;
			}
		});
		return insertedRows;
	}

	public static int setDeleted(Workflow workflow) throws SQLException {
		UpdateBuilder<Workflow, Long> builder = getSetDeletedBuilder();
		builder.where().idEq(workflow.getId());
		return builder.update();
	}

	public static int setDeletedWhereAuthorIs(User author) throws SQLException {
		UpdateBuilder<Workflow, Long> builder = getSetDeletedBuilder();
		builder.where().eq("author", author);
		return builder.update();
	}

	private static Where<Workflow, Long> whereNotDeleted() throws SQLException {
		return dao().queryBuilder().where().eq("isDeleted", false);
	}

	private static UpdateBuilder<Workflow, Long> getSetDeletedBuilder() throws SQLException {
		return dao().updateBuilder().updateColumnValue("isDeleted", true);
	}

	private static Dao<Workflow, Long> dao() {
		return DatabaseHelper.getInstance().getWorkflowDao();
	}
}
