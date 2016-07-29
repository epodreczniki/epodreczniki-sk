package pl.epodr.sk.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.files.FilePathManager;
import pl.epodr.sk.task.Killable;

@Service
@Slf4j
public class EpxmlPreprocessor {

	private static final String SCRIPT = "epxml_preprocessing.py -i %s -o %s";

	@Autowired
	protected PythonRunner pythonRunner;

	@Autowired
	protected FilePathManager path;

	public void preprocess(IdAndVersion coll, Killable parent) {
		File workingDir = path.getWorkingDirectory(coll);
		try {
			File tmpDir = Files.createTempDirectory("epxml-prep-").toFile();
			String script = String.format(SCRIPT, workingDir, tmpDir);
			pythonRunner.runScript(script, parent);
			path.moveDirectory(tmpDir, workingDir);
		} catch (IOException | InterruptedException e) {
			log.error(e.toString(), e);
		}
	}

}
