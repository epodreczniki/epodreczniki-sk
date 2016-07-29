package pl.epodr.sk;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.epodr.sk.converter.PythonRunner.PythonScriptException;
import pl.epodr.sk.task.CollectionTransformationTask;
import pl.epodr.sk.task.Task;
import pl.epodr.sk.task.TaskKilledException;
import pl.epodr.sk.task.notifications.UnknownError;

public class TaskInRun implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(TaskInRun.class);

	private final QueueManager queueManager;

	private final Task task;

	public TaskInRun(Task task, QueueManager queueManager) {
		this.task = task;
		this.queueManager = queueManager;
	}

	@Override
	public void run() {
		try {
			if (task.isKilled()) {
				return;
			}
			processTaskAndQueueNext(task);
		} catch (Exception e) {
			if (e instanceof PythonScriptException) {
				logger.error("when processing " + task + "\n" + e);
			} else {
				logger.error("when processing " + task, e);
			}
			if (task instanceof CollectionTransformationTask) {
				((CollectionTransformationTask) task).stopWithTransformationError(new UnknownError());
			}
		} finally {
			queueManager.notifyTaskFinished(task);
		}
	}

	private void processTaskAndQueueNext(Task task) throws IOException, InterruptedException {
		logger.debug("starting: " + task);
		task.setProcessingStarted();
		try {
			task.process();
			logger.debug("finished: " + task);
			if (!task.isKilled()) {
				List<Task> next = task.getNext();
				if (next != null) {
					for (Task child : next) {
						queueManager.addToQueue(child);
					}
				}
			}
		} catch (TaskKilledException e) {
			logger.debug("interrupted: " + task);
		}
	}

	public Task getTask() {
		return task;
	}

}
