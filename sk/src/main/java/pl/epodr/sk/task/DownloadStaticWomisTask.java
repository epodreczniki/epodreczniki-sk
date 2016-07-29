package pl.epodr.sk.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.QueueManager;
import pl.epodr.sk.files.DownloadManager;
import pl.epodr.sk.parser.CollXmlParser;
import pl.epodr.sk.parser.CollXmlParser.ParseException;
import pl.epodr.sk.task.common.ConversionReason;

public class DownloadStaticWomisTask extends CollectionTransformationTask {

	@Autowired
	private DownloadManager downloadManager;

	private final ConversionFormat format;

	private final Set<Long> womiIds;

	private final IdAndVersion coll;

	@Autowired(required = false)
	private QueueManager queueManager;

	public DownloadStaticWomisTask(IdAndVersion collectionDescriptor, ConversionReason reason, ConversionFormat format,
			Set<Long> womiIds) {
		super(collectionDescriptor, reason);
		this.format = format;
		this.womiIds = new HashSet<>(womiIds);
		this.coll = getCollectionDescriptor();
	}

	@Override
	public void process() {
		File colxmlFile = path.getColxml(coll);
		if (!colxmlFile.exists()) {
			logger.error("killing " + this + " - collxml does not exist: " + colxmlFile);
			this.killTaskChain();
			return;
		}

		Long coverId = null;
		try {
			CollXmlParser parser = new CollXmlParser(colxmlFile);
			coverId = parser.getCoverId();
		} catch (IOException | ParseException e) {
			throw new IllegalStateException(e);
		}

		if (coverId != null) {
			try {
				fileManager.downloadAndSaveCovers(coverId, coll);
			} catch (IOException e) {
				logger.warn("error saving cover #" + coverId + " in " + this, e);
			}
		}

		if (format == ConversionFormat.MOBILE) {
			fileManager.createMobileImagesDirectories(coll);
		}

		for (long womiId : womiIds) {
			if (this.isKilled()) {
				throw new TaskKilledException();
			}

			try {
				downloadImages(womiId);
			} catch (FileNotFoundException e) {
				logger.debug("images not found for WOMI #" + womiId);
			} catch (IOException | ParserConfigurationException | SAXException | JSONException e) {
				logger.warn("error downloading images #" + womiId + ": " + e);
				handleWomiError(womiId);
			}
		}
	}

	private void handleWomiError(long womiId) {
		long errorWomiId = downloadManager.getErrorWomiId();
		try {
			downloadImages(errorWomiId, womiId);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			if (e.getCause() instanceof SocketTimeoutException) {
				runNextAttemptOrStop(e);
				throw new TaskKilledException();
			} else {
				logger.warn("error downloading images #" + errorWomiId + " because of ealier error for #" + womiId
						+ " in " + this, e);
			}
		}
	}

	private void downloadImages(long womiId) throws IOException, ParserConfigurationException, SAXException {
		downloadImages(womiId, womiId);
	}

	private void downloadImages(long womiId, long destinationWomiId) throws IOException, ParserConfigurationException,
			SAXException {
		switch (format) {
			case PDF:
				fileManager.downloadPdfImages(womiId, coll, destinationWomiId);
				break;
			case MOBILE:
				fileManager.downloadWomiForMobile(womiId, coll, destinationWomiId);
				break;
			default:
				throw new IllegalStateException("unsupported format: " + this);
		}
	}

	@Override
	public List<Task> getNext() {
		return null;
	}

	public ConversionFormat getFormat() {
		return format;
	}

	@Override
	public String toString() {
		return String.format("%s(%s, %s)", this.getClass().getSimpleName(), coll, format);
	}

	@Override
	public boolean hasPreconditionsFulfilled() {
		return queueManager.canProcessAnotherStaticTaskNow();
	}

}
