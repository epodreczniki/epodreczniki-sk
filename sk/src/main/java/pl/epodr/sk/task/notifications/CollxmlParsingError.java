package pl.epodr.sk.task.notifications;

import lombok.ToString;

@ToString
public class CollxmlParsingError extends CollectionNotification {

	@Override
	public CollectionNotificationType getType() {
		return CollectionNotificationType.ERROR;
	}

	@Override
	public String getMessage() {
		return "Kolekcja jest uszkodzona. W razie pytań skontaktuj się z administratorem.";
	}

}
