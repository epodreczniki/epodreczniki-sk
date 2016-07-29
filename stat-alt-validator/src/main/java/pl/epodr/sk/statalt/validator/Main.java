package pl.epodr.sk.statalt.validator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

class Main {

	public static void main(String[] args) throws FileNotFoundException {
		if (args.length != 1) {
			System.err.println("provide a single parameter - ALTERNATIVES.zip path");
			System.exit(1);
		}

		File zipFile = new File(args[0]);
		if (!zipFile.exists()) {
			System.err.println("the given file does not exist");
			System.exit(1);
		}

		StaticAlternativeValidator sav = new StaticAlternativeValidator();
		List<String> errors = sav.validate(zipFile);
		if (errors.size() == 0) {
			System.out.println("the file is VALID");
		} else {
			System.out.println("problems found:");
			for (String error : errors) {
				System.out.println(error);
			}
		}
	}
}
