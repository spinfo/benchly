package benchly.controller;

import java.sql.SQLException;
import java.util.List;

import benchly.BenchlyScheduler;
import benchly.database.ServerContactDao;
import benchly.error.ResourceNotFoundError;
import benchly.error.ServerAccessError;
import benchly.model.ServerContact;
import benchly.model.StatusReport;
import benchly.remote.ServerNameCheckTask;
import benchly.remote.ServerStatusReportTask;
import benchly.util.RequestUtil;
import benchly.util.RequestUtil.PaginationParams;
import spark.Request;
import spark.Route;

public class ServerContactController extends Controller {

	public static Route index = (request, response) -> {
		ensureAdminUser(request, "Only admin users may view server contacts.");

		PaginationParams pagination = RequestUtil.parsePaginationParams(request);
		List<ServerContact> contacts = ServerContactDao.fetchAll(pagination);
		long max = ServerContactDao.count();

		return JsonTransformer.renderPaginatedResult(contacts, request, pagination.limit, pagination.offset, max);
	};

	public static Route indexReports = (request, response) -> {
		ensureAdminUser(request, "Only admin users may view server contact reports.");
		ServerContact contact = ensureSingleContactByIdFromRoute(request);

		PaginationParams pagination = RequestUtil.parsePaginationParams(request);
		List<StatusReport> reports = ServerContactDao.fetchReports(pagination, contact);
		long max = ServerContactDao.countReports(contact);

		return JsonTransformer.renderPaginatedResult(reports, request, pagination.limit, pagination.offset, max);
	};

	public static Route create = (request, response) -> {
		ensureAdminUser(request, "Only admin users may create server contacts.");

		ServerContact contact = JsonTransformer.readRequestBody(request.body(), ServerContact.class);

		// do a synchronous status report check returning the name
		String name = checkServerName(contact);

		contact.setName(name);
		ServerContactDao.create(contact);

		// schedule a deferred status check that will actually be persisted now
		BenchlyScheduler.get().submit(new ServerStatusReportTask(contact));

		return JsonTransformer.render(contact, request);
	};

	public static Route show = (request, response) -> {
		ensureAdminUser(request, "Only admin users may view server contacts.");

		ServerContact contact = ensureSingleContactByIdFromRoute(request);

		return JsonTransformer.render(contact, request);
	};

	public static Route update = (request, response) -> {
		ensureAdminUser(request, "Only admin users may update server contacts.");

		// check that the new contact is reachable returns the same name
		ServerContact newContact = JsonTransformer.readRequestBody(request.body(), ServerContact.class);
		String name = checkServerName(newContact);

		ServerContact oldContact = ensureSingleContactByIdFromRoute(request);
		if (oldContact.getName().equals(name)) {
			// the endpoint is the only thing that can be changed
			oldContact.setEndpoint(newContact.getEndpoint());
			ServerContactDao.update(oldContact);
		} else {
			throw new ServerAccessError("The server reports a different name: '" + name + "'");
		}

		return JsonTransformer.render(oldContact, request);
	};

	private static ServerContact ensureSingleContactByIdFromRoute(Request request)
			throws ResourceNotFoundError, SQLException {
		long id = RequestUtil.parseIdParam(request);
		ServerContact contact = ServerContactDao.fetchById(id);

		if (contact == null) {
			throw new ResourceNotFoundError("No server contact for id: '" + id + "'");
		}
		return contact;
	}

	private static String checkServerName(ServerContact contact) throws ServerAccessError {
		try {
			return BenchlyScheduler.get().submit(new ServerNameCheckTask(contact)).get();
		} catch (Exception e) {
			// TODO: This should differentiate by exceptions
			throw new ServerAccessError(e.getCause().getMessage());
		}
	}

}
