package pl.epodr.sk.task.notifications;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import pl.epodr.sk.converter.MissingReferencePythonException.MissingReference;

@ToString(exclude = { "singleNotifications" })
public class MissingReferenceError extends CollectionNotification {

	private static final String MESSAGE = "Została zdefiniowana błędna referencja \"%s\" typu "
			+ "\"%s\" o identyfikatorze \"%s\", która występuje w module %s. "
			+ "Treść referencji to \"%s\", a element nazwa celu zawiera wartość \"%s\".";

	private static final String EXTRA_EXPLANATIONS = "Referencja \"local\" powinna odwoływać się do hasła "
			+ "słownikowego zadeklarowanego w tym samym module, co referencja. Referencja \"remote\" powinna "
			+ "odwoływać się do hasła słownikowego zadeklarowanego w innym module niż referencja.";

	private final List<CollectionNotification> singleNotifications = new LinkedList<>();

	public MissingReferenceError(List<MissingReference> missingReferences) {
		if (missingReferences.size() > 0) {
			for (MissingReference ref : missingReferences) {
				String message = String.format(MESSAGE, ref.getScope(), ref.getType(), ref.getId(), ref.getModule(),
						ref.getContent(), ref.getTargetName());
				singleNotifications.add(new SingleError(CollectionNotificationType.ERROR, message));
			}
			singleNotifications.add(new SingleError(CollectionNotificationType.WARNING, EXTRA_EXPLANATIONS));
		} else {
			singleNotifications.add(new UnknownError());
		}
	}

	@Override
	public CollectionNotificationType getType() {
		return CollectionNotificationType.ERROR;
	}

	@Override
	public String getMessage() {
		return "missing reference error";
	}

	@Override
	public List<CollectionNotification> getNotifications() {
		return singleNotifications;
	}
}

@RequiredArgsConstructor
@Getter
class SingleError extends CollectionNotification {

	public final CollectionNotificationType type;

	public final String message;

}