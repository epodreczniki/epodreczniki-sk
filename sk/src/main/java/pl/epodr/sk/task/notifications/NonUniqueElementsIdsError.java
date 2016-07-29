package pl.epodr.sk.task.notifications;

import java.util.LinkedList;
import java.util.List;

import lombok.ToString;

import org.apache.commons.lang.StringUtils;

@ToString
public class NonUniqueElementsIdsError extends CollectionNotification {

	private String elementId;

	private List<String> moduleIds = new LinkedList<>();

	public NonUniqueElementsIdsError(String elementId, String... moduleIds) {
		this.elementId = elementId;
		for (String moduleId : moduleIds) {
			this.moduleIds.add(moduleId);
		}
	}

	@Override
	public CollectionNotificationType getType() {
		return CollectionNotificationType.ERROR;
	}

	@Override
	public String getMessage() {
		return String.format(
				"Identyfikatory elementów w treści nie są unikalne (element o id \"%s\" pojawia się w modułach: %s)",
				elementId, StringUtils.join(moduleIds, ", "));
	}
}
