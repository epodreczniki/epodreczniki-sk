package pl.epodr.sk.statalt.validator;

import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

public class SchemaValidator implements Validator {

	private final String filename;

	private final String xsdResourcePath;

	public SchemaValidator(String filename, String xsdResourcePath) {
		this.filename = filename;
		this.xsdResourcePath = xsdResourcePath;
	}

	@Override
	public boolean validate(ZipFile zip, List<String> errors) {
		ZipEntry entry = zip.getEntry(filename);
		if (entry == null) {
			return false;
		}

		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			Schema schema = factory.newSchema(new StreamSource(getClass().getResourceAsStream(xsdResourcePath)));
			javax.xml.validation.Validator validator = schema.newValidator();
			try (InputStream is = zip.getInputStream(entry)) {
				validator.validate(new StreamSource(is));
			}
			return true;
		} catch (Exception e) {
			errors.add("Błąd walidacji " + filename + " - " + e.getMessage());
			return false;
		}
	}
}
