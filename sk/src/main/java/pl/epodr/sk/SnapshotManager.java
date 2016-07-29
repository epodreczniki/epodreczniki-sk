package pl.epodr.sk;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import pl.epodr.sk.task.CollectionTask;
import pl.epodr.sk.task.DeleteTask;
import pl.epodr.sk.task.DownloadTask;
import pl.epodr.sk.task.Task;
import pl.epodr.sk.task.TransformationUpdateTask;
import pl.epodr.sk.task.WomiModifiedTask;
import pl.epodr.sk.task.common.ConversionReason;
import pl.epodr.sk.task.common.ReasonType;

@Slf4j
public class SnapshotManager {

	private static final String CSV_FIELD_SEPARATOR = ";";

	private static SnapshotManager instance;

	public synchronized static SnapshotManager getInstance() {
		if (instance == null) {
			instance = new SnapshotManager();
		}
		return instance;
	}

	private final File snapshotFile = new File("SK_SNAPSHOT");

	private SnapshotManager() {
	}

	public synchronized void snapshot(List<Task> tasks) {
		File file;
		try {
			if (tasks.size() > 0) {
				Set<String> lines = serialize(tasks);
				file = File.createTempFile("sk-snapshot-", "");
				try (PrintStream ps = new PrintStream(file)) {
					for (String line : lines) {
						ps.println(line);
					}
				}
				FileUtils.deleteQuietly(snapshotFile);
				FileUtils.moveFile(file, snapshotFile);
			} else {
				FileUtils.deleteQuietly(snapshotFile);
			}
		} catch (IOException e) {
			log.error("error performing snapshot", e);
		}
	}

	public List<Task> load() {
		if (snapshotFile.exists()) {
			log.info("Snapshot file found - restoring tasks");
			try {
				List<String> lines = FileUtils.readLines(snapshotFile);
				return parseLinesToTasks(lines);
			} catch (IOException e) {
				log.error("cannot read snapshot file", e);
			}
		} else {
			log.debug("Snapshot file not found");
		}
		return new LinkedList<>();
	}

	private List<Task> parseLinesToTasks(List<String> lines) {
		List<Task> tasks = new LinkedList<>();
		for (String line : lines) {
			if (line.trim().length() > 0) {
				try {
					tasks.add(deserialize(line));
				} catch (InvalidParameterException | IllegalStateException e) {
					log.error("error parsing line: " + line, e);
				}
			}
		}
		return tasks;
	}

	private Task deserialize(String line) throws InvalidParameterException, IllegalStateException {
		String[] fields = line.split(CSV_FIELD_SEPARATOR);
		String className = fields[0];
		if (className.equals(DownloadTask.class.getSimpleName())) {
			return new DownloadTask(deserializeIdAndVersion(fields), new ConversionReason(ReasonType.ADMIN));
		}
		if (className.equals(DeleteTask.class.getSimpleName())) {
			return new DeleteTask(deserializeIdAndVersion(fields));
		}
		if (className.equals(TransformationUpdateTask.class.getSimpleName())) {
			return new TransformationUpdateTask();
		}
		if (className.equals(WomiModifiedTask.class.getSimpleName())) {
			return new WomiModifiedTask(deserializeWomiId(fields), new ConversionReason(ReasonType.ADMIN));
		}
		throw new IllegalStateException("unknown class name: " + className);
	}

	private long deserializeWomiId(String[] fields) {
		if (fields.length < 2) {
			throw new InvalidParameterException("too few fields in record");
		}
		String idString = fields[1];
		if (!NumberUtils.isNumber(idString)) {
			throw new InvalidParameterException("id must be a number, '" + idString + "' given");
		}
		return Long.parseLong(idString);
	}

	private IdAndVersion deserializeIdAndVersion(String[] fields) {
		if (fields.length < 3) {
			throw new InvalidParameterException("too few fields in record");
		}
		String id = fields[1];
		String versionString = fields[2];
		if (!NumberUtils.isNumber(versionString)) {
			throw new InvalidParameterException("version must be a number, '" + versionString + "' given");
		}
		long version = Long.parseLong(versionString);
		return new IdAndVersion(id, version);
	}

	private Set<String> serialize(List<Task> tasks) {
		Set<String> lines = new HashSet<>();
		for (Task task : tasks) {
			lines.add(serialize(task));
		}
		return lines;
	}

	private String serialize(Task task) {
		List<Object> attributes = new LinkedList<>();
		attributes.add(task.getSerializeTo().getSimpleName());
		if (task instanceof CollectionTask) {
			attributes.add(((CollectionTask) task).getCollectionDescriptor().getId());
			attributes.add(((CollectionTask) task).getCollectionDescriptor().getVersion());
		} else if (task instanceof WomiModifiedTask) {
			attributes.add(((WomiModifiedTask) task).getWomiId());
		}
		return StringUtils.join(attributes, CSV_FIELD_SEPARATOR);
	}
}
