package pl.epodr.sk.task.common;

import java.util.Date;

import lombok.Getter;

@Getter
public class ConversionReason {

	private final ReasonType type;

	private final Date date;

	public ConversionReason(ReasonType type) {
		this.type = type;
		this.date = new Date();
	}

	public long getTimestamp() {
		return date.getTime() / 1000;
	}

}
