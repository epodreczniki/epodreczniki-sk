package pl.epodr.sk.messaging;

import lombok.Getter;

import org.apache.commons.lang.StringUtils;

@Getter
class RoutingKey {

	private final String sender;

	private final String request;

	public RoutingKey(String source) {
		try {
			int pos = StringUtils.ordinalIndexOf(source, ".", 2);
			this.sender = source.substring(0, pos);
			this.request = source.substring(pos + 1);
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("invalid routing key: " + source);
		}
	}

}