package pl.epodr.sk.statalt.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class WellFormedXmlValidator implements Validator {

	private static final Pattern NAMED_ENTITY = Pattern.compile("&[a-zA-Z].{0,10};");

	private final String filename;

	public WellFormedXmlValidator(String filename) {
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
				builder.parse(is);
			}
		} catch (SAXException | IOException | ParserConfigurationException e) {
			errors.add(filename + " nie jest well-formed XML - " + e.getMessage());
		}
		return false;
	}

	String findNamedEntity(String text) {
		Matcher m = NAMED_ENTITY.matcher(text);
		if (m.find()) {
			return m.group();
		}
		return null;
	}
}
