package benchly.util;

import org.apache.commons.lang.StringUtils;

import spark.Filter;
import spark.Request;
import spark.Response;

public class Filters {

	// If a user manually manipulates paths and forgets to add
	// a trailing slash, redirect the user to the correct path
	public static Filter removeTrailingSlashes = (Request request, Response response) -> {
		if (request.pathInfo().endsWith("/")) {
			String newPath = StringUtils.stripEnd(request.pathInfo(), "/");
			response.redirect(newPath);
		}
	};

	public static Filter markGetRequestLocationInSession = (request, response) -> {
		if ("GET".equals(request.requestMethod())) {
			SessionUtil.addLastLocation(request);
		}
	};

}
