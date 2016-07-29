package pl.epodr.sk.task.notifications;

import lombok.ToString;

@ToString
public class UnknownError extends CollectionNotification {

	@Override
	public CollectionNotificationType getType() {
		return CollectionNotificationType.ERROR;
	}

	@Override
	public String getMessage() {
		return "Wystąpił błąd przy przetwarzaniu kolekcji. W razie pytań skontaktuj się z administratorem.";
	}

}
