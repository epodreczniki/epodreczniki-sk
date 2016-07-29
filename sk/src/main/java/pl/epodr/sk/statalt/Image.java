package pl.epodr.sk.statalt;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Cleanup;
import lombok.Data;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

@Data
class Image {

	private final long id;

	private final String filename;

	private final String author;

	private final String licence;

	public String getFilenameExtension() {
		return FilenameUtils.getExtension(filename).toLowerCase();
	}

	public String getFormatsXml(long womiId, Scope scope) {
		String mainFile = womiId + "-" + id + "-" + scope + "." + getFilenameExtension();
		return "<formats><format id=\"IMAGE_IMAGE_CLASSIC\" " + "mainFile=\"" + mainFile + "\"/></formats>";
	}

	public String getMetadata2Xml(long womiId, Scope scope) {
		try {
			@Cleanup
			InputStream is = getClass().getResourceAsStream("/templates/static-alternative-image-metadata2.xml");
			@Cleanup
			StringWriter writer = new StringWriter();
			IOUtils.copy(is, writer, "utf-8");
			String content = writer.toString();
			Map<String, Object> replacements = new HashMap<>();
			replacements.put("AUTHOR", author);
			replacements.put("LICENCE", licence);
			replacements.put("WOMI-ID", womiId);
			replacements.put("IMAGE-ID", id);
			replacements.put("SCOPE", scope);
			return replace(content, replacements);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private String replace(String string, Map<String, Object> replacements) {
		for (Entry<String, Object> entry : replacements.entrySet()) {
			string = string.replace("{{{" + entry.getKey() + "}}}", entry.getValue().toString());
		}
		return string;
	}

}
