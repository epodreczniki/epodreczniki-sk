package pl.epodr.sk;

import lombok.Data;

@Data
public class IdAndVersion {

	private final String id;

	private final long version;

	@Override
	public String toString() {
		return String.format("%s(%d)", id, version);
	}
}
