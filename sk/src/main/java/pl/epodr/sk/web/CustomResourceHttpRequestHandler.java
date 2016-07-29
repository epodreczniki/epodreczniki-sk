package pl.epodr.sk.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

public class CustomResourceHttpRequestHandler extends ResourceHttpRequestHandler {

	private static final Logger accessLogger = LoggerFactory.getLogger("accessLogger");

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		accessLogger.debug(String.format("[%s] %s %s", request.getRemoteAddr(), request.getMethod(),
				request.getRequestURI()));
		super.handleRequest(request, response);
	}

	@Override
	protected void setHeaders(HttpServletResponse response, Resource resource, MediaType mediaType) throws IOException {
		long length = resource.contentLength();
		if (length <= Integer.MAX_VALUE) {
			response.setContentLength((int) length);
		} else {
			response.addHeader("Content-Length", Long.toString(length));
		}

		if (mediaType != null) {
			response.setContentType(mediaType.toString());
		}
	}

}
