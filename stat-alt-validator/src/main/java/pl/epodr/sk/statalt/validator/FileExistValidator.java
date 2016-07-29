package pl.epodr.sk.statalt.validator;

import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileExistValidator implements Validator {

	private final String filename;

	private Validator condition;

	public FileExistValidator(String filename) {
		this.filename = filename;
	}

	/**
	 * this validator is going to validate if and only if the condition is fulfilled (validated)
	 * 
	 * @param condition a condition that must be fulfilled (validated) before running this validator
	 */
	public FileExistValidator(String filename, Validator condition) {
		this(filename);
		this.condition = condition;
	}

	@Override
	public boolean validate(ZipFile zip, List<String> errors) {
		if (condition != null && condition.validate(zip, new LinkedList<String>())) {
			return false;
		}

		ZipEntry entry = zip.getEntry(filename);
		if (entry != null) {
			return true;
		}
		errors.add("Nie znaleziono pliku " + filename);
		return false;
	}
}
