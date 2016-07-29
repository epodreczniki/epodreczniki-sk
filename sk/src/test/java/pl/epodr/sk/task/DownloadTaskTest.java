package pl.epodr.sk.task;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import pl.epodr.sk.IdAndVersion;

public class DownloadTaskTest {

	@Test
	public void testGetNextTimeout() {
		CollectionTransformationTask task = new DownloadTask(new IdAndVersion("1309", 1), null);

		int[] timeouts = CollectionTransformationTask.ATTEMPT_TIMEOUTS;
		for (int timeout : Arrays.copyOfRange(timeouts, 1, timeouts.length)) {
			assertEquals(timeout, (int) task.getNextTimeout());
			task.setStartTimeout(timeout);
		}

		assertEquals(null, task.getNextTimeout());
	}

	@Test
	public void testStartTimeout() throws InterruptedException {
		Task task = new DownloadTask(new IdAndVersion("1309", 1), null);
		task.setStartTimeout(5);
		assertEquals(false, task.isStartTimeoutExpired());
		assertEquals(true, task.getTaskAge() < 1);
		Thread.sleep(5100);
		assertEquals(5, task.getStartTimeout());
		assertEquals(true, task.isStartTimeoutExpired());
		assertEquals(true, task.getTaskAge() >= 5);
	}
}
