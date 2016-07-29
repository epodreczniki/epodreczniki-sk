package pl.epodr.sk.converter;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.files.FilePathManager;
import pl.epodr.sk.task.Killable;

public abstract class AbstractConverter {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractConverter.class);

	@Autowired
	protected PythonRunner pythonRunner;

	@Autowired
	protected FilePathManager filePathManager;

	public final void convert(IdAndVersion collection, String variant, Killable parent) {
		File collectionWorkingDir = filePathManager.getWorkingDirectory(collection);
		File womiDir = filePathManager.getWomiDir(collection);
		if (!womiDir.exists()) {
			womiDir.mkdir();
		}
		convert(collectionWorkingDir, variant, womiDir, parent);
	}

	protected abstract void convert(File collectionWorkingDir, String variant, File womiDir, Killable parent);

}
