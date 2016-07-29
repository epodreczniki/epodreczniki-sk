package pl.epodr.sk.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.converter.EpxmlPreprocessor;
import pl.epodr.sk.converter.SplitVariantsConverter;
import pl.epodr.sk.files.FileManager;
import pl.epodr.sk.parser.CollXmlParser;
import pl.epodr.sk.parser.CollXmlParser.ParseException;
import pl.epodr.sk.task.common.ConversionReason;

public class SplitVariantsTask extends CollectionTransformationTask {

	@Autowired
	private EpxmlPreprocessor epxmlPreprocessor;

	@Autowired
	private SplitVariantsConverter splitVariantsConverter;

	private final Map<String, List<String>> modulesPerVariant = new HashMap<>();

	private final List<ConversionFormat> formatsToGenerate;

	public SplitVariantsTask(IdAndVersion collectionDescriptor, ConversionReason reason,
			List<ConversionFormat> formatsToGenerate) {
		super(collectionDescriptor, reason);
		this.formatsToGenerate = formatsToGenerate;
	}

	@Override
	public void process() throws InterruptedException {
		IdAndVersion coll = getCollectionDescriptor();
		epxmlPreprocessor.preprocess(coll, this);
		splitVariantsConverter.convert(coll, this);

		File workingDir = path.getWorkingDirectory(coll);

		MetadataForPortal metadataForPortal = new MetadataForPortal(coll, formatsToGenerate, git);

		for (String variant : FileManager.VARIANTS) {
			try {
				File variantDir = new File(workingDir, variant);
				if (variantDir.exists() && variantDir.isDirectory()) {
					File colxmlFile = path.getColxml(coll, variant);
					List<String> moduleIds;
					try {
						moduleIds = new CollXmlParser(colxmlFile).getModuleIds();
					} catch (ParseException e) {
						throw new IllegalStateException("error parsing collxml in " + this + " from " + colxmlFile, e);
					}
					modulesPerVariant.put(variant, moduleIds);
					metadataForPortal.addVariant(variant);
					for (String moduleId : moduleIds) {
						fileManager.publishEpxml(coll, variant, moduleId);
					}
				}
			} catch (IOException e) {
				logger.error("when checking modules per variant of " + coll + " - " + variant, e);
			}
		}

		try {
			File file = path.getMetadataForPortal(coll);
			file.delete();
			FileUtils.moveFile(metadataForPortal.createFile(), file);
			messenger.notifyMetadataChanged(coll);
		} catch (IOException e) {
			logger.error("when creating metadataForPortal in " + this, e);
		}

		if (modulesPerVariant.size() == 0) {
			logger.error("no variants found for collection " + coll);
		} else {
			logger.debug(modulesPerVariant.size() + " variant(s) found");
		}
	}

	@Override
	public List<Task> getNext() {
		List<Task> list = new ArrayList<>();
		for (Entry<String, List<String>> entry : modulesPerVariant.entrySet()) {
			list.add(ConversionTask.create(getCollectionDescriptor(), entry.getKey(), ConversionFormat.EPXHTML, reason,
					entry.getValue(), formatsToGenerate, modulesPerVariant.size()));
		}
		return list;
	}

}
