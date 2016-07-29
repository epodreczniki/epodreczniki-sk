package pl.epodr.sk.task.common;

public enum ReasonType {
	COLLECTION_ADDED("collection.added"), COLLECTION_MODIFIED("collection.modified"), COLLECTION_DEPENDENCIES_MODIFIED(
			"collection.dependencies-modified"), COLLECTION_MISSING("collection.missing"), WOMI_MODIFIED(
			"womi.modified"), WOMI_DELETED("womi.deleted"), ADMIN("admin"), UNKNOWN("unknown");

	public static ReasonType fromRawString(String rawString) {
		for (ReasonType type : values()) {
			if (type.rawString.equals(rawString)) {
				return type;
			}
		}
		return UNKNOWN;
	}

	private String rawString;

	private ReasonType(String rawString) {
		this.rawString = rawString;
	}

	public String toRawString() {
		return rawString;
	}

}
