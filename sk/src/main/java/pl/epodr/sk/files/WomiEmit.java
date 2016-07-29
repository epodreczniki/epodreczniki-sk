package pl.epodr.sk.files;

import lombok.Data;

@Data
public class WomiEmit {

	private final byte[] content;

	private final String contentType;

	public String getExtension() {
		if (contentType == null) {
			throw new IllegalStateException("not supported image WOMI content type: " + contentType);
		}
		if (contentType.contains("svg")) {
			return FileExtensionsNormalizer.EXTENSION_SVG;
		}
		if (contentType.contains("jpeg") || contentType.contains("jpg")) {
			return FileExtensionsNormalizer.EXTENSION_JPEG;
		}
		if (contentType.contains("png")) {
			return FileExtensionsNormalizer.EXTENSION_PNG;
		}
		if (contentType.contains("tiff")) {
			return FileExtensionsNormalizer.EXTENSION_TIFF;
		}
		if (contentType.contains("gif")) {
			return FileExtensionsNormalizer.EXTENSION_GIF;
		}
		throw new IllegalStateException("not supported image WOMI content type: " + contentType);
	}

}
