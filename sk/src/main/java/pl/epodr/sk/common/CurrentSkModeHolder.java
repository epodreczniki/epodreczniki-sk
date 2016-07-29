package pl.epodr.sk.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrentSkModeHolder {

	private static final String PRODUCTION_VALUE = "production";

	@Value("${SK_MODE}")
	private String skMode;

	public String getMode() {
		return skMode;
	}

	public boolean isProduction() {
		return PRODUCTION_VALUE.equals(skMode);
	}

}
