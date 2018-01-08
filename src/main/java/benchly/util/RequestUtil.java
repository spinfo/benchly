package benchly.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;

public class RequestUtil {

	private static final Logger LOG = LoggerFactory.getLogger(RequestUtil.class.getName());

	public static class PaginationParams {

		public final long limit;
		public final long offset;

		public PaginationParams(long limit, long offset) {
			this.limit = (limit < 0) ? 1L : limit;
			this.offset = (offset < 0) ? 0L : offset;
		}
	}

	public static long parseIdParam(Request request) {
		long result = longConvert(request.params(":id"), -1L);
		if (result < 0) {
			LOG.warn("Invalid id value in request: " + result);
		}
		return result;
	}
	
	public static String parseUuidParam(Request request) {
		String result = request.params(":uuid");
		if (result == null) {
			result = "";
		}
		return result;
	}

	public static long parseNumberedQueryParamOrDefault(Request request, String queryParam, long defaultLong) {
		return longConvert(request.queryParams(queryParam), defaultLong);
	}

	public static PaginationParams parsePaginationParams(Request request) {
		long limit = parseNumberedQueryParamOrDefault(request, "limit", 10L);
		long offset = parseNumberedQueryParamOrDefault(request, "offset", 0L);
		return new PaginationParams(limit, offset);
	}

	private static long longConvert(String value, long defaultLong) {
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return defaultLong;
		}
	}

}
