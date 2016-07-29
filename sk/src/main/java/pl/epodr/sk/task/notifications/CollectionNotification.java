package pl.epodr.sk.task.notifications;

import java.util.LinkedList;
import java.util.List;

public abstract class CollectionNotification {

	public abstract CollectionNotificationType getType();

	public abstract String getMessage();

	public List<CollectionNotification> getNotifications() {
		LinkedList<CollectionNotification> list = new LinkedList<>();
		list.add(this);
		return list;
	}

}
