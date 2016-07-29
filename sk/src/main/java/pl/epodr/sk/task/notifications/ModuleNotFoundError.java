package pl.epodr.sk.task.notifications;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import pl.epodr.sk.IdAndVersion;

@RequiredArgsConstructor
@ToString
public class ModuleNotFoundError extends CollectionNotification {

	private final IdAndVersion module;

	@Override
	public CollectionNotificationType getType() {
		return CollectionNotificationType.ERROR;
	}

	@Override
	public String getMessage() {
		return String.format("W tej kolekcji użyto modułu %s/%d, który nie istnieje", module.getId(),
				module.getVersion());
	}

}
