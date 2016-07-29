package pl.epodr.sk.task;

import java.util.List;
import java.util.Set;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.QueueManager;
import pl.epodr.sk.task.common.ConversionReason;
import pl.epodr.sk.womi.IndexDatabase;

@Getter
public class WomiModifiedTask extends Task {

	private static final int SECONDS_TO_WAIT_BEFORE_PROCESSING_COLLS = 300;

	private final long womiId;

	@Autowired
	private IndexDatabase indexDatabase;

	@Autowired
	private QueueManager queueManager;

	private final ConversionReason reason;

	public WomiModifiedTask(long womiId, ConversionReason reason) {
		this.womiId = womiId;
		this.reason = reason;
	}

	@Override
	public void process() {
		final Set<IdAndVersion> collectionsForWomi = indexDatabase.getCollectionsForWomi(womiId);
		logger.debug("index for womi #" + womiId + ": collections(" + collectionsForWomi + ")");
		for (final IdAndVersion coll : collectionsForWomi) {
			int collectionTaskStartTimeout = SECONDS_TO_WAIT_BEFORE_PROCESSING_COLLS - this.getTaskAge();
			queueManager.handleCollectionConversion(coll, collectionTaskStartTimeout, reason);
		}
	}

	@Override
	public List<Task> getNext() {
		return null;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + womiId + ")";
	}

	@Override
	public Class<? extends Task> getSerializeTo() {
		return this.getClass();
	}

}
