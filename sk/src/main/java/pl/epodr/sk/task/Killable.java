package pl.epodr.sk.task;

public interface Killable {

	public static final Killable UNKILLABLE = new Killable() {

		@Override
		public boolean isKilled() {
			return false;
		}
	};

	boolean isKilled();
}
