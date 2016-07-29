package pl.epodr.sk.common.spring;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import pl.epodr.sk.TaskInRun;
import pl.epodr.sk.task.Task;
import pl.epodr.sk.task.TaskComparator;

public class ThreadPoolPriorityTaskExecutor extends ThreadPoolTaskExecutor {

	private static final int MAX_QUEUE_CAPACITY = 99999;

	private Comparator<Runnable> comparator;

	public ThreadPoolPriorityTaskExecutor() {
		comparator = new Comparator<Runnable>() {

			TaskComparator comparator = new TaskComparator();

			@Override
			public int compare(Runnable o1, Runnable o2) {
				if (o1 == null || !(o1 instanceof TaskInRun)) {
					return -1;
				}
				if (o2 == null || !(o2 instanceof TaskInRun)) {
					return 1;
				}
				Task t1 = ((TaskInRun) o1).getTask();
				Task t2 = ((TaskInRun) o2).getTask();
				return comparator.compare(t1, t2);
			}
		};
	}

	@Override
	protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
		if (queueCapacity <= 0) {
			queueCapacity = MAX_QUEUE_CAPACITY;
		} else if (queueCapacity > MAX_QUEUE_CAPACITY) {
			queueCapacity = MAX_QUEUE_CAPACITY;
		}
		return new PriorityBlockingQueue<>(queueCapacity, comparator);
	}

}
