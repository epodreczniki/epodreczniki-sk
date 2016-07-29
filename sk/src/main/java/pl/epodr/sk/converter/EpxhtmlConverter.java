package pl.epodr.sk.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.stereotype.Service;

import pl.epodr.sk.task.Killable;

@Service
public class EpxhtmlConverter extends AbstractConverter {

	private static final String SCRIPT = "collection2epxhtml.py -i %s -o %s";

	@Override
	protected void convert(File collectionWorkingDir, String variant, File womiDir, Killable parent) {
		File workingDir = new File(collectionWorkingDir, variant);
		try {
			File tmpDir = Files.createTempDirectory("coll2xhtml-").toFile();
			pythonRunner.runScript(String.format(SCRIPT, workingDir, tmpDir), parent);
			super.filePathManager.moveDirectory(tmpDir, workingDir);
		} catch (IOException | InterruptedException e) {
			logger.error("when converting " + collectionWorkingDir + " on variant " + variant + " with womi dir "
					+ womiDir, e);
		}
	}

}
