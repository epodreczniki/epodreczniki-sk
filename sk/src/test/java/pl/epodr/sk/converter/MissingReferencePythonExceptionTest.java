package pl.epodr.sk.converter;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import pl.epodr.sk.converter.MissingReferencePythonException.MissingReference;

public class MissingReferencePythonExceptionTest {

	@Test
	public void test() {
		List<String> pythonStackTrace = new LinkedList<>();
		pythonStackTrace.add("test");
		pythonStackTrace.add("[XSLT_ERR] Missing_reference=[local;biography;w3o48ctwoe"
				+ ";kusdgfnsid;To jest taki test;i134rwers;7]");
		pythonStackTrace.add("test");

		MissingReferencePythonException e = new MissingReferencePythonException("test", pythonStackTrace);
		List<MissingReference> refs = e.getMissingReferences();
		assertEquals(1, refs.size());

		MissingReference ref = refs.get(0);
		assertEquals("local", ref.getScope());
		assertEquals("biography", ref.getType());
		assertEquals("w3o48ctwoe", ref.getId());
		assertEquals("kusdgfnsid", ref.getTargetName());
		assertEquals("To jest taki test", ref.getContent());
		assertEquals("i134rwers (v.7)", ref.getModule());
	}
}
