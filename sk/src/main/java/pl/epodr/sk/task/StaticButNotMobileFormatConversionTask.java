package pl.epodr.sk.task;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.converter.PdfConverter;
import pl.epodr.sk.task.common.ConversionReason;

class StaticButNotMobileFormatConversionTask extends StaticFormatConversionTask {

	@Autowired
	private PdfConverter pdfConverter;

	private final ConversionFormat format;

	StaticButNotMobileFormatConversionTask(IdAndVersion coll, String variant, ConversionFormat format,
			ConversionReason reason, List<String> moduleIds, int numOfCollectionVariants) {
		super(coll, variant, reason, moduleIds, numOfCollectionVariants);
		this.format = format;

		if (format != ConversionFormat.PDF) {
			throw new IllegalArgumentException("unsupported conversion format: " + format);
		}
	}

	@Override
	public ConversionFormat getFormat() {
		return format;
	}

	@Override
	public void process() throws IOException, InterruptedException {
		IdAndVersion coll = getCollectionDescriptor();

		switch (format) {
			case PDF:
				pdfConverter.convert(coll, variant, this);
				break;
			default:
				throw new IllegalArgumentException("unsupported conversion format: " + format);
		}

		fileManager.publishCollectionStaticResults(coll, variant, format.getExtension());
		messenger.notifyConversionFinished(coll, variant, format, reason);
	}

	@Override
	public boolean hasPreconditionsFulfilled() {
		if (getFormat() == ConversionFormat.PDF) {
			if (queueManager.getNumberOfRunningPdfTasks() >= 2) {
				return false;
			}
		}
		return super.hasPreconditionsFulfilled();
	}

}
