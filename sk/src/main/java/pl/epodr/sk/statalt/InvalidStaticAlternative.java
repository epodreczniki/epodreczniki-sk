package pl.epodr.sk.statalt;

import java.util.List;

public class InvalidStaticAlternative extends Exception {

	private final List<String> errors;

	public InvalidStaticAlternative(List<String> errors) {
		this.errors = errors;
	}

	public List<String> getErrors() {
		return errors;
	}

}
