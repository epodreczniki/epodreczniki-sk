package pl.epodr.sk.statalt.validator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WellFormedXmlValidatorTest {

	@Test
	public void testFindNamedEntity() {
		WellFormedXmlValidator wfxv = new WellFormedXmlValidator(null);
		assertEquals("&nbsp;", wfxv.findNamedEntity("to jest fajny&nbsp;tekst"));
		assertEquals(null, wfxv.findNamedEntity("a ten jest już&#160;ok"));
	}

}
