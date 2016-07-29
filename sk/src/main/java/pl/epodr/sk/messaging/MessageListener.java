package pl.epodr.sk.messaging;

import lombok.extern.slf4j.Slf4j;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.QueueManager;
import pl.epodr.sk.files.DownloadManager;
import pl.epodr.sk.task.common.ConversionReason;
import pl.epodr.sk.task.common.ReasonType;

@Slf4j
public class MessageListener implements org.springframework.amqp.core.MessageListener {

	private final Logger requestLogger = LoggerFactory.getLogger("requestLogger");

	@Autowired
	private QueueManager queueManager;

	@Autowired
	private DownloadManager downloadManager;

	@Override
	public void onMessage(Message message) {
		String routingKeySource = message.getMessageProperties().getReceivedRoutingKey();
		RoutingKey routingKey = null;
		try {
			routingKey = new RoutingKey(routingKeySource);
		} catch (IllegalArgumentException e) {
			log.error("cannot parse routing key: " + routingKeySource);
			return;
		}

		try {
			JSONObject json = new JSONObject(new String(message.getBody()));
			logRequest(routingKey, new String(message.getBody()));

			String request = routingKey.getRequest();
			request = request.replace("colxml", "collection");
			switch (request) {
				case "womi.modified":
				case "womi.deleted":
					queueManager.handleWomiModified(extractWomiId(json),
							new ConversionReason(ReasonType.fromRawString(request)));
					break;
				case "collection.added":
				case "collection.modified":
					queueManager.handleCollectionModified(extractCollectionDescriptor(json), new ConversionReason(
							ReasonType.fromRawString(request)));
					break;
				case "collection.dependencies-modified":
					queueManager.handleCollectionDependenciesModified(extractCollectionDescriptor(json),
							new ConversionReason(ReasonType.fromRawString(request)));
					break;
				case "collection.missing":
					queueManager.handleCollectionMissing(extractCollectionDescriptor(json), new ConversionReason(
							ReasonType.fromRawString(request)));
					break;
				case "collection.deleted":
					queueManager.handleCollectionDeleted(extractCollectionDescriptor(json));
					break;
				case "module.added":
				case "module.modified":
				case "module.deleted":
					break;
				default:
					log.error("cannot handle request: " + request);
			}
		} catch (JSONException e) {
			log.error("cannot parse request: " + new String(message.getBody()) + "; " + e.getMessage());
		}
	}

	private long extractWomiId(JSONObject json) {
		return json.getLong("womi_id");
	}

	private IdAndVersion extractCollectionDescriptor(JSONObject json) {
		return new IdAndVersion(json.get("collection_id").toString(), json.getLong("collection_version"));
	}

	private void logRequest(RoutingKey routingKey, Object parameter) {
		requestLogger.debug(String.format("%s %s from %s", routingKey.getRequest(), parameter, routingKey.getSender()));
	}

}
