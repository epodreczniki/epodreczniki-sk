package pl.epodr.sk.web;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.QueueManager;
import pl.epodr.sk.task.common.ConversionReason;
import pl.epodr.sk.task.common.ReasonType;

@Controller
@RequestMapping("/")
@Slf4j
public class ControlController {

	private static final int DEFAULT_COLLECTION_VERSION = 1;

	private static final String ID_AND_VERSION_SEPARATOR = "-";

	private final Logger requestLogger = LoggerFactory.getLogger("requestLogger");

	@Autowired
	private QueueManager queueManager;

	@RequestMapping("/")
	public String home() {
		return "redirect:content/";
	}

	private void logRequest(HttpServletRequest request, Object parameter) {
		requestLogger
				.debug(String.format("%s: %s from %s", request.getRequestURI(), parameter, request.getRemoteHost()));
	}

	@RequestMapping(value = { "/womi/modified", "/womi/deleted" }, method = RequestMethod.POST)
	@ResponseBody
	public void processWomiModified(@RequestBody String body, HttpServletResponse response, HttpServletRequest request) {
		log.debug("request womi modified/deleted: " + body);
		String[] lines = getRequestsFromRequestBody(body);
		for (String line : lines) {
			if (line.trim().length() > 0) {
				long womiId;
				try {
					womiId = Long.parseLong(line.trim());
					logRequest(request, womiId);
					queueManager.handleWomiModified(womiId, new ConversionReason(ReasonType.ADMIN));
				} catch (NumberFormatException e) {
					log.error(e + " when parsing line: " + line);
				}
			}
		}
		response.setStatus(204);
	}

	@RequestMapping(value = { "/colxml/added", "/colxml/modified" }, method = RequestMethod.POST)
	@ResponseBody
	public void processCollection(@RequestBody String body, HttpServletResponse response, HttpServletRequest request) {
		log.debug("request - added/modified collection: " + body);
		String[] lines = getRequestsFromRequestBody(body);
		for (String line : lines) {
			line = line.trim();
			if (line.length() > 0) {
				logRequest(request, line);
				try {
					IdAndVersion collection = extractCollectionFromRequestLine(line);
					queueManager.handleCollectionModified(collection, new ConversionReason(ReasonType.ADMIN));
				} catch (IllegalArgumentException e) {
					log.error("error parsing line '" + line + "': " + e);
				}
			}
		}
		response.setStatus(204);
	}

	@RequestMapping(value = "/colxml/deleted", method = RequestMethod.POST)
	@ResponseBody
	public void deleteCollection(@RequestBody String body, HttpServletResponse response, HttpServletRequest request) {
		log.debug("request - deleted collection: " + body);
		String[] lines = getRequestsFromRequestBody(body);
		for (String line : lines) {
			line = line.trim();
			if (line.length() > 0) {
				logRequest(request, line);
				try {
					IdAndVersion collection = extractCollectionFromRequestLine(line);
					queueManager.handleCollectionDeleted(collection);
				} catch (IllegalArgumentException e) {
					log.error("error parsing line '" + line + "': " + e);
				}
			}
		}
		response.setStatus(204);
	}

	@RequestMapping(value = "/transformation/update", method = RequestMethod.POST)
	@ResponseBody
	public void updateTransformation(HttpServletRequest request, HttpServletResponse response) {
		log.debug("new request - update transformation");
		logRequest(request, "-");
		queueManager.handleTransformationUpdate();
		response.setStatus(204);
	}

	private String[] getRequestsFromRequestBody(String body) {
		return body.split("\n");
	}

	private IdAndVersion extractCollectionFromRequestLine(String line) throws IllegalArgumentException {
		if (line.contains(ID_AND_VERSION_SEPARATOR)) {
			String[] parts = line.split(Pattern.quote(ID_AND_VERSION_SEPARATOR));
			if (parts.length != 2) {
				throw new IllegalArgumentException("invalid format of line '" + line + "'");
			}
			if (!NumberUtils.isNumber(parts[1])) {
				throw new IllegalArgumentException("collection version must be a number, '" + parts[1] + "' given");
			}
			return new IdAndVersion(parts[0], Long.parseLong(parts[1]));
		} else {
			return new IdAndVersion(line, DEFAULT_COLLECTION_VERSION);
		}
	}

}
