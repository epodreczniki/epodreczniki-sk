package pl.epodr.sk.task;

import lombok.Getter;
import pl.epodr.sk.IdAndVersion;

@Getter
public abstract class CollectionTask extends Task {

	private final IdAndVersion collectionDescriptor;

	protected CollectionTask(IdAndVersion collectionDescriptor) {
		this.collectionDescriptor = collectionDescriptor;
	}

	@Override
	public String toString() {
		String timeoutInfo = "";
		int startTimeout = this.getStartTimeout();
		if (startTimeout > 0) {
			timeoutInfo = ", timeout=" + startTimeout;
		}
		return this.getClass().getSimpleName() + "(" + collectionDescriptor + timeoutInfo + ")";
	}

}
