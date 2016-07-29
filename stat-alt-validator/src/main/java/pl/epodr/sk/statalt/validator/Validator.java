package pl.epodr.sk.statalt.validator;

import java.util.List;
import java.util.zip.ZipFile;

public interface Validator {

	/**
	 * @return true if valid
	 */
	boolean validate(ZipFile zip, List<String> errors);

}
