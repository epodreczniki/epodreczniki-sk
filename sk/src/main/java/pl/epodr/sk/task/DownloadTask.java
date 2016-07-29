package pl.epodr.sk.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.Configuration;
import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.QueueManager;
import pl.epodr.sk.files.FileManager.ColxmlDownloadResult;
import pl.epodr.sk.parser.CollXmlParser;
import pl.epodr.sk.parser.EpxmlParser;
import pl.epodr.sk.task.common.ConversionReason;
import pl.epodr.sk.task.notifications.CollxmlParsingError;
import pl.epodr.sk.task.notifications.EpxmlParsingError;
import pl.epodr.sk.task.notifications.InvalidModuleIdsError;
import pl.epodr.sk.task.notifications.ModuleNotFoundError;
import pl.epodr.sk.task.notifications.NoModulesError;
import pl.epodr.sk.task.notifications.NonUniqueElementsIdsError;

public class DownloadTask extends CollectionTransformationTask {

	@Autowired(required = false)
	private QueueManager queueManager;

	@Autowired
	private Configuration configuration;

	@Getter
	private final Map<Long, Set<String>> womiRefs = new HashMap<>();

	private List<ConversionFormat> formatsToGenerate;

	private CollXmlParser collxml;

	public DownloadTask(IdAndVersion collectionDescriptor, ConversionReason reason) {
		super(collectionDescriptor, reason);
	}

	@Override
	public void process() throws IOException {
		this.formatsToGenerate = configuration.getStaticFormatsToGenerate();

		try {
			fileManager.deleteWorkingDirectory(getCollectionDescriptor());
			saveCollxmlAndEpxmls();
		} catch (IOException e) {
			runNextAttemptOrStop(e);
		}
	}

	private void saveCollxmlAndEpxmls() throws IOException {
		IdAndVersion coll = getCollectionDescriptor();
		logger.debug("saving colxml " + coll);
		File colxmlFile;
		try {
			ColxmlDownloadResult r = fileManager.saveColxml(coll);
			colxmlFile = r.getColxmlFile();
			if (r.isInEditionOnline()) {
				logger.debug("collection " + coll + " is in EO - skipping static formats");
				formatsToGenerate.clear();
			}
		} catch (FileNotFoundException e) {
			logger.info("collection " + coll + " no longer exists");
			fileManager.deleteOutputDirectory(coll);
			indexDatabase.removeCollection(coll);
			indexDatabase.commit();
			this.killTaskChain();
			return;
		}

		try {
			collxml = new CollXmlParser(colxmlFile);
			verifyFormatsToGenerate(collxml);

			List<IdAndVersion> modules = collxml.getModules();
			if (modules.size() == 0) {
				logger.info("collection " + coll + " has no modules");
				stopWithTransformationError(new NoModulesError());
				return;
			}

			boolean isEarlyEducation = collxml.isEarlyEducation();
			boolean skipIdUniquenessErrors = !"standard-2-uwr".equals(collxml.getStylesheet());
			Map<String, String> elementIdAndModuleId = new HashMap<>();
			for (IdAndVersion module : modules) {
				if (this.isKilled()) {
					return;
				}

				logger.debug("saving module " + module);
				File epxmlFile;
				try {
					epxmlFile = fileManager.saveEpxml(module, coll);
				} catch (FileNotFoundException e) {
					logger.info("cannot process collection " + coll + " - module " + module + " no longer exists");
					stopWithTransformationError(new ModuleNotFoundError(module));
					return;
				}

				try {
					EpxmlParser p = new EpxmlParser(epxmlFile);
					if (!p.hasValidModuleIds(module.getId())) {
						logger.info("error in collection " + coll + " - invalid module identifiers in epXML of "
								+ module);
						stopWithTransformationError(new InvalidModuleIdsError(module));
						return;
					}

					if (!isEarlyEducation) {
						List<String> elementIds = p.getAllElementIds();
						for (String elementId : elementIds) {
							if (elementIdAndModuleId.containsKey(elementId)) {
								String otherModuleId = elementIdAndModuleId.get(elementId);
								logger.info("errors in collection " + coll + " - element id '" + elementId
										+ "' is duplicated in modules " + otherModuleId + " and " + module.getId());

								if (!skipIdUniquenessErrors) {
									stopWithTransformationError(new NonUniqueElementsIdsError(elementId, otherModuleId,
											module.getId()));
									return;
								}
							} else {
								elementIdAndModuleId.put(elementId, module.getId());
							}
						}
					}

					for (long womiId : p.getWomiReferences()) {
						assignModuleToWomi(womiId, module.getId());
					}
				} catch (EpxmlParser.ParseException e) {
					logger.warn("error parsing epxml of " + module + " in " + this, e);
					stopWithTransformationError(new EpxmlParsingError(module));
					return;
				}
			}
		} catch (CollXmlParser.ParseException e) {
			logger.warn("error parsing collxml in " + this, e);
			stopWithTransformationError(new CollxmlParsingError());
			return;
		}
	}

	private void verifyFormatsToGenerate(CollXmlParser collxml) {
		if (CollXmlParser.isDummy(getCollectionDescriptor())) {
			formatsToGenerate.clear();
		} else if (collxml.isEarlyEducation()) {
			logger.debug("collection " + getCollectionDescriptor() + " is early education - skipping static formats");
			formatsToGenerate.clear();
		}
	}

	private void assignModuleToWomi(long womiId, String moduleId) {
		if (!womiRefs.containsKey(womiId)) {
			womiRefs.put(womiId, new HashSet<String>());
		}
		womiRefs.get(womiId).add(moduleId);
	}

	@Override
	public List<Task> getNext() {
		List<Task> list = new LinkedList<>();
		DownloadWomiTask downloadWomiTask = new DownloadWomiTask(getCollectionDescriptor(), reason, collxml,
				formatsToGenerate, womiRefs);
		downloadWomiTask.setStartTimeout(getStartTimeout());
		list.add(downloadWomiTask);
		return list;
	}

	@Override
	public boolean hasPreconditionsFulfilled() {
		return !queueManager.hasRunningTasksOfTheSameCollection(this);
	}

	@Override
	public String toString() {
		String timeoutInfo = "";
		int startTimeout = this.getStartTimeout();
		if (startTimeout > 0) {
			timeoutInfo = ", timeout=" + startTimeout;
			int readyIn = startTimeout - this.getTaskAge();
			if (readyIn > 0) {
				timeoutInfo += ", readyIn=" + readyIn;
			}
		}
		return this.getClass().getSimpleName() + "(" + getCollectionDescriptor() + timeoutInfo + ")";
	}

	public CollXmlParser getCollxmlParser() {
		return collxml;
	}

}
