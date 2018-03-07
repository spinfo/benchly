package benchly.controller;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.error.InternalServerError;
import benchly.error.InvalidModelException;
import benchly.error.InvalidRequestException;
import benchly.error.ResourceNotFoundError;
import benchly.util.SessionUtil;
import spark.ExceptionHandler;

public class ErrorHandlers {

	private static final Logger LOG = LoggerFactory.getLogger(ErrorHandlers.class);

	// a resource not found error is shown to the user with a warning
	public static ExceptionHandler<ResourceNotFoundError> resourceNotFound = (e, request, response) -> {
		SessionUtil.addWarningMessage(request, e.getMessage());
		response.body(JsonTransformer.renderWithoutContent(request));
		response.status(404);
	};

	// the handler for internal errors will output the exception's message to the
	// user and generally log the message
	public static ExceptionHandler<Exception> internalError = (e, request, response) -> {
		String msg = e.getMessage();
		if (e.getCause() != null) {
			msg += " CAUSED BY: ";
			msg += e.getCause().getMessage();
		}
		LOG.error("Internal error: " + msg);
		e.printStackTrace();
		SessionUtil.addErrorMessage(request, msg);

		response.body(JsonTransformer.renderWithoutContent(request));
		response.status(500);
	};

	// an SQL exception is logged quietly, a vague general message is shown to the
	// user
	public static ExceptionHandler<SQLException> sqlException = (e, request, response) -> {
		LOG.error("SQL error with state: " + e.getSQLState());
		internalError.handle(new InternalServerError("Unexpected error during database access.", e), request, response);
	};

	// on an invalid request, log a warning and return an error code
	public static ExceptionHandler<InvalidRequestException> invalidRequest = (e, request, response) -> {
		LOG.warn(e.getMessage());
		e.printStackTrace();
		SessionUtil.addErrorMessage(request, e.getMessage());
		response.status(400);
		response.body(JsonTransformer.renderWithoutContent(request));
	};

	// on an invalid model, the validation messages are shown to the user
	public static ExceptionHandler<InvalidModelException> invalidModel = (e, request, response) -> {
		for (String message : e.getModel().getErrorMessages()) {
			LOG.debug("Invalid model (" + e.getModel().getClass() + "): " + message);
			SessionUtil.addErrorMessage(request, message);
		}
		invalidRequest.handle(e, request, response);
	};

}
