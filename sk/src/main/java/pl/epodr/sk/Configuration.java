package pl.epodr.sk;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pl.epodr.sk.task.ConversionFormat;

@Service
@Slf4j
public class Configuration {

	private List<ConversionFormat> staticFormatsToGenerate;

	@Value("${staticFormats}")
	private String staticFormatsToGenerateRaw;

	@PostConstruct
	void init() {
		initStaticFormatsToGenerate();
	}

	public LinkedList<ConversionFormat> getStaticFormatsToGenerate() {
		return new LinkedList<>(staticFormatsToGenerate);
	}

	private void initStaticFormatsToGenerate() {
		staticFormatsToGenerate = new LinkedList<>();
		for (String format : staticFormatsToGenerateRaw.split(",")) {
			if (StringUtils.isNotEmpty(format)) {
				staticFormatsToGenerate.add(ConversionFormat.valueOf(format));
			}
		}
		log.info("static formats to generate: " + staticFormatsToGenerate);
	}

}
