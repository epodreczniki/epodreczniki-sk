package pl.epodr.sk.converter;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pl.epodr.sk.converter.css.PdfCssManager;
import pl.epodr.sk.files.FilePathManager;
import pl.epodr.sk.parser.CollXmlParser;
import pl.epodr.sk.parser.CollXmlParser.ParseException;
import pl.epodr.sk.task.Killable;

@Service
public class PdfConverter extends AbstractConverter {

	private static final String SCRIPT = "collection2pdf.py -i %s -o %s -w %s -r %s -s %s -pp %s -m o";

	@Value("${pl.epodr.sk.converter.PdfConverter.princeXmlPath}")
	private String princeXmlPath;

	@Autowired
	private PdfCssManager pdfCssManager;

	@Override
	protected void convert(File collectionWorkingDir, String variant, File womiDir, Killable parent) {
		String cssPath = getCssByStylesheet(collectionWorkingDir);
		String script = String.format(SCRIPT, collectionWorkingDir, collectionWorkingDir, womiDir, variant, cssPath,
				princeXmlPath);
		try {
			pythonRunner.runScript(script, parent);
		} catch (IOException | InterruptedException e) {
			logger.error(e.toString(), e);
		}
	}

	private String getCssByStylesheet(File collectionWorkingDir) {
		File colxmlFile = new File(collectionWorkingDir, FilePathManager.INDEX_COLXML);
		CollXmlParser parser;
		try {
			parser = new CollXmlParser(colxmlFile);
			String stylesheet = parser.getStylesheet();
			return pdfCssManager.getCssPath(stylesheet);
		} catch (IOException | ParseException e) {
			logger.error("when parsing colxml from " + collectionWorkingDir, e);
			return pdfCssManager.getCssPath(null);
		}
	}

}
