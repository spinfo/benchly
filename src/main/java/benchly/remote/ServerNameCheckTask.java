package benchly.remote;

import java.util.concurrent.Callable;

import benchly.error.ServerAccessError;
import benchly.model.ServerContact;

public class ServerNameCheckTask implements Callable<String> {

	private final ServerContact contact;
	
	public ServerNameCheckTask(ServerContact contact) {
		this.contact = contact;
	}
	
	@Override
	public String call() throws ServerAccessError {
		return ServerAccess.fetchStatus(contact).getName();
	}

}
