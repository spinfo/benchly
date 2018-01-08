package benchly.error;

import static spark.Spark.internalServerError;
import static spark.Spark.notFound;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.util.SessionUtil;
import benchly.util.Views;
import spark.ExceptionHandler;

public class Handlers {
	
	private static final Logger LOG = LoggerFactory.getLogger(Handlers.class);
	
	// a resource not found error is shown to the user with a warning
	public static ExceptionHandler<ResourceNotFoundError> resourceNotFound = (e, request, response) -> {
		SessionUtil.addWarningMessage(request, e.getMessage());
		notFound(Views.notFound);
	};
	
	// the handler for internal errors will output the exception's message to the user
	// and generally log the message
	public static ExceptionHandler<InternalServerError> internalError = (e, request, response) -> {
		LOG.error(e.getMessage());
		SessionUtil.addErrorMessage(request, e.getMessage());
		internalServerError(Views.errorPage);
	};
	
	// an SQL exception is logged quietly, a vague general message is shown to the user
	public static ExceptionHandler<SQLException> sqlException = (e, request, response) -> {
		LOG.error(e.getMessage());
		e.printStackTrace();
		internalError.handle(new InternalServerError("Unexpected error during database access.", e), request, response);
	};
	
	// on an invalid model, the validation messages are shown to the user 
	public static ExceptionHandler<InvalidModelException> invalidModel = (e, request, response) -> {
		for (String message : e.getModel().getErrorMessages()) {
			SessionUtil.addErrorMessage(request, message);
		}
		response.redirect(SessionUtil.getLastLocation(request));
	};
}
