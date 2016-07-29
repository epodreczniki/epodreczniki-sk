package pl.epodr.sk.converter.css;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pl.epodr.sk.converter.css.PdfCssManager;

public class PdfCssManagerTest {

	@Test
	public void testGetStylesheetName() {
		PdfCssManager pcm = new PdfCssManager();
		assertEquals(null, pcm.extractStylesheetNameFromFilename("test.css"));
		assertEquals(null, pcm.extractStylesheetNameFromFilename("pdf.css"));
		assertEquals(null, pcm.extractStylesheetNameFromFilename("pdf-.css"));
		assertEquals("mathematics", pcm.extractStylesheetNameFromFilename("pdf-mathematics.css"));
		assertEquals("informatics", pcm.extractStylesheetNameFromFilename("pdf-informatics.css"));
	}

}
