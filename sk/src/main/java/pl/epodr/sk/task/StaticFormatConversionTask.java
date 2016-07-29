package pl.epodr.sk.task;

import java.util.List;

import pl.epodr.sk.IdAndVersion;
import pl.epodr.sk.task.common.ConversionReason;

abstract class StaticFormatConversionTask extends ConversionTask {

	public static ConversionTask create(IdAndVersion coll, String variant, ConversionFormat format,
			ConversionReason reason, List<String> moduleIds, int numOfCollectionVariants) {
		switch (format) {
			case MOBILE:
				return new MobileConversionTask(coll, variant, reason, moduleIds, numOfCollectionVariants);
			default:
				return new StaticButNotMobileFormatConversionTask(coll, variant, format, reason, moduleIds,
						numOfCollectionVariants);
		}
	}

	public StaticFormatConversionTask(IdAndVersion coll, String variant, ConversionReason reason,
			List<String> moduleIds, int numOfCollectionVariants) {
		super(coll, variant, reason, moduleIds, numOfCollectionVariants);
	}

	@Override
	public boolean hasPreconditionsFulfilled() {
		if (queueManager.hasTransformationUpdateTask()) {
			return false;
		}
		for (Task task : queueManager.getRunningTasks()) {
			TaskComparator tc = new TaskComparator();
			if (tc.getPriority(task) >= TaskComparator.STATIC_TASKS_PRIORITY_BORDER) {
				return false;
			}
		}

		for (Task task : queueManager.getQueuedAndRunningTasks()) {
			if (task instanceof DownloadStaticWomisTask) {
				DownloadStaticWomisTask dswTask = (DownloadStaticWomisTask) task;
				if (doesTheGivenTaskDownloadWomisForThis(dswTask)) {
					return false;
				}
			}
		}

		if (!queueManager.canProcessAnotherStaticTaskNow()) {
			return false;
		}

		return true;
	}

	boolean doesTheGivenTaskDownloadWomisForThis(DownloadStaticWomisTask dswTask) {
		if (!dswTask.getCollectionDescriptor().equals(this.getCollectionDescriptor())) {
			return false;
		}

		return dswTask.getFormat() == this.getFormat();
	}

}
