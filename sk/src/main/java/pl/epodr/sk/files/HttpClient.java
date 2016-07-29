package pl.epodr.sk.files;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HttpClient {

	private static final String ORIGINAL_STATUS_HEADER = "X-EPO-ORIGINAL-STATUS";

	private static final String EDITION_ONLINE_HEADER = "X-EPO-EDITION";

	private static final String EDITION_ONLINE_HEADER_VALUE = "1";

	private static final int CONNECT_TIMEOUT = 30 * 1000;

	private static final int SOCKET_TIMEOUT = 30 * 1000;

	private CloseableHttpClient httpClient;

	@PostConstruct
	void initHttpClient() {
		try {
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT)
					.setSocketTimeout(SOCKET_TIMEOUT).build();
			PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
			httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setConnectionManager(cm)
					.build();
		} catch (Exception e) {
			log.error("FATAL ERROR on initialization of http client", e);
		}
	}

	public Downloadable download(String url) throws IOException {
		HttpGet get = new HttpGet(url);
		try (CloseableHttpResponse response = httpClient.execute(get)) {
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == 404) {
				throw new FileNotFoundException(url);
			} else if (statusLine.getStatusCode() >= 400) {
				Header originalStatus = response.getFirstHeader(ORIGINAL_STATUS_HEADER);
				if (originalStatus == null) {
					throw new IOException(String.format("%s %s for url: %s", statusLine.getStatusCode(),
							statusLine.getReasonPhrase(), url));
				} else {
					throw new IOException(String.format("%s %s (original: %s) for url: %s", statusLine.getStatusCode(),
							statusLine.getReasonPhrase(), originalStatus.getValue(), url));
				}
			}
			try (InputStream is = response.getEntity().getContent()) {
				Header ctHeader = response.getEntity().getContentType();
				String contentType = null;
				if (ctHeader != null) {
					contentType = ctHeader.getValue();
				}
				boolean inEditionOnline = isInEditionOnline(response);
				byte[] content = IOUtils.toByteArray(is);
				return new DownloadableStream(new ByteArrayInputStream(content), contentType, content.length,
						inEditionOnline);
			}
		} catch (SocketTimeoutException e) {
			throw new IOException("socket timeout for url: " + url, e);
		} catch (HttpHostConnectException e) {
			throw new IOException("host connection error for url: " + url, e);
		}
	}

	private boolean isInEditionOnline(CloseableHttpResponse response) {
		Header[] eoHeaders = response.getHeaders(EDITION_ONLINE_HEADER);
		if (eoHeaders.length == 0) {
			return false;
		}
		String eoHeaderValue = eoHeaders[0].getValue() != null ? eoHeaders[0].getValue().trim() : null;
		return EDITION_ONLINE_HEADER_VALUE.equals(eoHeaderValue);
	}

}

class HttpBan extends HttpRequestBase {

	public final static String METHOD_NAME = "BAN";

	/**
	 * @throws IllegalArgumentException if the uri is invalid.
	 */
	public HttpBan(final String uri) {
		super();
		setURI(URI.create(uri));
	}

	@Override
	public String getMethod() {
		return METHOD_NAME;
	}

}
