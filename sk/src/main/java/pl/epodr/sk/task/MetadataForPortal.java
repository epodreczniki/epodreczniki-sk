package pl.epodr.sk.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.StringUtils;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.common.Git;
import pl.epodr.sk.files.FileManager;
import pl.epodr.sk.task.notifications.CollectionNotification;
import pl.epodr.sk.task.notifications.CollectionNotificationType;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

@Slf4j
public class MetadataForPortal {

	private static final String XML_DOCTYPE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

	private static final String ENCODING = "utf-8";

	private static XStream xstream;

	static {
		xstream = new XStream();
		xstream.setMode(XStream.NO_REFERENCES);
		xstream.alias("collection-metadata", Metadata.class);
		xstream.aliasAttribute(Metadata.class, "id", "id");
		xstream.aliasAttribute(Metadata.class, "version", "version");
		xstream.aliasAttribute(Metadata.class, "transformationTimestamp", "transformation-timestamp");
		xstream.processAnnotations(TransformationState.class);
		xstream.aliasField("transformation-state", Metadata.class, "transformationState");
		xstream.aliasField("static-formats", Metadata.class, "staticFormats");
		xstream.alias("variant", Variant.class);
		xstream.aliasAttribute(Variant.class, "name", "name");
		xstream.alias("mobile-pack", MobilePack.class);
		xstream.aliasField("mobile-packs", Variant.class, "mobilePacks");
		xstream.aliasAttribute(MobilePack.class, "resolution", "resolution");
		xstream.aliasAttribute(MobilePack.class, "size", "size");
		xstream.alias("notification", Notification.class);
		xstream.aliasAttribute(Notification.class, "type", "type");
		xstream.aliasAttribute(Notification.class, "message", "message");
	}

	private final Metadata metadata;

	public MetadataForPortal(IdAndVersion collection, List<ConversionFormat> formatsToGenerate, Git git) {
		String transformationState = git.getTransformationState();
		String transformationCommit = null; // testing purposes

		this.metadata = new Metadata(collection, transformationState, transformationCommit, formatsToGenerate);
	}

	public MetadataForPortal(InputStream is) {
		this.metadata = (Metadata) xstream.fromXML(is);
	}

	public MetadataForPortal(File file) {
		this.metadata = (Metadata) xstream.fromXML(file);
	}

	public void addVariant(String variant) {
		if (metadata.getVariants() == null) {
			metadata.setVariants(new ArrayList<Variant>());
		}
		metadata.getVariants().add(new Variant(variant));
	}

	public void putMobilePacksSizes(String variant, Map<Integer, Long> mobilePacksSizes) {
		for (Variant var : metadata.getVariants()) {
			if (variant.equals(var.getName())) {
				List<MobilePack> mobilePacks = new ArrayList<>();
				for (Entry<Integer, Long> entry : mobilePacksSizes.entrySet()) {
					mobilePacks.add(new MobilePack(entry.getKey(), entry.getValue()));
				}
				var.setMobilePacks(mobilePacks);
				return;
			}
		}
		log.warn("variant " + variant + " is not known for collection " + metadata.getId() + ":"
				+ metadata.getVersion() + " - probably an error has been set to metadata.xml by another task");
	}

	public File createFile() throws IOException {
		File file = File.createTempFile("collection-metadata-" + metadata.getId() + "-", ".xml");
		try (FileWriterWithEncoding wr = new FileWriterWithEncoding(file, ENCODING)) {
			wr.append(XML_DOCTYPE);
			xstream.toXML(metadata, wr);
		}
		return file;
	}

	public void save(File file) throws IOException {
		FileUtils.moveFile(createFile(), file);
	}

	public List<String> getVariants() {
		List<String> list = new ArrayList<>();
		for (Variant var : metadata.getVariants()) {
			list.add(var.getName());
		}
		return list;
	}

	public void removeStaticFormat(String format) {
		metadata.removeStaticFormat(format);
	}

	public void addStaticFormat(String format) {
		metadata.addStaticFormat(format);
	}

	public void addNotification(CollectionNotification notification) {
		metadata.addNotification(notification);
	}

	public boolean hasErrors() {
		List<Notification> notifications = metadata.getNotifications();
		if (notifications != null) {
			for (Notification notification : notifications) {
				if (CollectionNotificationType.ERROR.name().toLowerCase().equals(notification.getType())) {
					return true;
				}
			}
		}
		return false;
	}

}

@Getter
@Setter
class Metadata {

	private final String id;

	private final long version;

	private final TransformationState transformationState;

	private final long transformationTimestamp = new Date().getTime() / 1000;

	private String staticFormats;

	private List<Variant> variants;

	private List<Notification> notifications;

	public Metadata(IdAndVersion collection, String transformationState, String transformationCommit,
			List<ConversionFormat> formatsToGenerate) {
		super();
		this.id = collection.getId();
		this.version = collection.getVersion();
		this.transformationState = new TransformationState(transformationState, transformationCommit);
		this.staticFormats = initStatisFormatsString(formatsToGenerate);
	}

	public void addNotification(CollectionNotification parentNotification) {
		if (this.notifications == null) {
			this.notifications = new ArrayList<>();
		}

		for (CollectionNotification notification : parentNotification.getNotifications()) {
			String type = notification.getType().name().toLowerCase();
			this.notifications.add(new Notification(type, notification.getMessage()));
		}
	}

	public void addStaticFormat(String format) {
		List<String> res = new LinkedList<>(Arrays.asList(this.staticFormats.split(" ")));
		res.add(format);
		this.staticFormats = StringUtils.join(res, " ");
	}

	public void removeStaticFormat(String format) {
		String[] formats = this.staticFormats.split(" ");
		List<String> res = new LinkedList<>();
		for (String el : formats) {
			if (!el.equals(format)) {
				res.add(el);
			}
		}
		this.staticFormats = StringUtils.join(res, " ");
	}

	private String initStatisFormatsString(List<ConversionFormat> formats) {
		List<String> formatStrings = new LinkedList<>();
		for (ConversionFormat format : formats) {
			if (format != ConversionFormat.MOBILE) {
				formatStrings.add(format.toString().toLowerCase());
			} else {
				for (int size : FileManager.MOBILE_PACK_RESOLUTIONS) {
					formatStrings.add("mobile-" + size);
				}
			}
		}

		return StringUtils.join(formatStrings, " ");
	}
}

@Data
@XStreamConverter(value = ToAttributedValueConverter.class, strings = { "transformationState" })
class TransformationState {

	private final String transformationState;

	@XStreamAsAttribute
	@XStreamAlias("commit")
	private final String transformationCommit;

}

@Data
class Variant {

	private final String name;

	private List<MobilePack> mobilePacks;

}

@Data
class MobilePack {

	private final int resolution;

	private final long size;
}

@Data
class Notification {

	private final String type;

	private final String message;
}