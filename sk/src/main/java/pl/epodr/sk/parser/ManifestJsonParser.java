package pl.epodr.sk.parser;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ManifestJsonParser {

	private static final String ADVANCED_STATIC_ALTERNATIVE_ELEMENT = "advancedStaticAlternative";

	private static final String PARAMETERS_ELEMENT = "parameters";

	private static final String ENGINE_ELEMENT = "engine";

	private static final String WOMI_IDS_ELEMENT = "womiIds";

	private final JSONObject json;

	public ManifestJsonParser(String content) throws JSONException {
		this.json = new JSONObject(content);
	}

	public ManifestJsonParser(File file) throws JSONException, IOException {
		this(FileUtils.readFileToString(file));
	}

	public String getEngine() throws JSONException {
		if (json.has(ENGINE_ELEMENT)) {
			return (String) json.get(ENGINE_ELEMENT);
		}
		return null;
	}

	public List<Long> getEmbeddedWomis() {
		List<Long> list = new LinkedList<>();
		if (!json.has(WOMI_IDS_ELEMENT)) {
			return list;
		}
		JSONArray arr = json.getJSONArray(WOMI_IDS_ELEMENT);
		for (int i = 0; i < arr.length(); i++) {
			list.add(arr.getLong(i));
		}
		return list;
	}

	public boolean hasAdvancedStaticAlternative() {
		if (json.has(PARAMETERS_ELEMENT)) {
			JSONObject params = json.getJSONObject(PARAMETERS_ELEMENT);
			return params.has(ADVANCED_STATIC_ALTERNATIVE_ELEMENT);
		}
		return false;
	}

	public boolean isWomiAudio() {
		return Engine.AUDIO.getValue().equals(getEngine());
	}

	public boolean isWomiIcon() {
		return Engine.ICON.getValue().equals(getEngine());
	}

	public boolean isWomiInteractive() {
		String currentEngine = getEngine();
		for (Engine possibleEngine : Engine.values()) {
			if (possibleEngine.getValue().equals(currentEngine)) {
				return false;
			}
		}
		return true;
	}

}

@Getter
@RequiredArgsConstructor
enum Engine {
	IMAGE("image"), ICON("icon"), VIDEO("video"), AUDIO("audio");

	private final String value;
}