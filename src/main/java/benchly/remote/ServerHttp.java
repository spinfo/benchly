package benchly.remote;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import benchly.error.ServerAccessError;

class ServerHttp {
	
	interface ResponseCallback {
		Object call(int returnCode, String responseBody) throws ServerAccessError;
	}

	private static final String CHARSET = "UTF-8";

	private static final Logger LOG = LoggerFactory.getLogger(ServerHttp.class);

	protected static Object get(URI uri, ResponseCallback callback) throws ServerAccessError {
		HttpGet get = new HttpGet(uri);
		return executeRequest(get, callback);
	}

	protected static Object post(URI uri, String body, ResponseCallback callback) throws ServerAccessError {
		HttpPost post = new HttpPost(uri);
		post.setEntity(buildStringEntity(body));
		return executeRequest(post, callback);
	}

	protected static Object put(URI uri, String body, ResponseCallback callback) throws ServerAccessError {
		HttpPut put = new HttpPut(uri);
		put.setEntity(buildStringEntity(body));
		return executeRequest(put, callback);
	}

	protected static Object delete(URI uri, ResponseCallback callback) throws ServerAccessError {
		HttpDelete delete = new HttpDelete(uri);
		return executeRequest(delete, callback);
	}

	private static StringEntity buildStringEntity(String content) {
		if (content == null) {
			content = "";
		}
		return new StringEntity(content, ContentType.create("application/json", "UTF-8"));
	}

	private static Object executeRequest(HttpUriRequest request, ResponseCallback callback) throws ServerAccessError {
		String requestInfo = String.format("%s %s", request.getMethod(), request.getURI());
		LOG.debug(">>>> " + request.getMethod() + " " + request.getURI());

		try (CloseableHttpClient client = HttpClients.createDefault();
				CloseableHttpResponse response = client.execute(request)) {
			int code = response.getStatusLine().getStatusCode();
			String reason = response.getStatusLine().getReasonPhrase();
			String responseInfo = String.format("<<<< '%d %s' on: %s", code, reason, requestInfo);
			LOG.debug(responseInfo);

			HttpEntity entity = response.getEntity();
			String result = "";
			if (entity != null) {
				result = EntityUtils.toString(entity, CHARSET);
			}

			return callback.call(code, result);
		} catch (IOException | ParseException e) {
			throw new ServerAccessError("Error on request '" + request.getURI() + "'", e);
		}
	}

}
