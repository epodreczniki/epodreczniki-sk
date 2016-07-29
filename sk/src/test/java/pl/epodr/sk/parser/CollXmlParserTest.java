package pl.epodr.sk.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import pl.epodr.sk.IdAndVersion;

public class CollXmlParserTest {

	private static final String TEST_COLXML = "/collection.xml";

	@Test
	public void test() throws Exception {
		InputStream is = getClass().getResourceAsStream(TEST_COLXML);
		CollXmlParser parser = new CollXmlParser(is);

		List<IdAndVersion> list = parser.getModules();
		assertEquals(12, list.size());
		assertEquals("i2dnVn3HHk", list.get(0).getId());
		assertEquals(1l, list.get(0).getVersion());
		assertEquals("izzCaHKpcN", list.get(list.size() - 1).getId());
		assertEquals(2l, list.get(list.size() - 1).getVersion());

		assertEquals((long) 2290, (long) parser.getCoverId());

		assertEquals("informatics", parser.getStylesheet());

		assertEquals(false, parser.isEarlyEducation());
	}

	@Test
	public void testIsDummy() {
		assertFalse(CollXmlParser.isDummy(new IdAndVersion("dummYasd", 1)));
		assertFalse(CollXmlParser.isDummy(new IdAndVersion("zzdummyasd", 1)));
		assertFalse(CollXmlParser.isDummy(new IdAndVersion("1dummuasd", 1)));
		assertFalse(CollXmlParser.isDummy(new IdAndVersion("dumyasd", 1)));

		assertTrue(CollXmlParser.isDummy(new IdAndVersion("dummyasd", 1)));
		assertTrue(CollXmlParser.isDummy(new IdAndVersion("dummy", 1)));
		assertTrue(CollXmlParser.isDummy(new IdAndVersion("dummyY123", 1)));
	}

}
