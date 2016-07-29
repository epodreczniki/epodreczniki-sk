package pl.epodr.sk.task.notifications;

import lombok.ToString;

@ToString
public class NoModulesError extends CollectionNotification {

	@Override
	public CollectionNotificationType getType() {
		return CollectionNotificationType.ERROR;
	}

	@Override
	public String getMessage() {
		return "Ta kolekcja nie zawiera żadnego modułu";
	}

}
