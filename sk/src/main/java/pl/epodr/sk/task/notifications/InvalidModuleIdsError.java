package pl.epodr.sk.task.notifications;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import pl.epodr.sk.IdAndVersion;

@RequiredArgsConstructor
@ToString
public class InvalidModuleIdsError extends CollectionNotification {

	private final IdAndVersion module;

	@Override
	public CollectionNotificationType getType() {
		return CollectionNotificationType.ERROR;
	}

	@Override
	public String getMessage() {
		return String.format("Moduł %s zawiera nieprawidłowe identyfikatory własne", module.getId());
	}

}
