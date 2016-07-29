package pl.epodr.sk;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import pl.epodr.sk.common.SortedList;
import pl.epodr.sk.common.spring.AutowireManager;
import pl.epodr.sk.files.DownloadManager;
import pl.epodr.sk.files.FileManager;
import pl.epodr.sk.task.CollectionTask;
import pl.epodr.sk.task.ConversionFormat;
import pl.epodr.sk.task.ConversionTask;
import pl.epodr.sk.task.DeleteTask;
import pl.epodr.sk.task.DownloadStaticWomisTask;
import pl.epodr.sk.task.DownloadTask;
import pl.epodr.sk.task.Task;
import pl.epodr.sk.task.TaskComparator;
import pl.epodr.sk.task.TransformationUpdateTask;
import pl.epodr.sk.task.WomiModifiedTask;
import pl.epodr.sk.task.common.ConversionReason;

@Service
public class QueueManager {

	protected static final Logger logger = LoggerFactory.getLogger(QueueManager.class);

	protected final SortedList<Task> queue = new SortedList<>(new TaskComparator());

	protected final SortedList<Task> runningTasks = new SortedList<>(new TaskComparator());

	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	private AutowireManager autowireManager;

	@Autowired
	private DownloadManager downloadManager;

	@Autowired
	private FileManager fileManager;

	@Value("${numberOfThreads}")
	private Integer numberOfThreads;

	@Value("${numberOfThreadsReservedForHtml}")
	private Integer numberOfThreadsReservedForHtml;

	@PostConstruct
	protected void init() {
		if (numberOfThreadsReservedForHtml >= numberOfThreads) {
			logger.error("Wrong configuration: numberOfThreadsReservedForHtml must be less than numberOfThreads!");
		}
		loadLastSnapshot();
	}

	@Async("singleThreadExecutor")
	public void handleCollectionModified(IdAndVersion coll, ConversionReason reason) {
		handleCollectionModified(coll, 0, reason);
	}

	@Async("singleThreadExecutor")
	public void handleCollectionModified(IdAndVersion coll, int startTimeout, ConversionReason reason) {
		handleCollectionConversion(coll, startTimeout, reason);
	}

	@Async("singleThreadExecutor")
	public void handleCollectionMissing(IdAndVersion coll, ConversionReason reason) {
		if (this.hasAnyTaskOfCollection(coll)) {
			logger.info("ignoring collection.missing for {} - transformations in progress", coll);
		} else if (fileManager.hasErrors(coll)) {
			logger.info("ignoring collection.missing for {} - it has errors", coll);
		} else {
			handleCollectionConversion(coll, 0, reason);
		}
	}

	@Async("singleThreadExecutor")
	public void handleCollectionDependenciesModified(IdAndVersion coll, ConversionReason reason) {
		handleCollectionConversion(coll, 0, reason);
	}

	@Async("singleThreadExecutor")
	public void handleCollectionConversion(IdAndVersion coll, int startTimeout, ConversionReason reason) {
		removeCollectionTasksFromQueue(coll);
		DownloadTask task = new DownloadTask(coll, reason);
		task.setStartTimeout(startTimeout);
		addToQueue(task);
	}

	@Async("singleThreadExecutor")
	public void handleCollectionDeleted(IdAndVersion coll) {
		removeCollectionTasksFromQueue(coll);
		addToQueue(new DeleteTask(coll));
	}

	@Async("singleThreadExecutor")
	public void handleWomiModified(long womiId, ConversionReason reason) {
		runTaskNow(new WomiModifiedTask(womiId, reason));
	}

	@Async("singleThreadExecutor")
	public void handleTransformationUpdate() {
		synchronized (queue) {
			Iterator<Task> it = queue.iterator();
			while (it.hasNext()) {
				Task task = it.next();
				if (task instanceof TransformationUpdateTask) {
					it.remove();
				}
			}
		}
		addToQueue(new TransformationUpdateTask());
	}

	private void removeCollectionTasksFromQueue(IdAndVersion collectionDescriptor) {
		synchronized (queue) {
			Iterator<Task> it = queue.iterator();
			while (it.hasNext()) {
				Task task = it.next();
				if (task instanceof CollectionTask
						&& ((CollectionTask) task).getCollectionDescriptor().equals(collectionDescriptor)) {
					it.remove();
				}
			}
		}
		synchronized (runningTasks) {
			for (Task task : runningTasks) {
				if (task instanceof CollectionTask
						&& ((CollectionTask) task).getCollectionDescriptor().equals(collectionDescriptor)) {
					task.killTaskChain();
				}
			}
		}
	}

	private boolean hasAnyTaskOfCollection(IdAndVersion coll) {
		synchronized (queue) {
			for (Task task : queue) {
				if (task instanceof CollectionTask && ((CollectionTask) task).getCollectionDescriptor().equals(coll)) {
					return true;
				}
			}
		}
		synchronized (runningTasks) {
			for (Task task : runningTasks) {
				if (task instanceof CollectionTask && ((CollectionTask) task).getCollectionDescriptor().equals(coll)) {
					return true;
				}
			}
		}
		return false;
	}

	void addToQueue(Task task) {
		synchronized (queue) {
			queue.add(task);
		}
		snapshot();
		vivifyQueue();
	}

	private void addAllToQueue(List<Task> tasks) {
		synchronized (queue) {
			queue.addAll(tasks);
		}
		vivifyQueue();
	}

	@Scheduled(fixedRate = 60000)
	protected synchronized void vivifyQueue() {
		removeKilledTasksFromRunningButNotStarted();
		List<Task> tasks;
		synchronized (queue) {
			tasks = new LinkedList<>(queue);
		}
		for (Task task : tasks) {
			autowireManager.autowireFields(task);

			boolean startTimeoutExpired = !(task instanceof DownloadTask) || task.isStartTimeoutExpired();

			if (startTimeoutExpired && task.hasPreconditionsFulfilled()) {
				synchronized (queue) {
					if (queue.containsElement(task)) {
						queue.remove(task);
					} else {
						continue;
					}
				}
				synchronized (runningTasks) {
					runningTasks.add(task);
				}
				taskExecutor.execute(new TaskInRun(task, this));
			}
		}
	}

	private void runTaskNow(Task task) {
		try {
			logger.debug("starting now: " + task);
			autowireManager.autowireFields(task);
			task.process();
			logger.debug("finished now: " + task);
		} catch (Exception e) {
			logger.error("when processing now: " + task, e);
		}
	}

	private void removeKilledTasksFromRunningButNotStarted() {
		List<Task> tasks;
		synchronized (runningTasks) {
			tasks = new LinkedList<>(runningTasks);
		}
		for (Task task : tasks) {
			if (task.isKilled() && !task.isProcessingStarted()) {
				handleTaskFinished(task);
			}
		}
	}

	public void notifyTaskFinished(Task task) {
		handleTaskFinished(task);
		vivifyQueue();
	}

	private void handleTaskFinished(Task task) {
		synchronized (runningTasks) {
			runningTasks.remove(task);
		}
		snapshot();
	}

	private synchronized void snapshot() {
		List<Task> tasks = new LinkedList<>();
		synchronized (runningTasks) {
			tasks.addAll(runningTasks);
		}
		synchronized (queue) {
			tasks.addAll(queue);
		}
		SnapshotManager.getInstance().snapshot(tasks);
	}

	private synchronized void loadLastSnapshot() {
		List<Task> tasks = SnapshotManager.getInstance().load();
		if (tasks.size() > 0) {
			logger.info(tasks.size() + " tasks found - adding to queue");
			addAllToQueue(tasks);
		}
	}

	public boolean hasRunningTasksOfTheSameCollection(CollectionTask collectionTask) {
		synchronized (runningTasks) {
			for (Task task : runningTasks) {
				if (task instanceof CollectionTask
						&& ((CollectionTask) task).getCollectionDescriptor().equals(
								collectionTask.getCollectionDescriptor()) && !collectionTask.equals(task)) {
					return true;
				}
			}
		}
		return false;
	}

	public List<Task> getRunningTasks() {
		synchronized (runningTasks) {
			return Collections.unmodifiableList(new LinkedList<>(runningTasks));
		}
	}

	public List<Task> getQueue() {
		synchronized (queue) {
			return Collections.unmodifiableList(new LinkedList<>(queue));
		}
	}

	public Set<Task> getQueuedAndRunningTasks() {
		Set<Task> tasks = new HashSet<>();
		synchronized (runningTasks) {
			tasks.addAll(runningTasks);
		}
		synchronized (queue) {
			tasks.addAll(queue);
		}
		return tasks;
	}

	public int getNumberOfRunningPdfTasks() {
		int count = 0;
		synchronized (runningTasks) {
			for (Task task : runningTasks) {
				if (task instanceof ConversionTask && ((ConversionTask) task).getFormat() == ConversionFormat.PDF) {
					count++;
				}
			}
		}
		return count;
	}

	public boolean hasNoRunningTasks() {
		synchronized (runningTasks) {
			return runningTasks.isEmpty();
		}
	}

	public boolean canProcessAnotherStaticTaskNow() {
		if (hasTransformationUpdateTask()) {
			return false;
		}

		int staticTasksCount = 0;
		for (Task task : getRunningTasks()) {
			if (task instanceof DownloadStaticWomisTask
					|| (task instanceof ConversionTask && ((ConversionTask) task).getFormat().isStatic())) {
				staticTasksCount++;
			}
		}
		return staticTasksCount < getMaxNumberOfStaticTasks();
	}

	public boolean hasTransformationUpdateTask() {
		synchronized (queue) {
			for (Task task : queue) {
				if (task instanceof TransformationUpdateTask) {
					return true;
				}
			}
		}
		return false;
	}

	private int getMaxNumberOfStaticTasks() {
		return numberOfThreads - numberOfThreadsReservedForHtml;
	}

}
