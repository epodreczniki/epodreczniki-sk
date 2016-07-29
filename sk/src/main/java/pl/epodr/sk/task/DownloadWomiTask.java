package pl.epodr.sk.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.files.DownloadManager;
import pl.epodr.sk.parser.CollXmlParser;
import pl.epodr.sk.parser.EpxmlParser;
import pl.epodr.sk.parser.EpxmlParser.ParseException;
import pl.epodr.sk.parser.ManifestJsonParser;
import pl.epodr.sk.statalt.InvalidStaticAlternative;
import pl.epodr.sk.statalt.StaticAlternativeManager;
import pl.epodr.sk.task.common.ConversionReason;
import pl.epodr.sk.task.notifications.CoverNotFoundError;

public class DownloadWomiTask extends CollectionTransformationTask {

	@Autowired
	private DownloadManager downloadManager;

	@Autowired
	private StaticAlternativeManager staticAlternativeManager;

	private final WomiErrorRefs womiNotFoundRefs = new WomiErrorRefs("Nie znaleziono WOMI %d");

	private final WomiErrorRefs womiErrorRefs = new WomiErrorRefs("Nieprawidłowa struktura pliku ZIP w WOMI %d");

	private final CollXmlParser parser;

	private final List<ConversionFormat> staticFormatsToGenerate;

	private final Map<Long, Set<String>> womiRefs;

	public DownloadWomiTask(IdAndVersion collectionDescriptor, ConversionReason reason, CollXmlParser parser,
			List<ConversionFormat> formatsToGenerate, Map<Long, Set<String>> womiRefs) {
		super(collectionDescriptor, reason);
		this.parser = parser;
		this.staticFormatsToGenerate = formatsToGenerate;
		this.womiRefs = womiRefs;
	}

	@Override
	public void process() throws IOException {
		IdAndVersion coll = getCollectionDescriptor();
		indexDatabase.removeCollection(coll);
		downloadWomiXmlsAndInteractivePackages(womiRefs.keySet());

		if (womiNotFoundRefs.size() > 0) {
			long notFoundWomiId = downloadManager.getNotFoundWomiId();
			logger.debug("missing WOMIs are: " + womiNotFoundRefs.keySet() + "; rewriting to " + notFoundWomiId);
			downloadWomiXmlsAndInteractivePackages(Arrays.asList(notFoundWomiId));
			rewriteWomisToAnother(womiNotFoundRefs, notFoundWomiId);
		}
		if (womiErrorRefs.size() > 0) {
			long errorWomiId = downloadManager.getErrorWomiId();
			logger.debug("error WOMIs are: " + womiErrorRefs.keySet() + "; rewriting to " + errorWomiId);
			downloadWomiXmlsAndInteractivePackages(Arrays.asList(errorWomiId));
			rewriteWomisToAnother(womiErrorRefs, errorWomiId);
		}

		Long coverId = parser.getCoverId();
		if (coverId != null) {
			indexDatabase.putCollectionForWomi(coll, coverId);
			try {
				downloadInfoFiles(coll, coverId);
			} catch (FileNotFoundException e) {
				logger.error("error downloading cover #" + coverId + " in " + this + ": " + e);
				stopWithTransformationError(new CoverNotFoundError(coverId));
			} catch (IOException e) {
				runNextAttemptOrStop(e);
				throw new TaskKilledException();
			}
		}

		indexDatabase.commit();
	}

	private void downloadInfoFiles(IdAndVersion coll, long womiId) throws IOException {
		logger.debug("saving metadata and manifest for womi #" + womiId);
		fileManager.saveWomiMetadata2(womiId, coll);
		fileManager.saveManifestJsonXml(womiId, coll);
	}

	private void downloadWomiXmlsAndInteractivePackages(Collection<Long> womiRefs) {
		IdAndVersion coll = getCollectionDescriptor();

		path.getWomiDir(coll).mkdirs();

		List<Long> womis = new LinkedList<>(womiRefs);
		womis.add(downloadManager.getErrorWomiId());

		List<Long> allEmbeddedWomiIds = new LinkedList<>();

		for (long womiId : womis) {
			if (this.isKilled()) {
				throw new TaskKilledException();
			}
			allEmbeddedWomiIds.addAll(downloadWomiXmlsAndInteractivePackages(womiId));
		}

		allEmbeddedWomiIds.removeAll(womis);
		for (long womiId : allEmbeddedWomiIds) {
			downloadWomiXmlsAndInteractivePackages(womiId);
			this.womiRefs.put(womiId, new HashSet<String>());
		}
	}

	private List<Long> downloadWomiXmlsAndInteractivePackages(long womiId) {
		IdAndVersion coll = getCollectionDescriptor();
		List<Long> embeddedWomiIds = new LinkedList<>();

		if (womiId != downloadManager.getErrorWomiId() && womiId != downloadManager.getNotFoundWomiId()) {
			indexDatabase.putCollectionForWomi(coll, womiId);
		}
		try {
			downloadInfoFiles(coll, womiId);
			embeddedWomiIds.addAll(fileManager.preprocessInteractiveWomi(womiId, coll));

			File manifestJsonFile = path.getWomiManifestJsonFile(womiId, coll);
			if (new ManifestJsonParser(manifestJsonFile).hasAdvancedStaticAlternative()) {
				downloadStaticAlternatives(womiId);
			}
		} catch (FileNotFoundException e) {
			womiNotFoundRefs.add(womiId);
			logger.debug("when downloading and proprocessing WOMI #" + womiId + " in " + this + ": " + e);
		} catch (IOException e) {
			if (e.getCause() instanceof SocketTimeoutException) {
				runNextAttemptOrStop(e);
				throw new TaskKilledException();
			} else {
				womiErrorRefs.add(womiId);
				logger.debug("when downloading and proprocessing WOMI #" + womiId + " in " + this + ": " + e);
			}
		} catch (JSONException e) {
			womiErrorRefs.add(womiId);
			logger.debug("when downloading and proprocessing WOMI #" + womiId + " in " + this + ": " + e);
		}
		return embeddedWomiIds;
	}

	private void rewriteWomisToAnother(WomiErrorRefs womisToRewrite, long destinationWomiId) throws IOException {
		Set<String> modules = new HashSet<>();
		for (long womiId : womisToRewrite.keySet()) {
			Set<String> modulesWhereTheWomiIsUsed = womiRefs.get(womiId);
			if (modulesWhereTheWomiIsUsed != null) {
				modules.addAll(modulesWhereTheWomiIsUsed);
				womiRefs.remove(womiId);
			}
		}
		womiRefs.put(destinationWomiId, modules);
		for (String moduleId : modules) {
			File epxmlFile = path.getModuleEpxml(getCollectionDescriptor(), moduleId);
			try {
				EpxmlParser p = new EpxmlParser(epxmlFile);
				p.rewriteWomisToAnother(womisToRewrite, destinationWomiId);
				try (OutputStream fos = new FileOutputStream(epxmlFile)) {
					p.overwriteEpxml(fos);
				}
			} catch (ParseException e) {
				throw new IllegalStateException("error parsing module " + moduleId + " in " + this, e);
			}
		}
	}

	private void downloadStaticAlternatives(long womiId) {
		logger.debug("downloading static alternative for " + womiId);
		IdAndVersion coll = getCollectionDescriptor();
		try {
			Set<Long> embeddedWomiIds = staticAlternativeManager.downloadAndProcess(womiId, path.getWomiDir(coll));
			logger.debug("static alt. found for WOMI #" + womiId + " with embedded WOMIs: " + embeddedWomiIds);
			Set<String> modulesWhereWomiIsUsed = womiRefs.get(womiId);
			for (long embeddedWomiId : embeddedWomiIds) {
				try {
					downloadInfoFiles(coll, embeddedWomiId);

					Set<String> modulesWhereEmbeddedWomiIsUsed = new HashSet<>(modulesWhereWomiIsUsed);
					if (womiRefs.containsKey(embeddedWomiId)) {
						modulesWhereEmbeddedWomiIsUsed.addAll(womiRefs.get(embeddedWomiId));
					}
					womiRefs.put(embeddedWomiId, modulesWhereEmbeddedWomiIsUsed);
				} catch (FileNotFoundException e) {
					womiNotFoundRefs.add(embeddedWomiId);
					logger.debug("when downloading and proprocessing WOMI #" + embeddedWomiId + " embedded in #"
							+ womiId + " in " + this + ": " + e);
				} catch (IOException e) {
					womiErrorRefs.add(embeddedWomiId);
					logger.debug("when downloading and proprocessing WOMI #" + embeddedWomiId + " embedded in #"
							+ womiId + " in " + this + ": " + e);
				}
			}
		} catch (IOException e) {
			logger.error("when processing static alts for womi #" + womiId, e);
		} catch (InvalidStaticAlternative e) {
			womiErrorRefs.add(womiId, e.getErrors());
		}
	}

	@Override
	public List<Task> getNext() {
		List<Task> list = new LinkedList<>();
		list.add(new SplitVariantsTask(getCollectionDescriptor(), reason, staticFormatsToGenerate));

		if (staticFormatsToGenerate.contains(ConversionFormat.PDF)) {
			DownloadStaticWomisTask task = new DownloadStaticWomisTask(getCollectionDescriptor(), reason,
					ConversionFormat.PDF, womiRefs.keySet());
			task.setStartTimeout(getStartTimeout());
			list.add(task);
		}
		if (staticFormatsToGenerate.contains(ConversionFormat.MOBILE)) {
			DownloadStaticWomisTask task = new DownloadStaticWomisTask(getCollectionDescriptor(), reason,
					ConversionFormat.MOBILE, womiRefs.keySet());
			task.setStartTimeout(getStartTimeout());
			list.add(task);
		}

		return list;
	}

	public static class WomiErrorRefs extends HashMap<Long, String> {

		private final String defaultErrorMessage;

		public WomiErrorRefs(String defaultErrorMessage) {
			this.defaultErrorMessage = defaultErrorMessage;
		}

		public void add(long womiId) {
			this.put(womiId, String.format(defaultErrorMessage, womiId));
		}

		public void add(long womiId, List<String> errors) {
			this.put(womiId, format(womiId, errors));
		}

		private String format(long womiId, List<String> errors) {
			return "Błędy w WOMI " + womiId + ": " + StringUtils.join(errors, "; ");
		}
	}

}
