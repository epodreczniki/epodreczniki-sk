package pl.epodr.sk.converter;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

import pl.epodr.sk.task.Killable;

@Service
public class MobileConverter extends AbstractConverter {

	private static final String SCRIPT = "collection2mobile_app.py -i %s -o %s -w %s -r %s -s %s -sc %s -m o";

	private final static String CSS_PATH = "css/mobile_app.css";

	private final static String CSS_DIR_PATH = "css/mobile_app";

	@Override
	protected void convert(File collectionWorkingDir, String variant, File womiDir, Killable parent) {
		String script = String.format(SCRIPT, collectionWorkingDir, collectionWorkingDir, womiDir, variant, CSS_PATH,
				CSS_DIR_PATH);
		try {
			pythonRunner.runScript(script, parent);
		} catch (IOException | InterruptedException e) {
			logger.error(e.toString(), e);
		}
	}

}
