package pl.epodr.sk.messaging;

import java.io.UnsupportedEncodingException;

import lombok.extern.slf4j.Slf4j;

import org.json.JSONObject;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.files.FileManager;
import pl.epodr.sk.task.ConversionFormat;
import pl.epodr.sk.task.common.ConversionReason;

@Service
@Slf4j
public class Messenger {

	@Autowired
	private RabbitTemplate template;

	@Value("${messaging.id}")
	private String messagingId;

	@Async
	public void notifyConversionFinished(IdAndVersion coll, String variant, ConversionFormat format,
			ConversionReason reason) {
		notifyConversionFinished(coll, variant, format.name().toLowerCase(), reason);
	}

	@Async
	public void notifyConversionFinished(IdAndVersion coll, String variant, String format, ConversionReason reason) {
		JSONObject json = createJSONForCollection(coll);
		json.put("variant", variant);
		json.put("format", format);
		json.put("reason_type", reason.getType().toRawString());
		json.put("reason_timestamp", reason.getTimestamp());
		send("collection.transformed", json);
	}

	@Async
	public void notifyMetadataChanged(IdAndVersion coll) {
		JSONObject json = createJSONForCollection(coll);
		send("collection.metadata-changed", json);
	}

	private JSONObject createJSONForCollection(IdAndVersion coll) {
		JSONObject json = new JSONObject();
		json.put("collection_id", coll.getId());
		json.put("collection_version", coll.getVersion());
		return json;
	}

	private void send(String messageCode, JSONObject json) {
		String routingKey = messagingId + "." + messageCode;
		log.debug("sending message as " + routingKey + " with body " + json);
		Message message = createMessage(json);
		template.convertAndSend(routingKey, message);
	}

	private Message createMessage(JSONObject json) {
		byte[] body;
		try {
			body = json.toString().getBytes(FileManager.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType("application/json");
		messageProperties.setContentEncoding(FileManager.ENCODING);
		return new Message(body, messageProperties);
	}

	public void notifyMetadataChangedAndEmissionFormatsDeleted(IdAndVersion coll, ConversionReason reason) {
		notifyMetadataChanged(coll);
		for (String variant : FileManager.VARIANTS) {
			notifyConversionFinished(coll, variant, "xml", reason);
		}
	}

}
