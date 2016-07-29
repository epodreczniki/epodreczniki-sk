package pl.epodr.sk.converter;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

import pl.epodr.sk.task.Killable;

@Service
public class HtmlConverter extends AbstractConverter {

	private static final String SCRIPT = "collection2html.py -i %s -o %s -w %s -r %s -m o";

	@Override
	protected void convert(File collectionWorkingDir, String variant, File womiDir, Killable parent) {
		String script = String.format(SCRIPT, collectionWorkingDir, collectionWorkingDir, womiDir, variant);
		try {
			pythonRunner.runScript(script, parent);
		} catch (IOException | InterruptedException e) {
			logger.error(e.toString(), e);
		}
	}

}
