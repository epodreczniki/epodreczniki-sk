package pl.epodr.sk.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Git {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String transformationStateInfoFilename = "transformation-state.info";

	@Value("${pl.epodr.sk.converter.PythonRunner.workingDir}")
	private String transformationWorkingDir;

	@Value("${pl.epodr.sk.converter.PythonRunner.workingDir}")
	private String workingDir;

	public String getTransformationState() {
		File file = new File(transformationWorkingDir, transformationStateInfoFilename);
		List<String> states = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("#") && line.length() > 0) {
					states.add(line);
				}
			}
		} catch (IOException e) {
			logger.error("when reading transformation state", e);
			return null;
		}
		return StringUtils.join(states, " ").trim();
	}

}
