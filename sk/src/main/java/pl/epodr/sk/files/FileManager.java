package pl.epodr.sk.files;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import lombok.Data;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.parser.CollXmlParser;
import pl.epodr.sk.parser.JsonToXmlConverter;
import pl.epodr.sk.parser.ManifestJsonParser;
import pl.epodr.sk.parser.MobilePackInfoParser;
import pl.epodr.sk.parser.SwiffyParser;
import pl.epodr.sk.task.ConversionFormat;
import pl.epodr.sk.task.MetadataForPortal;

@Service
public class FileManager {

	private static final String MANIFEST_JSON_ROOT_ELEMENT = "manifest";

	private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

	public static final int[] MOBILE_PACK_RESOLUTIONS = new int[] { 480, 980, 1440, 1920 };

	public static final int[] MOBILE_IMAGE_RESOLUTIONS = new int[] { 120, 480, 980, 1440, 1920 };

	public static final String ENCODING = "UTF-8";

	public static final String[] VARIANTS = new String[] { "student-canon", "teacher-canon" };

	private static final List<String> INTERACTIVE_ENGINES_TO_BE_DOWNLOADED_FOR_MOBILE = Arrays.asList(
			"womi_exercise_engine", "custom_womi", "processingjs_animation", "custom_logic_exercise_womi");

	@Autowired
	private DownloadManager downloadManager;

	@Autowired
	private FilePathManager path;

	private boolean dontDeleteAnyDirectory = false;

	public File saveWomiMetadata2(long womiId, IdAndVersion collection) throws IOException {
		Downloadable metadata = downloadManager.getWomiMetadata2(womiId);
		File file = path.getWomiMetadata2File(womiId, collection);
		try (InputStream is = metadata.getInputStream(); FileOutputStream fos = new FileOutputStream(file)) {
			IOUtils.copy(is, fos);
		}
		return file;
	}

	public void deleteOutputDirectory(IdAndVersion collection) throws IOException {
		if (!dontDeleteAnyDirectory) {
			File dir = path.getOutputDirectory(collection);
			deleteDirectory(dir);

			File colDir = path.getOutputDirectory(collection.getId());
			File[] files = colDir.listFiles();
			if ((files == null) || (files.length == 0)) {
				deleteDirectory(colDir);
			}
		}
	}

	public void deleteWorkingDirectory(IdAndVersion collection) throws IOException {
		if (!dontDeleteAnyDirectory) {
			File dir = path.getWorkingDirectory(collection);
			deleteDirectory(dir);
		}
	}

	private void deleteDirectory(File dir) throws IOException {
		logger.debug("deleting: " + dir);
		if (!dir.exists()) {
			return;
		}
		if (dir.isDirectory()) {
			FileUtils.deleteDirectory(dir);
		} else {
			dir.delete();
		}
	}

	public ColxmlDownloadResult saveColxml(IdAndVersion collection) throws IOException {
		Downloadable d = downloadManager.getColXml(collection);
		if (d.getContentLength() == 0) {
			throw new IOException("empty collxml is returned for " + collection);
		}
		File file = path.getColxml(collection);
		try (InputStream is = d.getInputStream(); FileOutputStream os = new FileOutputStream(file)) {
			IOUtils.copy(is, os);
		} catch (FileNotFoundException e) {
			throw new IOException(e);
		}

		return new ColxmlDownloadResult(file, d.isInEditionOnline());
	}

	public File saveEpxml(IdAndVersion module, IdAndVersion collection) throws IOException {
		Downloadable d = downloadManager.getEpxml(module);
		File file = path.getModuleEpxml(collection, module.getId());
		try (InputStream is = d.getInputStream(); FileOutputStream os = new FileOutputStream(file)) {
			IOUtils.copy(is, os);
		}
		return file;
	}

	public void downloadPdfImages(long womiId, IdAndVersion collection, long destinationWomiId) throws IOException,
			JSONException {
		ManifestJsonParser p;
		try {
			p = new ManifestJsonParser(path.getWomiManifestJsonFile(womiId, collection));
		} catch (FileNotFoundException e) {
			throw new IOException(e);
		}

		if (p.hasAdvancedStaticAlternative()) {
			logger.debug("skipping PDF image for WOMI #" + womiId + ": stat. alt. exists");
			return;
		}

		String destFilename = womiId + "-pdf.*";
		logger.debug("saving: " + destFilename);

		if (p.isWomiAudio()) {
			return;
		}

		WomiEmit womi;
		if (p.isWomiIcon()) {
			womi = downloadManager.getWomiEmitIcon(womiId, "pdf");
		} else {
			womi = downloadManager.getWomiEmit(womiId, "pdf");
		}
		destFilename = destinationWomiId + "-pdf." + womi.getExtension();
		File womiDir = path.getWomiDir(collection);
		File file = new File(womiDir, destFilename);
		FileUtils.writeByteArrayToFile(file, womi.getContent());
	}

	public void downloadWomiForMobile(long womiId, IdAndVersion collection, long destinationWomiId) throws IOException,
			ParserConfigurationException, SAXException {
		logger.debug("saving WOMI #" + womiId + " for mobile");

		ManifestJsonParser p;
		try {
			p = new ManifestJsonParser(path.getWomiManifestJsonFile(womiId, collection));
		} catch (FileNotFoundException e) {
			throw new IOException(e);
		}

		if (p.isWomiAudio()) {
			return;
		}

		if (p.isWomiInteractive()) {
			File manifestFile = path.getWomiManifestJsonFile(womiId, collection);
			String manifestJsonContent = FileUtils.readFileToString(manifestFile);
			String engine = new ManifestJsonParser(manifestJsonContent).getEngine();
			if (FileManager.INTERACTIVE_ENGINES_TO_BE_DOWNLOADED_FOR_MOBILE.contains(engine)) {
				Downloadable interactivePackage = downloadManager.getWomiInteractivePackage(womiId);
				File zipFile = File.createTempFile("womi-" + womiId + "-interactive-", ".zip");
				try (InputStream in = interactivePackage.getInputStream();
						OutputStream out = new FileOutputStream(zipFile)) {
					IOUtils.copy(in, out);
				}
				try {
					extractInteractiveWomiZip(collection, destinationWomiId, zipFile);
				} finally {
					deleteFileAndLogOnException(zipFile);
				}
				overrideManifestJsonFromInteractivePackage(collection, destinationWomiId, manifestJsonContent);
			}
		}

		for (int res : MOBILE_IMAGE_RESOLUTIONS) {
			WomiEmit womi;
			if (p.isWomiIcon()) {
				womi = downloadManager.getWomiEmitIcon(womiId, "mobile");
			} else {
				womi = downloadManager.getWomiEmit(womiId, "mobile", res);
			}

			File dir = path.getMobileWomiDir(collection, res);
			if ("svg".equals(womi.getExtension())) {
				WomiEmit womiPngForSvg = downloadManager.getWomiEmit(womiId, "mobile", res, "&svg2png");
				String destFilename = destinationWomiId + "." + womiPngForSvg.getExtension();
				File filePngForSvg = new File(dir, destFilename);
				FileUtils.writeByteArrayToFile(filePngForSvg, womiPngForSvg.getContent());
			} else {
				String destFilename = destinationWomiId + "." + womi.getExtension();
				File file = new File(dir, destFilename);
				FileUtils.writeByteArrayToFile(file, womi.getContent());
			}
		}
	}

	public void deleteFileAndLogOnException(File file) {
		try {
			Files.delete(file.toPath());
		} catch (IOException e) {
			logger.warn(String.format("error deleting %s: %s", file, e));
		}
	}

	private void overrideManifestJsonFromInteractivePackage(IdAndVersion collection, long womiId,
			String manifestJsonContent) throws IOException {
		File dir = new File(path.getMobileInteractiveWomiDir(collection), womiId + "");
		File manifest = new File(dir, "manifest.json");
		if (manifest.exists()) {
			FileUtils.writeStringToFile(manifest, manifestJsonContent);
		}
	}

	private void extractInteractiveWomiZip(IdAndVersion collection, long womiId, File zipFile) throws IOException {
		File dir = new File(path.getMobileInteractiveWomiDir(collection), womiId + "");
		try (ZipFile zip = new ZipFile(zipFile, Charset.forName("Windows-1250"))) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				try {
					ZipEntry entry = entries.nextElement();
					if (entry.isDirectory()) {
						continue;
					}
					File dest = new File(dir, entry.getName());
					dest.getParentFile().mkdirs();
					try (OutputStream os = new FileOutputStream(dest)) {
						IOUtils.copy(zip.getInputStream(entry), os);
					}
				} catch (IllegalArgumentException e) {
					if ("MALFORMED".equals(e.getMessage())) {
						logger.error("when extracting zip: " + zip, e);
					} else {
						throw e;
					}
				}
			}
		}
	}

	public void createMobileImagesDirectories(IdAndVersion collection) {
		for (int res : MOBILE_IMAGE_RESOLUTIONS) {
			path.getMobileWomiDir(collection, res).mkdirs();
		}
		path.getMobileInteractiveWomiDir(collection).mkdirs();
	}

	public void downloadAndSaveCovers(long coverId, IdAndVersion collection) throws IOException {
		WomiEmit d = downloadManager.getWomiEmit(coverId, "pdf", "?cover");
		File file = path.getWomiCoverFile(coverId, d.getExtension(), "pdf", collection);
		FileUtils.writeByteArrayToFile(file, d.getContent());

		if ("svg".equals(d.getExtension())) {
			d = downloadManager.getWomiEmit(coverId, "pdf", "?cover&svg2png");
			file = path.getWomiCoverFile(coverId, d.getExtension(), "pdf", collection);
			FileUtils.writeByteArrayToFile(file, d.getContent());
		}
	}

	public List<Long> preprocessInteractiveWomi(long womiId, IdAndVersion collection) throws IOException, JSONException {
		File womiDir = path.getWomiDir(collection);
		ManifestJsonParser manifestJson = new ManifestJsonParser(path.getWomiManifestJsonFile(womiId, collection));
		String womiEngine = manifestJson.getEngine();

		List<Long> embeddedWomis = new LinkedList<>();
		if (INTERACTIVE_ENGINES_TO_BE_DOWNLOADED_FOR_MOBILE.contains(womiEngine)) {
			embeddedWomis.addAll(manifestJson.getEmbeddedWomis());
		}

		File file;
		if ("geogebra".equals(womiEngine)) {
			file = new File(womiDir, path.getWomiGeogebraFilename(womiId));
		} else if ("swiffy".equals(womiEngine)) {
			file = new File(womiDir, path.getWomiSwiffyFilename(womiId));
		} else {
			return embeddedWomis;
		}
		logger.debug(String.format("saving womi %s: %s", womiEngine, file.getName()));
		try (InputStream html = downloadManager.getWomiEmitInteractive(womiId).getInputStream();
				FileOutputStream fos = new FileOutputStream(file)) {
			IOUtils.copy(html, fos);
		}

		if ("swiffy".equals(womiEngine)) {
			SwiffyParser p = new SwiffyParser(file);
			p.removeDoctypeAndMeta();
		}

		return embeddedWomis;
	}

	public String saveManifestJsonXml(long womiId, IdAndVersion collection) throws IOException {
		Downloadable manifest = downloadManager.getWomiManifestJson(womiId);
		File jsonFile = path.getWomiManifestJsonFile(womiId, collection);
		FileUtils.copyInputStreamToFile(manifest.getInputStream(), jsonFile);

		String manifestContent = FileUtils.readFileToString(jsonFile);
		File file = path.getWomiManifestXmlFile(womiId, collection);
		JsonToXmlConverter conv = new JsonToXmlConverter(manifestContent, MANIFEST_JSON_ROOT_ELEMENT);
		FileUtils.writeStringToFile(file, conv.getXml(), ENCODING);
		return manifestContent;
	}

	public List<String> publishCollectionStaticResults(IdAndVersion collection, String variant, String extension) {
		cleanCollectionOutputDirectoryByExtension(collection, variant, extension);
		File src = path.getWorkingDirectory(collection, variant);
		List<String> publishedFilenames = new LinkedList<>();

		if (ConversionFormat.MOBILE.getExtension().equals(extension)) {
			for (int res : MOBILE_PACK_RESOLUTIONS) {
				File srcFile = new File(src, "mobile-" + res + ".zip");
				String destFilename = "mobile-" + res + ".zip";
				publishFile(srcFile, collection, variant, destFilename);
				publishedFilenames.add(destFilename);
			}
		} else {
			File srcFile = new File(src, variant + "." + extension);
			String destFilename = FilePathManager.STATIC_EMIT_FILENAME + "." + extension;
			publishFile(srcFile, collection, variant, destFilename);
			publishedFilenames.add(destFilename);
		}
		return publishedFilenames;
	}

	private void publishFile(File srcFile, IdAndVersion collection, String variant, String destFilename) {
		if (srcFile.exists()) {
			File destFile = path.getOutputFile(collection, variant, destFilename);
			logger.debug("publishing results: " + srcFile + "=>" + destFile);
			try {
				FileUtils.copyFile(srcFile, destFile);
			} catch (IOException e) {
				logger.error("when copying " + srcFile + " to " + destFile, e);
			}
		} else {
			logger.error("file not found: " + srcFile);
		}
	}

	public void publishCollectionHtmlResults(IdAndVersion collection, String variant, List<String> modulesIds) {
		cleanCollectionOutputDirectoryByExtension(collection, variant, "html");
		File src = path.getWorkingDirectory(collection, variant);
		logger.debug(String.format("publishing HTMLs of %s - %s", collection, variant));
		for (String moduleId : modulesIds) {
			File srcFile = new File(src, moduleId + ".html");
			if (srcFile.exists()) {
				File destFile = path.getOutputHtmlFile(collection, variant, moduleId);
				try {
					FileUtils.copyFile(srcFile, destFile);
				} catch (IOException e) {
					logger.error(e.toString(), e);
				}
			} else {
				logger.error("file not found: " + srcFile);
			}
		}
	}

	private void cleanCollectionOutputDirectoryByExtension(IdAndVersion collection, String variant, String extension) {
		FileFilter filter = new WildcardFileFilter("*." + extension);
		File dir = path.getOutputDirectory(collection, variant);
		File[] files = dir.listFiles(filter);
		if (files != null) {
			for (File file : files) {
				file.delete();
			}
		}
	}

	public List<String> publishColxml(IdAndVersion collection, String variant) throws IOException {
		File src = path.getVariantColxmlForPortal(collection, variant);
		File dest = path.getVariantColxmlDestination(collection, variant);
		FileUtils.copyFile(src, dest);
		try {
			return new CollXmlParser(src).getModuleIds();
		} catch (pl.epodr.sk.parser.CollXmlParser.ParseException e) {
			throw new IllegalStateException(e);
		}
	}

	public void publishEpxml(IdAndVersion collection, String variant, String moduleId) throws IOException {
		File src = path.getVariantEpxmlSource(collection, variant, moduleId);
		File dest = path.getVariantEpxmlDestination(collection, variant, moduleId);
		FileUtils.copyFile(src, dest);
	}

	public Map<Integer, Long> getMobilePacksSizes(IdAndVersion collection, String variant) throws IOException {
		File file = path.getMobilePackInfoFile(collection, variant);
		try (InputStream is = new FileInputStream(file)) {
			MobilePackInfoParser p = new MobilePackInfoParser(is);
			return p.getMobilePacksSizes();
		}
	}

	public boolean hasErrors(IdAndVersion coll) {
		File file = path.getMetadataForPortal(coll);
		if (file.exists()) {
			MetadataForPortal metadataForPortal = new MetadataForPortal(file);
			return metadataForPortal.hasErrors();
		} else {
			return false;
		}
	}

	@Data
	public static class ColxmlDownloadResult {

		private final File colxmlFile;

		private final boolean inEditionOnline;
	}

}
