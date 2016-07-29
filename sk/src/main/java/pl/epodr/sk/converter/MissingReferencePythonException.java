package pl.epodr.sk.converter;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pl.epodr.sk.converter.PythonRunner.PythonScriptException;

@Slf4j
public class MissingReferencePythonException extends PythonScriptException {

	private static final String MISSING_REFERENCE_TOKEN = "[XSLT_ERR] Missing_reference=";

	private static final String FIELD_SEPARATOR = "|-|";

	public static boolean hasMissingReferences(List<String> pythonStackTrace) {
		for (String line : pythonStackTrace) {
			if (line.startsWith(MISSING_REFERENCE_TOKEN)) {
				return true;
			}
		}
		return false;
	}

	private final List<String> pythonStackTrace;

	public MissingReferencePythonException(String message, List<String> pythonStackTrace) {
		super(message);
		this.pythonStackTrace = pythonStackTrace;
	}

	/**
	 * Pattern:
	 * 
	 * [XSLT_ERR] Missing_reference=[{local/remote};{reference_type};{reference_id};
	 * {target_name};{reference_content};{module_id};{module_version}]
	 */
	public List<MissingReference> getMissingReferences() {
		List<MissingReference> list = new LinkedList<>();
		for (String line : pythonStackTrace) {
			if (line.startsWith(MISSING_REFERENCE_TOKEN)) {
				line = line.substring(MISSING_REFERENCE_TOKEN.length()).trim();
				if (line.charAt(0) != '[' || line.charAt(line.length() - 1) != ']') {
					log.warn("error parsing line from \"{}\": {}", getMessage(), line);
				} else {
					line = line.substring(1, line.length() - 1);
					String[] p = line.split(Pattern.quote(FIELD_SEPARATOR));
					String module = String.format("%s (v.%s)", p[5], p[6]);
					MissingReference ref = new MissingReference(p[0], p[1], p[2], p[3], p[4], module);
					list.add(ref);
				}
			}
		}
		return list;
	}

	@Data
	public static class MissingReference {

		private final String scope;

		private final String type;

		private final String id;

		private final String targetName;

		private final String content;

		private final String module;

	}

}
