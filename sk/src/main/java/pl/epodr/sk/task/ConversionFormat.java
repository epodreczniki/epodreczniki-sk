package pl.epodr.sk.task;

public enum ConversionFormat {

	EPXHTML, HTML, PDF, MOBILE;

	public String getExtension() {
		if (this == MOBILE) {
			return "zip";
		}
		return this.name().toLowerCase();
	}

	public boolean isStatic() {
		return this != EPXHTML && this != HTML;
	}

}
