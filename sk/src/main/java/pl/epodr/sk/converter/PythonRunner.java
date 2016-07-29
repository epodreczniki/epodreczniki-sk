package pl.epodr.sk.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pl.epodr.sk.task.Killable;
import pl.epodr.sk.task.TaskKilledException;

@Service
public class PythonRunner {

	private static final List<String> PYTHON_EXCEPTION_TOKENS = Arrays.asList("Traceback (most recent call last):",
			"Error at", "Error on line", "Exception in thread", "[PY_XSLT_ERR]", "[XSLT_ERR]", "[PY_ERR]");

	private static final Logger logger = LoggerFactory.getLogger(PythonRunner.class);

	@Value("${pl.epodr.sk.converter.PythonRunner.pythonPath}")
	private String pythonPath;

	@Value("${pl.epodr.sk.converter.PythonRunner.workingDir}")
	private String workingDir;

	public void runScript(String script, Killable parent) throws IOException, InterruptedException {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			logger.warn("cannot run: " + script);
			return;
		}
		logger.debug("running: " + pythonPath + " " + script);

		List<String> params = new ArrayList<>();
		params.add(0, pythonPath);
		for (String param : script.split(" ")) {
			params.add(param);
		}

		ProcessBuilder pb = new ProcessBuilder(params);
		pb.directory(new File(workingDir));
		pb.redirectErrorStream(true);
		Process p = pb.start();

		List<String> pythonStackTrace = new LinkedList<>();
		boolean pythonStackTraceAppeared = false;

		try (@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
			String line = "";
			while ((line = br.readLine()) != null) {
				System.out.println(" > " + line);

				if (!pythonStackTraceAppeared && doesPythonErrorStartsHere(line)) {
					pythonStackTraceAppeared = true;
				}
				if (pythonStackTraceAppeared) {
					pythonStackTrace.add(line);
				}

				if (parent.isKilled()) {
					p.destroy();
					br.close();
					p.waitFor();
					System.out.println(" > INTERRUPTED");
					throw new TaskKilledException();
				}
			}
		}

		p.waitFor();

		if (p.exitValue() != 0) {
			if (MissingReferencePythonException.hasMissingReferences(pythonStackTrace)) {
				throw new MissingReferencePythonException(script, pythonStackTrace);
			} else {
				throw new PythonScriptException(script, pythonStackTrace);
			}
		}

		logger.debug("python script finished");
	}

	private boolean doesPythonErrorStartsHere(String line) {
		for (String token : PYTHON_EXCEPTION_TOKENS) {
			if (line.contains(token)) {
				return true;
			}
		}
		return false;
	}

	public static class PythonScriptException extends RuntimeException {

		public PythonScriptException(String message, List<String> pythonStackTrace) {
			super(message + "\n" + StringUtils.join(pythonStackTrace, "\n"));
		}

		protected PythonScriptException(String message) {
			super(message);
		}

	}
}
