package pl.epodr.sk.task;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.converter.MobileConverter;
import pl.epodr.sk.task.common.ConversionReason;

class MobileConversionTask extends StaticFormatConversionTask {

	@Autowired
	private MobileConverter mobileConverter;

	MobileConversionTask(IdAndVersion coll, String variant, ConversionReason reason, List<String> moduleIds,
			int numOfCollectionVariants) {
		super(coll, variant, reason, moduleIds, numOfCollectionVariants);
	}

	@Override
	public ConversionFormat getFormat() {
		return ConversionFormat.MOBILE;
	}

	@Override
	public void process() throws IOException, InterruptedException {
		if (variant.contains("student") && numOfCollectionVariants > 1) {
			logger.debug("skipping " + this);
			return;
		}

		IdAndVersion coll = getCollectionDescriptor();
		mobileConverter.convert(coll, variant, this);
		fileManager.publishCollectionStaticResults(coll, variant, getFormat().getExtension());
		modifyMetadataForPortalForMobilePacksSizes();
		messenger.notifyConversionFinished(coll, variant, getFormat(), reason);
	}

	private void modifyMetadataForPortalForMobilePacksSizes() throws IOException {
		IdAndVersion coll = getCollectionDescriptor();
		File file = path.getMetadataForPortal(coll);
		MetadataForPortal metadataForPortal = new MetadataForPortal(file);
		Map<Integer, Long> mobilePacksSizes = fileManager.getMobilePacksSizes(coll, variant);
		metadataForPortal.putMobilePacksSizes(variant, mobilePacksSizes);
		file.delete();
		FileUtils.moveFile(metadataForPortal.createFile(), file);
	}

}
