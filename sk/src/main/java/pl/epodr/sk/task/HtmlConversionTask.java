package pl.epodr.sk.task;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.converter.HtmlConverter;
import pl.epodr.sk.task.common.ConversionReason;

class HtmlConversionTask extends ConversionTask {

	@Autowired
	private HtmlConverter htmlConverter;

	HtmlConversionTask(IdAndVersion coll, String variant, ConversionReason reason, List<String> moduleIds,
			int numOfCollectionVariants) {
		super(coll, variant, reason, moduleIds, numOfCollectionVariants);
	}

	@Override
	public ConversionFormat getFormat() {
		return ConversionFormat.HTML;
	}

	@Override
	public void process() throws IOException, InterruptedException {
		IdAndVersion coll = getCollectionDescriptor();

		putMathmlAltTextsInWorkingDir();
		htmlConverter.convert(coll, variant, this);

		fileManager.publishCollectionHtmlResults(coll, variant, moduleIds);
		messenger.notifyConversionFinished(coll, variant, getFormat(), reason);
	}

	@Override
	public boolean hasPreconditionsFulfilled() {
		return true;
	}

	private void putMathmlAltTextsInWorkingDir() throws IOException {
		File sourceAltTextFile = path.getSourceMathmlAltTextFile(getCollectionDescriptor());
		if (sourceAltTextFile.exists()) {
			File collectionAltTextFile = path.getDestinationMathmlAltTextFile(getCollectionDescriptor(), variant);
			logger.debug(String.format("copying %s to %s", sourceAltTextFile, collectionAltTextFile));
			FileUtils.copyFile(sourceAltTextFile, collectionAltTextFile);
		}
	}
}
