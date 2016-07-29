package pl.epodr.sk.statalt.validator;

import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class IsNoAlternativeValidator implements Validator {

	private final String filename;

	public IsNoAlternativeValidator(String filename) {
		this.filename = filename;
	}

	@Override
	public boolean validate(ZipFile zip, List<String> errors) {
		ZipEntry entry = zip.getEntry(filename);
		if (entry == null) {
			return false;
		}

		DocumentBuilderFactory dBF = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = dBF.newDocumentBuilder();
			try (InputStream is = zip.getInputStream(entry)) {
				Document doc = builder.parse(is);
				NodeList nodeList = doc.getElementsByTagName("no-alternative-reason");
				return nodeList.getLength() == 1;
			}
		} catch (Exception e) {
			return false;
		}
	}

}
