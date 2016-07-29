package pl.epodr.sk.task.notifications;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
public class CoverNotFoundError extends CollectionNotification {

	private final long coverWomiId;

	@Override
	public CollectionNotificationType getType() {
		return CollectionNotificationType.ERROR;
	}

	@Override
	public String getMessage() {
		return String.format("WOMI wskazane jako okładka (%d) nie istnieje.", coverWomiId);
	}

}
