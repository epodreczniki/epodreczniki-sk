package pl.epodr.sk.parser;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class SwiffyParser {

	private static final String DOCTYPE = "<!doctype html>";

	private static final String META_PATTERN = "<meta[^>]+>";

	private final File file;

	public SwiffyParser(File file) {
		this.file = file;
	}

	public void removeDoctypeAndMeta() throws IOException {
		String content = readFile();
		content = content.replace(DOCTYPE, "");
		content = content.replaceAll(META_PATTERN, "");
		saveFile(content);
	}

	private void saveFile(String content) throws IOException {
		FileUtils.writeStringToFile(file, content);
	}

	private String readFile() throws IOException {
		return FileUtils.readFileToString(file);
	}

}
