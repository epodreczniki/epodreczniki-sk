package pl.epodr.sk.converter.css;

import org.springframework.stereotype.Service;

@Service
public class PdfCssManager extends AbstractCssManager {

	private static final String CSS_FILE_PREFIX = "pdf-";

	private static final String CSS_FILE_SUFFIX = ".css";

	@Override
	String extractStylesheetNameFromFilename(String filename) {
		if (filename.startsWith(CSS_FILE_PREFIX) && filename.endsWith(CSS_FILE_SUFFIX)) {
			String stylesheetName = filename.substring(CSS_FILE_PREFIX.length(),
					filename.length() - CSS_FILE_SUFFIX.length());
			if (stylesheetName.length() > 0) {
				return stylesheetName;
			} else {
				return null;
			}
		}
		return null;
	}

	@Override
	String buildCssFilename(String stylesheet) {
		return CSS_FILE_PREFIX + stylesheet + CSS_FILE_SUFFIX;
	}

	@Override
	String getDefaultStylesheetName() {
		return "standard-2";
	}
}
