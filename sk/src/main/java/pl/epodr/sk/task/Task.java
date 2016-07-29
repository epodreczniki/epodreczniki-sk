package pl.epodr.sk.task;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public abstract class Task implements Killable {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final Date timestamp = new Date();

	private int startTimeout = 0;

	private Date timeoutExpirationMoment = null;

	private boolean killed = false;

	private boolean processingStarted = false;

	public abstract void process() throws IOException, InterruptedException;

	public abstract List<Task> getNext();

	public abstract Class<? extends Task> getSerializeTo();

	public void killTaskChain() {
		logger.debug("killing " + this);
		this.killed = true;
	}

	public boolean hasPreconditionsFulfilled() {
		return true;
	}

	public void setStartTimeout(int startTimeout) {
		this.startTimeout = startTimeout;
		Calendar cal = Calendar.getInstance();
		cal.setTime(timestamp);
		cal.add(Calendar.SECOND, startTimeout);
		this.timeoutExpirationMoment = cal.getTime();
	}

	public int getStartTimeout() {
		return startTimeout;
	}

	public boolean isStartTimeoutExpired() {
		if (timeoutExpirationMoment == null) {
			return true;
		}

		Date now = new Date();
		return timeoutExpirationMoment.before(now) || timeoutExpirationMoment.equals(now);
	}

	public int getTaskAge() {
		Date now = new Date();
		long ms = now.getTime() - timestamp.getTime();
		return (int) (ms / 1000);
	}

	public void setProcessingStarted() {
		this.processingStarted = true;
	}

}
