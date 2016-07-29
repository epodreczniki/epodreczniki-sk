package pl.epodr.sk.web;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

public class RtHealthChecker {

	private static final int TIMEOUT_STATUS_CODE = -1;

	private static final int CONNECT_TIMEOUT = 10;

	private static final int READ_TIMEOUT = 20;

	public static void checkUrl(String url) throws HealthCheckException, IOException {
		int statusCode = readResponseCodeForHealthCheckUrl(url);
		if (statusCode != 200) {
			if (statusCode != TIMEOUT_STATUS_CODE) {
				throw new HealthCheckException(statusCode, url);
			} else {
				throw new HealthCheckTimeoutException(url);
			}
		}
	}

	private static int readResponseCodeForHealthCheckUrl(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("HEAD");
		conn.setConnectTimeout(CONNECT_TIMEOUT * 1000);
		conn.setReadTimeout(READ_TIMEOUT * 1000);
		try {
			return conn.getResponseCode();
		} catch (SocketTimeoutException e) {
			return TIMEOUT_STATUS_CODE;
		}
	}

	private RtHealthChecker() {
	}

	public static class HealthCheckException extends Exception {

		public HealthCheckException(int statusCode, String url) {
			super("RT or Varnish returned HTTP status " + statusCode + " for " + url);
		}

		protected HealthCheckException(String message) {
			super(message);
		}

	}

	public static class HealthCheckTimeoutException extends HealthCheckException {

		public HealthCheckTimeoutException(String url) {
			super("RT or Varnish timed out for " + url);
		}

	}
}
