package benchly.util;

import java.util.ArrayList;
import java.util.List;

import benchly.util.RequestUtil.PaginationParams;
import io.mikael.urlbuilder.UrlBuilder;
import spark.Request;

public class PaginationLinksBuilder {

	private final int maxPage;
	private final int currentPage;
	private final List<String> linkTargets;

	public PaginationLinksBuilder(Request request, PaginationParams pagination, long entitiesAmount) {

		// calculate the highest page number
		maxPage = pageFor(entitiesAmount, pagination.limit);

		// calculate the current page
		currentPage = pageFor((pagination.offset + 1), pagination.limit);

		// build links for each page
		UrlBuilder builder = UrlBuilder.fromString("").withPath(request.pathInfo()).withQuery(request.queryString());
		linkTargets = new ArrayList<>(maxPage);
		Long currentOffset = 0L;
		Long limit = pagination.limit;
		for (int i = 1; i <= maxPage; i++) {
			String linkTarget = builder.setParameter("limit", limit.toString())
					.setParameter("offset", currentOffset.toString()).toString();
			linkTargets.add(linkTarget);
			currentOffset += pagination.limit;
		}
	}

	/**
	 * Return a link to the paginated path with offset and limit parameters set
	 * according to the pageNo.
	 * 
	 * @param pageNo
	 *            The (1-indexed) result page the link should point to.
	 * @return A url to the paginated path. Returns an empty string on an invalid
	 *         pageNo.
	 */
	public String getLinkTarget(int pageNo) {
		if (pageNo > 0 && pageNo <= maxPage) {
			return linkTargets.get(pageNo - 1);
		}
		return "";
	}

	/**
	 * @return The highest page number this has a link for.
	 */
	public int getMaxPage() {
		return maxPage;
	}

	/**
	 * @return The current page number according to the query parameters.
	 */
	public int getCurrentPage() {
		return currentPage;
	}

	// calculate on which page the n-th entity would appear
	private static int pageFor(long n, long limit) {
		return (int) Math.ceil((double) n / (double) limit);
	}

}
