package pl.epodr.sk.statalt;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class Metadata {

	private Document doc;

	public Metadata(InputStream inputStream) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(inputStream);
			doc.getDocumentElement().normalize();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public boolean doesAltExists() {
		NodeList noAlt = doc.getElementsByTagName("no-alternative-reason");
		return noAlt.getLength() == 0;
	}

	public List<Image> getImages() {
		List<Image> list = new ArrayList<>();
		NodeList imageNodes = doc.getElementsByTagName("image");
		for (int i = 0; i < imageNodes.getLength(); i++) {
			Element imageElement = (Element) imageNodes.item(i);
			long id = Long.parseLong(imageElement.getAttribute("id"));
			String filename = imageElement.getAttribute("filename");
			String author = imageElement.getAttribute("author");
			String licence = imageElement.getAttribute("licence");
			list.add(new Image(id, filename, author, licence));
		}
		return list;
	}

	public List<Long> getWomis() {
		List<Long> list = new ArrayList<>();
		NodeList womiNodes = doc.getElementsByTagName("womi");
		for (int i = 0; i < womiNodes.getLength(); i++) {
			Element womiElement = (Element) womiNodes.item(i);
			long id = Long.parseLong(womiElement.getAttribute("id"));
			list.add(id);
		}
		return list;
	}

}
