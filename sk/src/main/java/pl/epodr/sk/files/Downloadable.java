package pl.epodr.sk.files;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public abstract class Downloadable {

	public abstract String getContentType();

	public abstract InputStream getInputStream();

	public abstract int getContentLength();

	public abstract boolean isInEditionOnline();

	public byte[] getBytes() throws IOException {
		InputStream is = getInputStream();
		byte[] bytes = IOUtils.toByteArray(is);
		IOUtils.closeQuietly(is);
		return bytes;
	}

}
