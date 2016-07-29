package pl.epodr.sk.files;

import java.io.InputStream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
class DownloadableStream extends Downloadable {

	private final InputStream inputStream;

	private final String contentType;

	private final int contentLength;

	private final boolean inEditionOnline;

}
