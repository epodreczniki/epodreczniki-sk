package pl.epodr.sk.converter.css;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;

@Slf4j
public abstract class AbstractCssManager {

	protected static final String CSS_DIR_NAME = "css";

	@Value("${pl.epodr.sk.converter.PythonRunner.workingDir}")
	protected String workingDir;

	protected final Set<String> stylesheets = new HashSet<>();

	abstract String extractStylesheetNameFromFilename(String filename);

	abstract String buildCssFilename(String stylesheet);

	abstract String getDefaultStylesheetName();

	@PostConstruct
	void init() {
		scanCssFiles();
	}

	public void scanCssFiles() {
		File cssDir = new File(workingDir, CSS_DIR_NAME);
		File[] files = cssDir.listFiles();
		stylesheets.clear();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				String stylesheet = extractStylesheetNameFromFilename(files[i].getName());
				if (stylesheet != null) {
					stylesheets.add(stylesheet);
				}
			}
		}
		log.info(this.getClass().getSimpleName() + " - stylesheets found: " + stylesheets);
	}

	public String getCssPath(String stylesheet) {
		if (stylesheet == null) {
			return buildCssPath(getDefaultStylesheetName());
		}

		stylesheet = stylesheet.trim().toLowerCase();

		if (stylesheets.contains(stylesheet)) {
			return buildCssPath(stylesheet);
		}
		return buildCssPath(getDefaultStylesheetName());
	}

	private String buildCssPath(String stylesheet) {
		File cssDir = new File(workingDir, CSS_DIR_NAME);
		String filename = buildCssFilename(stylesheet);
		File file = new File(cssDir, filename);
		return file.getAbsolutePath();
	}
}
