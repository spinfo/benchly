package benchly.controller;

import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;

import benchly.error.InvalidRequestException;
import benchly.util.SessionUtil;
import spark.Request;

class JsonTransformer {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation()
			.create();

	private static class PaginationInfo {

		@Expose
		long limit, offset, max;

		private PaginationInfo(long limit, long offset, long max) {
			this.limit = limit;
			this.offset = offset;
			this.max = max;
		}

	}

	protected static String render(Object model, Request request) {
		return prepareWithDefaultFields(model, request).toString();
	}
	
	// renders only the information contained in every response without a main model 
	protected static String renderWithoutContent(Request request) {
		return prepareWithDefaultFields("", request).toString();
	}

	protected static String renderPaginatedResult(Collection<? extends Object> objects, Request request, long limit,
			long offset, long max) {
		JsonObject result = prepareWithDefaultFields(objects, request);

		JsonElement pagination = GSON.toJsonTree(new PaginationInfo(limit, offset, max));
		result.add("pagination", pagination);

		return result.toString();
	}

	// read a json object but wrap any json syntax exceptions
	protected static <T> T readRequestBody(String input, Class<T> classOfT) throws InvalidRequestException {
		try {
			return GSON.fromJson(input, classOfT);
		} catch (JsonSyntaxException e) {
			throw new InvalidRequestException(e);
		}
	}

	private static JsonObject prepareWithDefaultFields(Object model, Request request) {
		JsonElement content = GSON.toJsonTree(model);

		JsonObject result = prepareDefaultFields(request);
		result.add("content", content);

		return result;
	}

	private static JsonObject prepareDefaultFields(Request request) {
		// TODO: Is it really necessary to include the user in every response?
		JsonElement user = GSON.toJsonTree(SessionUtil.getCurrentUser(request));
		JsonElement messages = GSON.toJsonTree(SessionUtil.clearUserMessages(request));

		JsonObject root = new JsonObject();
		root.add("messages", messages);
		root.add("user", user);

		return root;
	}

}
