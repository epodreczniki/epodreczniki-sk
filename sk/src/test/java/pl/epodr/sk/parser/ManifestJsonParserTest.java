package pl.epodr.sk.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ManifestJsonParserTest {

	private static final String TEST = "/manifest.json";

	private static final String TEST2 = "/manifest-without-embedded.json";

	@Test
	public void testGetEngine() throws IOException {
		String content = loadContent(TEST);
		assertEquals("womi_exercise_engine", new ManifestJsonParser(content).getEngine());

		content = loadContent(TEST2);
		assertEquals("audio", new ManifestJsonParser(content).getEngine());
	}

	@Test
	public void testGetEmbeddedWomis() throws IOException {
		String content = loadContent(TEST);
		ManifestJsonParser parser = new ManifestJsonParser(content);
		List<Long> list = parser.getEmbeddedWomis();
		assertEquals(2, list.size());
		assertEquals(3959, (long) list.get(0));
		assertEquals(2350, (long) list.get(1));
		assertFalse(parser.hasAdvancedStaticAlternative());
		assertFalse(parser.isWomiAudio());
		assertFalse(parser.isWomiIcon());
		assertTrue(parser.isWomiInteractive());

		content = loadContent(TEST2);
		parser = new ManifestJsonParser(content);
		list = parser.getEmbeddedWomis();
		assertEquals(0, list.size());
		assertTrue(parser.hasAdvancedStaticAlternative());
		assertTrue(parser.isWomiAudio());
		assertFalse(parser.isWomiIcon());
		assertFalse(parser.isWomiInteractive());
	}

	private String loadContent(String resourcePath) throws IOException {
		InputStream is = getClass().getResourceAsStream(resourcePath);
		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer, "utf-8");
		String content = writer.toString();
		return content;
	}

}
