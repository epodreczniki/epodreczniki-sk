package pl.epodr.sk.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import pl.epodr.sk.task.DownloadWomiTask.WomiErrorRefs;

public class EpxmlParserTest {

	private static final String TEST_EPXML = "/test.epxml";

	private static EpxmlParser parser;

	@BeforeClass
	public static void init() throws Exception {
		try (InputStream is = EpxmlParserTest.class.getResourceAsStream(TEST_EPXML)) {
			parser = new EpxmlParser(is);
		}
	}

	@Test
	public void testGetWomiReferences() throws Exception {
		List<Long> list = parser.getWomiReferences();
		assertEquals(4, list.size());
		assertTrue(list.contains(101l));
		assertTrue(list.contains(102l));
		assertTrue(list.contains(103l));
		assertTrue(list.contains(104l));
	}

	@Test
	public void testRewriteWomisToAnother() throws Exception {
		WomiErrorRefs toChange = new WomiErrorRefs("Nie ma WOMI %d");
		toChange.add(102l);
		toChange.add(104l);
		parser.rewriteWomisToAnother(toChange, 99);

		List<Long> list = parser.getWomiReferences();
		assertEquals(3, list.size());
		assertTrue(list.contains(101l));
		assertTrue(list.contains(99l));
		assertTrue(list.contains(103l));
	}

	@Test
	public void testHasValidModuleIds() throws Exception {
		assertTrue(parser.hasValidModuleIds("izlSiytlDl"));
	}

	@Test
	public void testGetSectionIds() {
		List<String> sectionIds = parser.getAllElementIds();
		assertEquals("izlSiytlDl", sectionIds.get(0));
		assertEquals("d3e1764para", sectionIds.get(sectionIds.size() - 1));
	}

}
