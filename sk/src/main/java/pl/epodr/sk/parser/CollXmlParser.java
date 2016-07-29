package pl.epodr.sk.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.math.NumberUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pl.epodr.sk.IdAndVersion;

public class CollXmlParser {

	private static final String ENVIRONMENT_TYPE_ELEMENT = "environment-type";

	private static final String STYLESHEET_ELEMENT = "stylesheet";

	private static final String COVER_ELEMENT = "cover";

	private static final String MODULE_ELEMENT = "module";

	private static final String MODULE_ID_ATTR = "document";

	private static final String MODULE_VERSION_ATTR = "version-at-this-collection-version";

	public static boolean isDummy(IdAndVersion coll) {
		return coll.getId().startsWith("dummy");
	}

	private Document doc;

	public CollXmlParser(InputStream is) throws IOException, ParseException {
		parse(is);
	}

	public CollXmlParser(File colxmlFile) throws FileNotFoundException, IOException, ParseException {
		try (FileInputStream fis = new FileInputStream(colxmlFile)) {
			parse(fis);
		}
	}

	private void parse(InputStream is) throws IOException, ParseException {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setNamespaceAware(true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();
		} catch (ParserConfigurationException | SAXException e) {
			throw new ParseException(e);
		}
	}

	public List<IdAndVersion> getModules() throws ParseException {
		NodeList modules = doc.getElementsByTagNameNS(NS.COLLXML, MODULE_ELEMENT);
		List<IdAndVersion> list = new ArrayList<>();
		for (int i = 0; i < modules.getLength(); i++) {
			Element module = (Element) modules.item(i);
			String id = module.getAttribute(MODULE_ID_ATTR);
			String versionStr = module.getAttributeNS(NS.CNXSI, MODULE_VERSION_ATTR);
			if (!NumberUtils.isNumber(versionStr)) {
				throw new ParseException("version '" + versionStr + "' of module '" + id + "' is not a number");
			}
			long version = Long.parseLong(versionStr);
			list.add(new IdAndVersion(id, version));
		}
		return list;
	}

	public List<String> getModuleIds() throws ParseException {
		List<String> ids = new LinkedList<>();
		for (IdAndVersion module : getModules()) {
			ids.add(module.getId());
		}
		return ids;
	}

	private Element getCoverElement() {
		NodeList nodes = doc.getElementsByTagNameNS(NS.EP, COVER_ELEMENT);
		if (nodes.getLength() > 0) {
			return (Element) nodes.item(0);
		}
		return null;
	}

	public Long getCoverId() {
		Element node = getCoverElement();
		if ((node != null) && (StringUtils.hasLength(node.getTextContent()))) {
			return Long.parseLong(node.getTextContent());
		}
		return null;
	}

	public String getStylesheet() {
		NodeList nodes = doc.getElementsByTagNameNS(NS.EP, STYLESHEET_ELEMENT);
		if (nodes.getLength() > 0) {
			Element node = (Element) nodes.item(0);
			if (node != null) {
				return node.getTextContent();
			}
		}
		return null;
	}

	public boolean isEarlyEducation() {
		NodeList nodes = doc.getElementsByTagNameNS(NS.EP, ENVIRONMENT_TYPE_ELEMENT);
		if (nodes.getLength() > 0) {
			Element node = (Element) nodes.item(0);
			if (node != null) {
				return "ee".equals(node.getTextContent());
			}
		}
		return false;
	}

	public static class ParseException extends Exception {

		public ParseException(String message, Throwable cause) {
			super(message, cause);
		}

		public ParseException(String message) {
			super(message);
		}

		public ParseException(Throwable cause) {
			super(cause);
		}

	}

}
