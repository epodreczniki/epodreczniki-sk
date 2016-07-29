package pl.epodr.sk.task;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;
import org.mockito.Mockito;

import pl.epodr.sk.IdAndVersion;

public class ConversionTaskTest {

	@Test
	public void testDoesTheGivenTaskDownloadWomisForThis() {
		IdAndVersion coll = Mockito.mock(IdAndVersion.class);

		StaticFormatConversionTask convPdf = new StaticButNotMobileFormatConversionTask(coll, "student-canon",
				ConversionFormat.PDF, null, null, 1);
		DownloadStaticWomisTask downPdf = new DownloadStaticWomisTask(coll, null, ConversionFormat.PDF,
				new HashSet<Long>());

		assertEquals(true, convPdf.doesTheGivenTaskDownloadWomisForThis(downPdf));
	}

}
