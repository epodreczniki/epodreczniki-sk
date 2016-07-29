package pl.epodr.sk.task;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.QueueManager;
import pl.epodr.sk.common.Git;
import pl.epodr.sk.files.FileManager;
import pl.epodr.sk.files.FilePathManager;
import pl.epodr.sk.messaging.Messenger;
import pl.epodr.sk.task.common.ConversionReason;
import pl.epodr.sk.task.notifications.CollectionNotification;
import pl.epodr.sk.womi.IndexDatabase;

public abstract class CollectionTransformationTask extends CollectionTask {

	protected static final int[] ATTEMPT_TIMEOUTS = new int[] { 0, 120, 1800, 14400 };

	@Autowired(required = false)
	private QueueManager queueManager;

	protected final ConversionReason reason;

	@Autowired
	protected FileManager fileManager;

	@Autowired
	protected FilePathManager path;

	@Autowired
	protected IndexDatabase indexDatabase;

	@Autowired(required = false)
	protected Messenger messenger;

	@Autowired
	protected Git git;

	protected CollectionTransformationTask(IdAndVersion collectionDescriptor, ConversionReason reason) {
		super(collectionDescriptor);
		this.reason = reason;
	}

	@Override
	public Class<? extends Task> getSerializeTo() {
		return DownloadTask.class;
	}

	protected void runNextAttemptOrStop(Exception e) {
		this.killTaskChain();

		Integer timeout = getNextTimeout();
		if (timeout != null) {
			logger.warn("requeueing the task chain of " + this + " because of I/O error (" + e.toString() + ")");
			queueManager.handleCollectionModified(getCollectionDescriptor(), timeout, reason);
		} else {
			logger.error(this + " has permanently failed (" + e.toString() + ")");
		}
	}

	protected Integer getNextTimeout() {
		int attempt = 0;
		for (; attempt < ATTEMPT_TIMEOUTS.length; attempt++) {
			if (getStartTimeout() < ATTEMPT_TIMEOUTS[attempt]) {
				break;
			}
		}
		Integer timeout = null;
		if (attempt < ATTEMPT_TIMEOUTS.length) {
			timeout = ATTEMPT_TIMEOUTS[attempt];
		}
		return timeout;
	}

	public void stopWithTransformationError(CollectionNotification error) {
		IdAndVersion coll = getCollectionDescriptor();
		logger.warn("setting " + error + " to " + coll);

		indexDatabase.removeCollection(coll);
		indexDatabase.commit();

		try {
			fileManager.deleteOutputDirectory(coll);
		} catch (IOException e) {
			logger.error("when deleting output directory in " + this, e);
		}

		try {
			MetadataForPortal metadataForPortal = new MetadataForPortal(coll, new ArrayList<ConversionFormat>(), git);
			metadataForPortal.addVariant(FileManager.VARIANTS[0]);
			metadataForPortal.addNotification(error);
			metadataForPortal.save(path.getMetadataForPortal(coll));
			messenger.notifyMetadataChangedAndEmissionFormatsDeleted(coll, this.reason);
		} catch (IOException e) {
			logger.error("when creating metadataForPortal in " + this, e);
		}

		this.killTaskChain();
	}

}
