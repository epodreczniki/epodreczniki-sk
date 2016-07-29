package pl.epodr.sk.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.stereotype.Service;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.task.Killable;

@Service
public class SplitVariantsConverter extends AbstractConverter {

	private static final String SCRIPT = "collection2variants.py -i %s -o %s";

	@Override
	protected void convert(File collectionWorkingDir, String variant, File womiDir, Killable parent) {
		convert(collectionWorkingDir, parent);
	}

	public final void convert(IdAndVersion collection, Killable parent) {
		File collectionWorkingDir = filePathManager.getWorkingDirectory(collection);
		convert(collectionWorkingDir, parent);
	}

	private void convert(File collectionWorkingDir, Killable parent) {
		try {
			File tmpDir = Files.createTempDirectory("split-variants-").toFile();
			String script = String.format(SCRIPT, collectionWorkingDir, tmpDir);
			pythonRunner.runScript(script, parent);
			super.filePathManager.moveDirectory(tmpDir, collectionWorkingDir);
		} catch (IOException | InterruptedException e) {
			logger.error("when splitting variants for input dir: " + collectionWorkingDir, e);
		}
	}

}
