package pl.epodr.sk.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.math.NumberUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pl.epodr.sk.task.DownloadWomiTask.WomiErrorRefs;

public class EpxmlParser {

	private static final String WOMI_REFERENCE_ELEMENT = "reference";

	private static final String ID_ATTR = "id";

	private static final String CONTENT_ELEMENT = "content";

	private static final String FORMAT_ATTR = "format";

	private Document doc;

	public EpxmlParser(InputStream is) throws IOException, ParseException {
		parse(is);
	}

	public EpxmlParser(File epxmlFile) throws FileNotFoundException, IOException, ParseException {
		try (FileInputStream fis = new FileInputStream(epxmlFile)) {
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

	public List<Long> getWomiReferences() throws ParseException {
		NodeList modules = doc.getElementsByTagNameNS(NS.EP, WOMI_REFERENCE_ELEMENT);
		Set<Long> set = new HashSet<>();
		for (int i = 0; i < modules.getLength(); i++) {
			Element epRef = (Element) modules.item(i);
			String epId = epRef.getAttributeNS(NS.EP, ID_ATTR);
			if (NumberUtils.isNumber(epId)) {
				set.add(Long.parseLong(epId));
			} else {
				throw new ParseException("WOMI id is not a number: " + epId);
			}
		}
		return new ArrayList<>(set);
	}

	public void rewriteWomisToAnother(WomiErrorRefs womiErrors, long newWomiId) {
		NodeList womiRefs = doc.getElementsByTagNameNS(NS.EP, WOMI_REFERENCE_ELEMENT);
		for (int i = 0; i < womiRefs.getLength(); i++) {
			Element epRef = (Element) womiRefs.item(i);
			String epId = epRef.getAttributeNS(NS.EP, ID_ATTR);
			if (NumberUtils.isNumber(epId)) {
				long womiId = Long.parseLong(epId);
				if (womiErrors.containsKey(womiId)) {
					epRef.setAttributeNS(NS.EP, NS.EP_PREFIX + ":" + ID_ATTR, newWomiId + "");
					String msg = "!!! " + womiErrors.get(womiId) + " !!!";
					for (ReferenceContentFormat format : ReferenceContentFormat.values()) {
						setMessage(epRef, format, msg);
					}
				}
			}
		}
	}

	private void setMessage(Element epRef, ReferenceContentFormat format, String message) {
		NodeList contents = epRef.getElementsByTagNameNS(NS.EP, CONTENT_ELEMENT);
		for (int i = 0; i < contents.getLength(); i++) {
			Element content = (Element) contents.item(i);
			if (format.toString().equals(content.getAttributeNS(NS.EP, FORMAT_ATTR))) {
				String currentMsg = content.getTextContent();
				if (currentMsg != null) {
					content.setTextContent(message + " " + currentMsg);
				} else {
					content.setTextContent(message);
				}
				return;
			}
		}

		Element content = doc.createElementNS(NS.EP, NS.EP_PREFIX + ":" + CONTENT_ELEMENT);
		epRef.appendChild(content);
		content.setAttributeNS(NS.EP, NS.EP_PREFIX + ":" + ID_ATTR, UUID.randomUUID().toString());
		content.setAttributeNS(NS.EP, NS.EP_PREFIX + ":" + FORMAT_ATTR, format.toString());
		content.setTextContent(message);
	}

	public void overwriteEpxml(OutputStream os) throws IOException {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.transform(new DOMSource(doc), new StreamResult(os));
		} catch (TransformerException e) {
			throw new IOException(e);
		}
	}

	public boolean hasValidModuleIds(String expectedId) throws ParseException {
		Element root = doc.getDocumentElement();
		if (!expectedId.equals(root.getAttribute("id"))) {
			return false;
		}
		if (!expectedId.equals(root.getAttribute("module-id"))) {
			return false;
		}

		Element metadataElement = getChildElementByName(root, NS.CNXML, "metadata");
		Element contentIdElement = getChildElementByName(metadataElement, NS.MDML, "content-id");
		if (!expectedId.equals(contentIdElement.getTextContent())) {
			return false;
		}

		return true;
	}

	private Element getChildElementByName(Element parent, String childNameNS, String childName) throws ParseException {
		NodeList children = parent.getElementsByTagNameNS(childNameNS, childName);
		if (children.getLength() != 1) {
			throw new ParseException(String.format("it should be only 1 %s element in %s", childName,
					parent.getLocalName()));
		}
		return (Element) children.item(0);
	}

	public List<String> getAllElementIds() {
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList elements = null;
		try {
			elements = (NodeList) xpath.evaluate("//*[@id]", doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new IllegalStateException(e);
		}
		final String attributeLocalName = "id";
		List<String> sectionIds = new LinkedList<>();
		for (int i = 0; i < elements.getLength(); i++) {
			Element element = (Element) elements.item(i);
			if (element.hasAttribute(attributeLocalName)) {
				sectionIds.add(element.getAttribute(attributeLocalName));
			}
		}
		return sectionIds;
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

enum ReferenceContentFormat {
	CLASSIC("classic"), MOBILE("mobile"), STATIC("static"), STATIC_MONO("static-mono");

	private String toStringVal;

	private ReferenceContentFormat(String toStringVal) {
		this.toStringVal = toStringVal;
	}

	@Override
	public String toString() {
		return toStringVal;
	}
}