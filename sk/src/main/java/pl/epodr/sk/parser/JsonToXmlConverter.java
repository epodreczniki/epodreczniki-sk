package pl.epodr.sk.parser;

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonToXmlConverter {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final String jsonContent;

	private final String rootElementName;

	public JsonToXmlConverter(String jsonContent, String rootElementName) throws IOException {
		this.jsonContent = jsonContent;
		this.rootElementName = rootElementName;
	}

	public String getXml() {
		try {
			JSONObject jsonObject = new JSONObject(jsonContent);
			return toString(jsonObject, rootElementName);
		} catch (JSONException e) {
			logger.warn("error when parsing json", e);
			return null;
		}
	}

	private static String toString(Object object, String tagName) throws JSONException {
		StringBuffer sb = new StringBuffer();
		int i;
		JSONArray ja;
		JSONObject jo;
		String key;
		Iterator<?> keys;
		int length;
		String string;
		Object value;
		if (object instanceof JSONObject) {
			if (tagName != null) {
				sb.append('<');
				sb.append(tagName);
				sb.append('>');
			}
			jo = (JSONObject) object;
			keys = jo.keys();
			while (keys.hasNext()) {
				key = keys.next().toString();
				value = jo.opt(key);
				if (value == null) {
					value = "";
				}
				if (value instanceof String) {
					string = (String) value;
				} else {
					string = null;
				}
				if ("content".equals(key)) {
					if (value instanceof JSONArray) {
						ja = (JSONArray) value;
						length = ja.length();
						for (i = 0; i < length; i += 1) {
							if (i > 0) {
								sb.append('\n');
							}
							sb.append(escape(ja.get(i).toString()));
						}
					} else {
						sb.append(escape(value.toString()));
					}
				} else if (value instanceof JSONArray) {
					ja = (JSONArray) value;
					length = ja.length();
					sb.append('<');
					sb.append(key);
					sb.append('>');
					for (i = 0; i < length; i += 1) {
						value = ja.get(i);
						if (value instanceof JSONArray) {
							sb.append('<');
							sb.append(key);
							sb.append('>');
							sb.append(toString(value, null));
							sb.append("</");
							sb.append(key);
							sb.append('>');
						} else {
							sb.append(toString(value, "item"));
						}
					}
					sb.append("</");
					sb.append(key);
					sb.append('>');
				} else if ("".equals(value)) {
					sb.append('<');
					sb.append(key);
					sb.append("/>");
				} else {
					sb.append(toString(value, key));
				}
			}
			if (tagName != null) {
				sb.append("</");
				sb.append(tagName);
				sb.append('>');
			}
			return sb.toString();
		} else {
			if (object.getClass().isArray()) {
				object = new JSONArray(object);
			}
			if (object instanceof JSONArray) {
				ja = (JSONArray) object;
				length = ja.length();
				for (i = 0; i < length; i += 1) {
					sb.append(toString(ja.opt(i), tagName == null ? "array" : tagName));
				}
				return sb.toString();
			} else {
				string = (object == null) ? "null" : escape(object.toString());
				return (tagName == null) ? "\"" + string + "\"" : (string.length() == 0) ? "<" + tagName + "/>" : "<"
						+ tagName + ">" + string + "</" + tagName + ">";
			}
		}
	}

	private static String escape(String string) {
		return XML.escape(string);
	}

}
