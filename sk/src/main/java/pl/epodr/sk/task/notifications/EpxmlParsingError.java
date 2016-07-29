package pl.epodr.sk.task.notifications;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import pl.epodr.sk.IdAndVersion;

@RequiredArgsConstructor
@ToString
public class EpxmlParsingError extends CollectionNotification {

	private final IdAndVersion module;

	@Override
	public CollectionNotificationType getType() {
		return CollectionNotificationType.ERROR;
	}

	@Override
	public String getMessage() {
		return String.format("Moduł %s jest uszkodzony. W razie pytań skontaktuj się z administratorem.",
				module.getId());
	}

}
