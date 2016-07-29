package pl.epodr.sk.files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pl.epodr.sk.IdAndVersion;

@Service
public class FilePathManager {

	private static final String WOMI_DIR = "womi";

	private static final String MOBILE_WOMI_DIR = "mobile-app";

	private static final String MOBILE_INTERACTIVE_WOMI_DIR = "womi_engine";

	private static final String INDEX_EPXML = "index.epxml";

	public static final String INDEX_COLXML = "collection.xml";

	private static final String INDEX_COLXML_FOR_PORTAL = "collection_portal.xml";

	static final String STATIC_EMIT_FILENAME = "collection";

	private static final String MOBILE_PACK_INFO_FILENAME = "zip_info.txt";

	@Value("${pl.epodr.sk.files.FileManager.workingDir}")
	private String workingDir;

	@Value("${pl.epodr.sk.files.FileManager.outputDir}")
	private String outputDir;

	private final CollectionNameComparator collectionNameComparator = new CollectionNameComparator();

	public File getWorkingDirectory(IdAndVersion collection) {
		return new File(new File(workingDir), collection.getId() + "-" + collection.getVersion());
	}

	File getWorkingDirectory(IdAndVersion collection, String variant) {
		return new File(getWorkingDirectory(collection), variant);
	}

	public File getModuleEpxml(IdAndVersion collection, String moduleId) {
		File dir = getWorkingDirectory(collection);
		dir = new File(dir, moduleId);
		dir.mkdir();
		return new File(dir, INDEX_EPXML);
	}

	public File getColxml(IdAndVersion collection) {
		File dir = getWorkingDirectory(collection);
		dir.mkdir();
		return new File(dir, INDEX_COLXML);
	}

	public File getColxml(IdAndVersion collection, String variant) {
		File dir = getWorkingDirectory(collection, variant);
		return new File(dir, INDEX_COLXML);
	}

	private File getOutputDirectory() {
		return new File(outputDir);
	}

	File getOutputDirectory(String collectionId) {
		return new File(getOutputDirectory(), collectionId + "");
	}

	File getOutputDirectory(IdAndVersion collection) {
		return new File(getOutputDirectory(collection.getId()), collection.getVersion() + "");
	}

	File getOutputDirectory(IdAndVersion collection, String variant) {
		return new File(getOutputDirectory(collection), variant);
	}

	File getOutputFile(IdAndVersion collection, String variant, String filename) {
		File dir = getOutputDirectory(collection, variant);
		return new File(dir, filename);
	}

	File getOutputHtmlFile(IdAndVersion collection, String variant, String moduleId) {
		return getOutputFile(collection, variant, moduleId + ".html");
	}

	File getWomiMetadata2File(long womiId, IdAndVersion collection) {
		return new File(getWomiDir(collection), womiId + "-metadata2.xml");
	}

	public File getWomiManifestJsonFile(long womiId, IdAndVersion collection) {
		return new File(getWomiDir(collection), womiId + "-manifest.json");
	}

	File getWomiManifestXmlFile(long womiId, IdAndVersion collection) {
		return new File(getWomiDir(collection), womiId + "-manifest.json.xml");
	}

	File getWomiCoverFile(long womiId, String coverExtension, String emitFormat, IdAndVersion collection) {
		return new File(getWomiDir(collection), womiId + "-" + emitFormat + "-cover." + coverExtension);
	}

	String getWomiSwiffyFilename(long womiId) {
		return womiId + "-swiffy-preprocessed.html";
	}

	String getWomiGeogebraFilename(long womiId) {
		return womiId + "-geogebra.html";
	}

	public File getWomiDir(IdAndVersion collection) {
		return new File(getWorkingDirectory(collection), WOMI_DIR);
	}

	File getMobileWomiDir(IdAndVersion collection, int resolution) {
		File commonMobileDir = new File(getWomiDir(collection), MOBILE_WOMI_DIR);
		return new File(commonMobileDir, resolution + "");
	}

	File getMobileInteractiveWomiDir(IdAndVersion collection) {
		File commonMobileDir = new File(getWomiDir(collection), MOBILE_WOMI_DIR);
		return new File(commonMobileDir, MOBILE_INTERACTIVE_WOMI_DIR);
	}

	private List<String> getFileNames(File dir) {
		File[] files = dir.listFiles();
		List<String> list = new ArrayList<>();
		if (files != null) {
			for (File file : files) {
				if (!file.isDirectory()) {
					list.add(file.getName());
				}
			}
		}
		return list;
	}

	private List<String> getFileInDirectoryNames(File dir) {
		File[] files = dir.listFiles();
		List<String> list = new ArrayList<>();
		if (files != null) {
			for (File file : files) {
				list.add(file.getName());
			}
		}
		return list;
	}

	public List<String> getCollections() {
		File dir = getOutputDirectory();
		List<String> list = getFileInDirectoryNames(dir);
		Collections.sort(list, this.collectionNameComparator);
		return list;
	}

	public List<String> getCollectionVersions(String collectionId) {
		File dir = getOutputDirectory(collectionId);
		if (!dir.exists()) {
			return null;
		}
		List<String> list = getFileInDirectoryNames(dir);
		Collections.sort(list);
		return list;
	}

	public List<String> getCollectionVariants(IdAndVersion collection) {
		File dir = getOutputDirectory(collection);
		if (!dir.exists()) {
			return null;
		}
		List<String> list = getFileInDirectoryNames(dir);
		Collections.sort(list);
		return list;
	}

	public List<String> getCollectionOutputFiles(IdAndVersion collection, String variant) {
		File dir = getOutputDirectory(collection, variant);
		if (!dir.exists()) {
			return null;
		}
		List<String> list = getFileNames(dir);
		Collections.sort(list);
		return list;
	}

	File getVariantColxmlForPortal(IdAndVersion collection, String variant) {
		File dir = getWorkingDirectory(collection, variant);
		return new File(dir, INDEX_COLXML_FOR_PORTAL);
	}

	File getVariantEpxmlSource(IdAndVersion collection, String variant, String moduleId) {
		File dir = new File(getWorkingDirectory(collection, variant), moduleId);
		return new File(dir, INDEX_EPXML);
	}

	File getVariantColxmlDestination(IdAndVersion collection, String variant) {
		File dir = getOutputDirectory(collection, variant);
		return new File(dir, INDEX_COLXML);
	}

	File getVariantEpxmlDestination(IdAndVersion collection, String variant, String moduleId) {
		File dir = getOutputDirectory(collection, variant);
		return new File(dir, moduleId + ".xml");
	}

	public File getMetadataForPortal(IdAndVersion collection) {
		return new File(getOutputDirectory(collection), "metadata.xml");
	}

	File getMobilePackInfoFile(IdAndVersion collection, String variant) {
		File dir = getWorkingDirectory(collection, variant);
		return new File(dir, MOBILE_PACK_INFO_FILENAME);
	}

	public void moveDirectory(File srcDir, File destDir) throws IOException {
		FileUtils.copyDirectory(srcDir, destDir);
		FileUtils.deleteDirectory(srcDir);
	}

	public File getSourceMathmlAltTextFile(IdAndVersion coll) {
		return new File(getWorkingDirectory(coll), "mathml_digest.xml");
	}

	public File getDestinationMathmlAltTextFile(IdAndVersion coll, String variant) {
		return new File(getWorkingDirectory(coll, variant), "mathml_digest.xml");
	}

	private class CollectionNameComparator implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			boolean isNum1 = NumberUtils.isNumber(o1);
			boolean isNum2 = NumberUtils.isNumber(o2);
			if (!isNum1 && !isNum2) {
				return o1.compareTo(o2);
			}
			if (isNum1 && !isNum2) {
				return -1;
			}
			if (!isNum1 && isNum2) {
				return 1;
			}
			long i1 = Long.parseLong(o1);
			long i2 = Long.parseLong(o2);
			return Long.compare(i1, i2);
		}

	}

}
