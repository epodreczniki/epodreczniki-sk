package pl.epodr.sk.task;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pl.epodr.sk.QueueManager;
import pl.epodr.sk.converter.css.PdfCssManager;

public class TransformationUpdateTask extends Task {

	@Autowired
	private QueueManager queueManager;

	@Autowired
	private PdfCssManager pdfCssManager;

	@Override
	public void process() {
		pdfCssManager.scanCssFiles();
	}

	@Override
	public List<Task> getNext() {
		return null;
	}

	@Override
	public boolean hasPreconditionsFulfilled() {
		return queueManager.hasNoRunningTasks();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	@Override
	public Class<? extends Task> getSerializeTo() {
		return this.getClass();
	}

}
