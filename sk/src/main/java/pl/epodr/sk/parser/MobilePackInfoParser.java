package pl.epodr.sk.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;

import pl.epodr.sk.files.FileManager;

@Slf4j
public class MobilePackInfoParser {

	private final InputStream is;

	public MobilePackInfoParser(InputStream is) {
		this.is = is;
	}

	public Map<Integer, Long> getMobilePacksSizes() throws IOException, FileNotFoundException {
		Map<Integer, Long> map = new LinkedHashMap<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}

				String[] p = line.split("\\t");
				if (p.length != 2) {
					log.error(String.format("line '%s' has %d fields instead of 2", line, p.length));
				} else if (!NumberUtils.isNumber(p[0]) || !NumberUtils.isNumber(p[1])) {
					log.error(String.format("line '%s' does not contain numbers", line));
				} else {
					int resolution = Integer.parseInt(p[0]);
					if (ArrayUtils.contains(FileManager.MOBILE_PACK_RESOLUTIONS, resolution)) {
						map.put(resolution, Long.parseLong(p[1]));
					} else {
						log.error(String.format("resolution %d is not valid", resolution));
					}
				}
			}
		}
		return map;
	}

}
