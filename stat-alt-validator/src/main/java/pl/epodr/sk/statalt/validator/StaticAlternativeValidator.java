package pl.epodr.sk.statalt.validator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class StaticAlternativeValidator {

	private static List<Validator> validators = new LinkedList<>();

	static {
		validators.add(new FileExistValidator("ALTERNATIVE-STATIC/metadata.xml"));
		validators.add(new FileExistValidator("ALTERNATIVE-STATIC-MONO/metadata.xml"));
		validators.add(new FileExistValidator("ALTERNATIVE-RELIEF-PRINTING/metadata.xml"));

		validators.add(new FileExistValidator("ALTERNATIVE-STATIC/content.html",
				new IsNoAlternativeValidator("ALTERNATIVE-STATIC/metadata.xml")));
		validators.add(new FileExistValidator("ALTERNATIVE-STATIC-MONO/content.html",
				new IsNoAlternativeValidator("ALTERNATIVE-STATIC-MONO/metadata.xml")));
		validators.add(new FileExistValidator("ALTERNATIVE-RELIEF-PRINTING/content.xml",
				new IsNoAlternativeValidator("ALTERNATIVE-RELIEF-PRINTING/metadata.xml")));

		validators.add(new WellFormedXmlValidator("ALTERNATIVE-STATIC/content.html"));
		validators.add(new WellFormedXmlValidator("ALTERNATIVE-STATIC/solution.html"));
		validators.add(new WellFormedXmlValidator("ALTERNATIVE-STATIC-MONO/content.html"));
		validators.add(new WellFormedXmlValidator("ALTERNATIVE-STATIC-MONO/solution.html"));
		validators.add(new WellFormedXmlValidator("ALTERNATIVE-RELIEF-PRINTING/content.xml"));
		validators.add(new WellFormedXmlValidator("ALTERNATIVE-RELIEF-PRINTING/solution.xml"));

		validators.add(new SchemaValidator("ALTERNATIVE-STATIC/metadata.xml", "metadata.xsd"));
		validators.add(new SchemaValidator("ALTERNATIVE-STATIC-MONO/metadata.xml", "metadata.xsd"));
		validators.add(new SchemaValidator("ALTERNATIVE-RELIEF-PRINTING/metadata.xml", "metadata.xsd"));
	}

	public StaticAlternativeValidator() {
	}

	/**
	 * validates the given ALTERNATIVES.zip
	 * 
	 * @param zipFile ALTERNATIVES.zip file
	 * @return a list of error messages; an empty list if the ZIP is valid
	 */
	public List<String> validate(File zipFile) {
		List<String> errors = new LinkedList<>();

		try {
			ZipFile zip = new ZipFile(zipFile, Charset.forName("Windows-1250"));
			for (Validator validator : validators) {
				validator.validate(zip, errors);
			}
		} catch (ZipException e) {
			errors.add("Problem z formatem pliku ZIP - " + e.getMessage());
		} catch (IOException e) {
			errors.add("Problem I/O - " + e.getMessage());
		}

		return errors;
	}

}
