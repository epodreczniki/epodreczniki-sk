package pl.epodr.sk.task;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.QueueManager;
import pl.epodr.sk.files.FileManager;
import pl.epodr.sk.womi.IndexDatabase;

public class DeleteTask extends CollectionTask {

	@Autowired
	private QueueManager queueManager;

	@Autowired
	private IndexDatabase indexDatabase;

	@Autowired
	private FileManager fileManager;

	public DeleteTask(IdAndVersion collectionDescriptor) {
		super(collectionDescriptor);
	}

	@Override
	public void process() {
		IdAndVersion collectionDescriptor = getCollectionDescriptor();
		try {
			indexDatabase.removeCollection(collectionDescriptor);
			indexDatabase.commit();
			fileManager.deleteWorkingDirectory(collectionDescriptor);
			fileManager.deleteOutputDirectory(collectionDescriptor);
		} catch (IOException e) {
			logger.error("when deleting in " + this, e);
		}
	}

	@Override
	public List<Task> getNext() {
		return null;
	}

	@Override
	public boolean hasPreconditionsFulfilled() {
		return !queueManager.hasRunningTasksOfTheSameCollection(this);
	}

	@Override
	public Class<? extends Task> getSerializeTo() {
		return this.getClass();
	}

}
