package pl.epodr.sk.task;

import java.util.Comparator;

import org.apache.commons.lang.math.NumberUtils;

public class TaskComparator implements Comparator<Task> {

	public static final int STATIC_TASKS_PRIORITY_BORDER = 10;

	@Override
	public int compare(Task o1, Task o2) {
		if (o1 == null) {
			return -1;
		}
		if (o2 == null) {
			return 1;
		}

		int p1 = getPriority(o1);
		int p2 = getPriority(o2);
		if (p1 > p2) {
			return -1;
		}
		if (p2 > p1) {
			return 1;
		}

		int startTimeoutComparation = Integer.compare(o1.getStartTimeout(), o2.getStartTimeout());
		if (startTimeoutComparation != 0) {
			return startTimeoutComparation;
		}

		if (o1 instanceof CollectionTask && o2 instanceof CollectionTask) {
			long version1 = ((CollectionTask) o1).getCollectionDescriptor().getVersion();
			long version2 = ((CollectionTask) o2).getCollectionDescriptor().getVersion();
			if (version1 > version2) {
				return -1;
			} else if (version1 < version2) {
				return 1;
			}

			String col1Str = ((CollectionTask) o1).getCollectionDescriptor().getId();
			String col2Str = ((CollectionTask) o2).getCollectionDescriptor().getId();
			boolean col1isNumber = NumberUtils.isNumber(col1Str);
			boolean col2isNumber = NumberUtils.isNumber(col2Str);
			if (col1isNumber && !col2isNumber) {
				return 1;
			} else if (!col1isNumber && col2isNumber) {
				return -1;
			} else if (col1isNumber && col2isNumber) {
				long col1 = Long.parseLong(col1Str);
				long col2 = Long.parseLong(col2Str);
				if (col1 > col2) {
					return -1;
				} else if (col1 < col2) {
					return 1;
				}
			}
		}

		if (o1.getTimestamp() != null && o2.getTimestamp() != null) {
			if (o1.getTimestamp().before(o2.getTimestamp())) {
				return -1;
			}
			if (o2.getTimestamp().before(o1.getTimestamp())) {
				return 1;
			}
		}
		return 0;
	}

	int getPriority(Task t) {
		if (t instanceof TransformationUpdateTask) {
			return 30;
		}
		if (t instanceof DeleteTask) {
			return 29;
		}
		if (t instanceof WomiModifiedTask) {
			return 28;
		}

		if (t instanceof DownloadTask) {
			return 16;
		}
		if (t instanceof SplitVariantsTask) {
			return 17;
		}
		if (t instanceof DownloadWomiTask) {
			return 18;
		}
		if (t instanceof ConversionTask) {
			switch (((ConversionTask) t).getFormat()) {
				case EPXHTML:
					return 19;
				case HTML:
					return 20;

				case PDF:
					return 7;
				case MOBILE:
					return 4;
				default:
					return 0;
			}
		}
		if (t instanceof DownloadStaticWomisTask) {
			switch (((DownloadStaticWomisTask) t).getFormat()) {
				case PDF:
					return 3;
				case MOBILE:
					return 1;
				default:
					return 0;
			}
		}
		return -99;
	}

}
