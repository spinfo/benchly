package benchly.util;

import benchly.model.Workflow;

public class Path {

	// A container class for routing on the web page
	// The getter methods are needed for use in velocity templates
	public static class Web {

		public static final String INDEX = "/";

		public static final String WORKFLOWS = "/workflows";
		public static final String SHOW_WORKFLOW = "/workflows/:uuid";
		public static final String SHOW_WORKFLOW_VERSION = "/workflows/:uuid/version/:id";
		public static final String NEW_WORKLFOW = "/workflows/new";
		public static final String EDIT_WORKFLOW = "/workflow/:uuid/edit";
		public static final String UPDATE_WORKFLOW = "/workflow/:uuid/update";
		public static final String DELETE_WORKFLOW = "/workflows/:uuid/delete";
		public static final String DESTROY_WORKFLOW = "/workflows/:uuid/destroy";

		public static final String LOGIN = "/login";
		public static final String LOGOUT = "/logout";

		public static String getWorkflows() {
			return WORKFLOWS;
		}

		public static String getShowWorkflow(Workflow workflow) {
			return insertUuid(SHOW_WORKFLOW, workflow.getVersionId());
		}

		public static String getShowWorkflowVersion(Workflow workflow) {
			return insertId(insertUuid(SHOW_WORKFLOW_VERSION, workflow.getVersionId()), workflow.getId());
		}

		public static String getNewWorkflow() {
			return NEW_WORKLFOW;
		}

		public static String getEditWorkflow(Workflow workflow) {
			return insertUuid(EDIT_WORKFLOW, workflow.getVersionId());
		}
		
		public static String getUpdateWorkflow(Workflow workflow) {
			return insertUuid(UPDATE_WORKFLOW, workflow.getVersionId());
		}

		public static String getDeleteWorkflow(Workflow workflow) {
			return insertUuid(DELETE_WORKFLOW, workflow.getVersionId());
		}

		public static String getDestroyWorkflow(Workflow workflow) {
			return insertUuid(DESTROY_WORKFLOW, workflow.getVersionId());
		}

		public static String getLogin() {
			return LOGIN;
		}

		public static String getLogout() {
			return LOGOUT;
		}

		private static String insertId(String path, long id) {
			return path.replaceFirst(":id", Long.toString(id));
		}

		private static String insertUuid(String path, String uuid) {
			return path.replaceFirst(":uuid", uuid);
		}
	}

	public class Template {

		public static final String WORKFLOWS = "/templates/workflows/index.vm";
		public static final String NEW_WORKFLOW = "/templates/workflows/new.vm";
		public static final String SHOW_WORKFLOW = "/templates/workflows/show.vm";
		public static final String SHOW_WORKFLOW_VERSION = "/templates/workflows/show_version.vm";
		public static final String EDIT_WORKFLOW = "templates/workflows/edit.vm";
		public static final String DELETE_WORKFLOW = "/templates/workflows/delete.vm";

		public static final String STATUS_403 = "/templates/status_403.vm";
		public static final String STATUS_404 = "/templates/status_404.vm";
		public static final String STATUS_500 = "/templates/status_500.vm";

		public static final String LOGIN = "/templates/user/login.vm";

	}

}
