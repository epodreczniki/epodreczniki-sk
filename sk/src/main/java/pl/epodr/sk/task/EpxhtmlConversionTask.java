package pl.epodr.sk.task;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.converter.EpcollxmlConverter;
import pl.epodr.sk.converter.EpxhtmlConverter;
import pl.epodr.sk.converter.MissingReferencePythonException;
import pl.epodr.sk.task.common.ConversionReason;
import pl.epodr.sk.task.notifications.MissingReferenceError;

class EpxhtmlConversionTask extends ConversionTask {

	private static final String MESSENGER_XML_FORMAT_VALUE = "xml";

	@Autowired
	private EpcollxmlConverter epcollxmlConverter;

	@Autowired
	private EpxhtmlConverter epxhtmlConverter;

	private final List<ConversionFormat> staticFormatsToGenerate;

	EpxhtmlConversionTask(IdAndVersion coll, String variant, ConversionReason reason, List<String> moduleIds,
			List<ConversionFormat> staticFormatsToGenerate, int numOfCollectionVariants) {
		super(coll, variant, reason, moduleIds, numOfCollectionVariants);
		this.staticFormatsToGenerate = staticFormatsToGenerate;
	}

	@Override
	public ConversionFormat getFormat() {
		return ConversionFormat.EPXHTML;
	}

	@Override
	public void process() throws IOException, InterruptedException {
		IdAndVersion coll = getCollectionDescriptor();
		try {
			epcollxmlConverter.convert(coll, variant, this);
			try {
				moduleIds = fileManager.publishColxml(coll, variant);
				messenger.notifyConversionFinished(coll, variant, MESSENGER_XML_FORMAT_VALUE, reason);
			} catch (IOException e) {
				logger.error("when publishing colxml for " + coll + " " + variant, e);
			}
			epxhtmlConverter.convert(coll, variant, this);
		} catch (MissingReferencePythonException e) {
			logger.warn("collection " + coll + " has missing references");
			stopWithTransformationError(new MissingReferenceError(e.getMissingReferences()));
			return;
		}
	}

	@Override
	public List<Task> getNext() {
		List<Task> list = new LinkedList<>();

		list.add(ConversionTask.create(getCollectionDescriptor(), variant, ConversionFormat.HTML, reason, moduleIds,
				staticFormatsToGenerate, numOfCollectionVariants));

		for (ConversionFormat format : staticFormatsToGenerate) {
			list.add(ConversionTask.create(getCollectionDescriptor(), variant, format, reason, moduleIds,
					staticFormatsToGenerate, numOfCollectionVariants));
		}

		return list;
	}

	@Override
	public boolean hasPreconditionsFulfilled() {
		for (Task task : queueManager.getQueuedAndRunningTasks()) {
			if (task instanceof DownloadWomiTask
					&& ((DownloadWomiTask) task).getCollectionDescriptor().equals(this.getCollectionDescriptor())) {
				return false;
			}
		}
		return true;
	}
}
