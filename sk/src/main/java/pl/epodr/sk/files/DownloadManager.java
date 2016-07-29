package pl.epodr.sk.files;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import pl.epodr.sk.IdAndVersion;

@Service
public class DownloadManager {

	@Value("${pl.epodr.sk.files.DownloadManager.epxmlUrl}")
	private String epxmlUrl;

	@Value("${pl.epodr.sk.files.DownloadManager.metadata2Url}")
	private String metadata2Url;

	@Value("${pl.epodr.sk.files.DownloadManager.womiStaticAlternativeUrl}")
	private String womiStaticAlternativeUrl;

	@Value("${pl.epodr.sk.files.DownloadManager.womiEmitUrl}")
	private String womiEmitUrl;

	@Value("${pl.epodr.sk.files.DownloadManager.womiEmitIconUrl}")
	private String womiEmitIconUrl;

	@Value("${pl.epodr.sk.files.DownloadManager.womiEmitInteractiveUrl}")
	private String womiEmitInteractiveUrl;

	@Value("${pl.epodr.sk.files.DownloadManager.womiManifestJsonUrl}")
	private String womiManifestJsonUrl;

	@Value("${pl.epodr.sk.files.DownloadManager.colXmlUrl}")
	private String colXmlUrl;

	@Value("${pl.epodr.sk.files.DownloadManager.notFoundWomiId}")
	private long notFoundWomiId;

	@Value("${pl.epodr.sk.files.DownloadManager.errorWomiId}")
	private long errorWomiId;

	@Value("${pl.epodr.sk.files.DownloadManager.rtHealthCheckUrl}")
	private String rtHealthCheckUrl;

	@Value("${pl.epodr.sk.files.DownloadManager.rtHealthCheckWomiId}")
	private long rtHealthCheckWomiId;

	@Value("${pl.epodr.sk.files.DownloadManager.rtHealthCheckCollectionIdAndVersion}")
	private String rtHealthCheckCollectionIdAndVersion;

	@Autowired
	private HttpClient httpClient;

	public long getNotFoundWomiId() {
		return notFoundWomiId;
	}

	public long getErrorWomiId() {
		return errorWomiId;
	}

	Downloadable getEpxml(IdAndVersion module) throws IOException {
		String url = getEpxmlUrl(module);
		return readDownloadableFromUrl(url);
	}

	private String getEpxmlUrl(IdAndVersion module) {
		return String.format(epxmlUrl, module.getId(), module.getVersion());
	}

	Downloadable getWomiMetadata2(long id) throws IOException {
		return readDownloadableFromUrl(getWomiMetadata2Url(id));
	}

	private String getWomiMetadata2Url(long womiId) {
		return String.format(metadata2Url, womiId);
	}

	public Downloadable getWomiInteractivePackage(long womiId) throws IOException, ParserConfigurationException,
			SAXException {
		String url = String.format(womiEmitInteractiveUrl, womiId) + "?zip";
		return readDownloadableFromUrl(url);
	}

	WomiEmit getWomiEmit(long womiId, String version) throws IOException {
		return getWomiEmit(womiId, version, "");
	}

	WomiEmit getWomiEmit(long womiId, String version, String suffix) throws IOException {
		String url = getWomiEmitUrl(womiId, version) + suffix;
		Downloadable d = readDownloadableFromUrl(url);
		byte[] content = d.getBytes();
		return new WomiEmit(content, d.getContentType());
	}

	public String getWomiEmitUrl(long womiId, String version) {
		return String.format(womiEmitUrl, womiId, version);
	}

	WomiEmit getWomiEmit(long womiId, String version, int res) throws IOException {
		return getWomiEmit(womiId, version, "?res=" + res);
	}

	WomiEmit getWomiEmit(long womiId, String version, int res, String suffix) throws IOException {
		return getWomiEmit(womiId, version, "?res=" + res + suffix);
	}

	WomiEmit getWomiEmitIcon(long womiId, String version) throws IOException {
		String url = getWomiEmitIconUrl(womiId, version);
		Downloadable d = readDownloadableFromUrl(url);
		byte[] content = d.getBytes();
		return new WomiEmit(content, d.getContentType());
	}

	String getWomiEmitIconUrl(long womiId, String version) {
		return String.format(womiEmitIconUrl, womiId, version);
	}

	Downloadable getWomiEmitInteractive(long womiId) throws IOException {
		String url = String.format(womiEmitInteractiveUrl, womiId);
		return readDownloadableFromUrl(url);
	}

	Downloadable getColXml(IdAndVersion collection) throws IOException {
		String url = getColXmlUrl(collection);
		return readDownloadableFromUrl(url);
	}

	public String getColXmlUrl(IdAndVersion collection) {
		return String.format(colXmlUrl, collection.getId(), collection.getVersion());
	}

	private String getWomiManifestJsonUrl(long womiId) {
		return String.format(womiManifestJsonUrl, womiId);
	}

	Downloadable getWomiManifestJson(long womiId) throws IOException {
		return readDownloadableFromUrl(getWomiManifestJsonUrl(womiId));
	}

	private Downloadable readDownloadableFromUrl(String url) throws IOException {
		return httpClient.download(url);
	}

	public String getRtHealthCheckUrl() {
		return rtHealthCheckUrl;
	}

	public byte[] getStaticAlternative(long womiId) throws IOException {
		String url = getStaticAlternativeUrl(womiId);
		return readDownloadableFromUrl(url).getBytes();
	}

	private String getStaticAlternativeUrl(long womiId) {
		return String.format(womiStaticAlternativeUrl, womiId);
	}

	public long getRtHealthCheckWomiId() {
		return rtHealthCheckWomiId;
	}

	public IdAndVersion getRtHealthCheckCollection() {
		String[] p = rtHealthCheckCollectionIdAndVersion.split("[-/]");
		return new IdAndVersion(p[0], Long.parseLong(p[1]));
	}

}
