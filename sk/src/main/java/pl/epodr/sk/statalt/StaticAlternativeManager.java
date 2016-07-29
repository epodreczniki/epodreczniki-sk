package pl.epodr.sk.statalt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pl.epodr.sk.files.DownloadManager;
import pl.epodr.sk.files.FileManager;
import pl.epodr.sk.statalt.validator.StaticAlternativeValidator;

@Service
@Slf4j
public class StaticAlternativeManager {

	@Autowired
	private DownloadManager downloadManager;

	@Autowired
	private FileManager fileManager;

	public Set<Long> downloadAndProcess(long womiId, File workingDir) throws IOException, InvalidStaticAlternative {
		byte[] zipContent = downloadManager.getStaticAlternative(womiId);

		File file = File.createTempFile("statalt-" + womiId + "-", ".zip");
		FileUtils.writeByteArrayToFile(file, zipContent);
		try {
			StaticAlternativeValidator validator = new StaticAlternativeValidator();
			List<String> errors = validator.validate(file);
			if (errors.size() > 0) {
				log.info(errors.size() + " errors found in stat. alt. for WOMI #" + womiId);
				throw new InvalidStaticAlternative(errors);
			}

			try (ZipFile zf = new ZipFile(file)) {
				Set<Long> womis = new HashSet<>();
				womis.addAll(processForChosenFormat(womiId, workingDir, zf, "ALTERNATIVE-STATIC", Scope.pdf));
				return womis;
			}
		} finally {
			fileManager.deleteFileAndLogOnException(file);
		}
	}

	private List<Long> processForChosenFormat(long womiId, File workingDir, ZipFile zipFile, String directory,
			Scope format) throws IOException {
		Metadata metadata = new Metadata(zipFile.getInputStream(zipFile.getEntry(directory + "/metadata.xml")));
		if (!metadata.doesAltExists()) {
			extract(zipFile, directory + "/metadata.xml", new File(workingDir, womiId + "-" + format + "-no-alt.xml"));
		} else {
			extract(zipFile, directory + "/content.html", new File(workingDir, womiId + "-" + format + "-content.html"));
			try {
				extract(zipFile, directory + "/solution.html", new File(workingDir, womiId + "-" + format
						+ "-solution.html"));
			} catch (FileNotFoundException e) {
			}
			List<Image> images = metadata.getImages();
			for (Image image : images) {
				extract(zipFile,
						directory + "/" + image.getFilename(),
						new File(workingDir, womiId + "-" + image.getId() + "-" + format + "."
								+ image.getFilenameExtension()));
				FileUtils.writeStringToFile(new File(workingDir, womiId + "-" + image.getId() + "-" + format
						+ "-formats.xml"), image.getFormatsXml(womiId, format));
				FileUtils.writeStringToFile(new File(workingDir, womiId + "-" + image.getId() + "-" + format
						+ "-metadata2.xml"), image.getMetadata2Xml(womiId, format));
			}
		}
		return metadata.getWomis();
	}

	private void extract(ZipFile zf, String path, File outputFile) throws IOException {
		ZipEntry e = zf.getEntry(path);
		if (e == null) {
			throw new FileNotFoundException(path + " nie istnieje");
		}
		try (InputStream is = zf.getInputStream(e); OutputStream fos = new FileOutputStream(outputFile)) {
			log.trace("extracting " + path + " to " + outputFile);
			IOUtils.copy(is, fos);
		}
	}

}
