package pl.epodr.sk.task;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.QueueManager;
import pl.epodr.sk.task.common.ConversionReason;

public abstract class ConversionTask extends CollectionTransformationTask {

	public static ConversionTask create(IdAndVersion coll, String variant, ConversionFormat format,
			ConversionReason reason, List<String> moduleIds, List<ConversionFormat> staticFormatsToGenerate,
			int numOfCollectionVariants) {
		switch (format) {
			case EPXHTML:
				return new EpxhtmlConversionTask(coll, variant, reason, moduleIds, staticFormatsToGenerate,
						numOfCollectionVariants);
			case HTML:
				return new HtmlConversionTask(coll, variant, reason, moduleIds, numOfCollectionVariants);
			default:
				return StaticFormatConversionTask.create(coll, variant, format, reason, moduleIds,
						numOfCollectionVariants);
		}
	}

	@Autowired
	protected QueueManager queueManager;

	protected final String variant;

	protected List<String> moduleIds;

	protected final int numOfCollectionVariants;

	protected ConversionTask(IdAndVersion collectionDescriptor, String variant, ConversionReason reason,
			List<String> moduleIds, int numOfCollectionVariants) {
		super(collectionDescriptor, reason);
		this.variant = variant;
		this.moduleIds = moduleIds;
		this.numOfCollectionVariants = numOfCollectionVariants;
	}

	public abstract ConversionFormat getFormat();

	@Override
	public List<Task> getNext() {
		return null;
	}

	@Override
	public String toString() {
		return String.format("ConversionTask(%s, %s, %s)", getCollectionDescriptor(), variant, getFormat());
	}

}
