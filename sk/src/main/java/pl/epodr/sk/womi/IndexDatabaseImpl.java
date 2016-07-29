package pl.epodr.sk.womi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import pl.epodr.sk.IdAndVersion;

@Service
public class IndexDatabaseImpl implements IndexDatabase {

	private static final Logger logger = LoggerFactory.getLogger(IndexDatabase.class);

	private static final Logger indexLogger = LoggerFactory.getLogger("indexLogger");

	private static final String DATABASE_FILENAME = "indexDb.dat";

	private static final String WOMI_MARKUP = "w";

	private static final String COLL_ID_VERSION_SEPARATOR = "_";

	private final Map<Long, Set<IdAndVersion>> womiToCollectionsMap = new HashMap<>();

	@PostConstruct
	void init() {
		load();
		logger.info("I have " + womiToCollectionsMap.size() + " records in womis->collections relation");
	}

	@Override
	public Set<IdAndVersion> getCollectionsForWomi(long womiId) {
		return Collections.unmodifiableSet(ifnull(womiToCollectionsMap.get(womiId)));
	}

	@Override
	public void putCollectionForWomi(IdAndVersion coll, long womiId) {
		indexLogger.info("womi->collection: " + womiId + "-" + coll);
		synchronized (womiToCollectionsMap) {
			addToMap(womiToCollectionsMap, womiId, coll);
		}
	}

	@Override
	public void removeCollection(IdAndVersion coll) {
		synchronized (womiToCollectionsMap) {
			for (long womiId : womiToCollectionsMap.keySet()) {
				womiToCollectionsMap.get(womiId).remove(coll);
			}
		}
	}

	private Set<IdAndVersion> ifnull(Set<IdAndVersion> set) {
		if (set == null) {
			return new HashSet<>();
		}
		return set;
	}

	private void addToMap(Map<Long, Set<IdAndVersion>> map, long key, IdAndVersion val) {
		Set<IdAndVersion> set = map.get(key);
		if (set == null) {
			set = new HashSet<>();
			map.put(key, set);
		}
		set.add(val);
	}

	@Override
	@Async
	public void commit() {
		indexLogger.info("commit");
		save();
	}

	private void save() {
		synchronized (womiToCollectionsMap) {
			try (PrintStream p = new PrintStream(DATABASE_FILENAME)) {
				saveMap(womiToCollectionsMap, p, WOMI_MARKUP);
			} catch (IOException e) {
				logger.error("when saving the index database", e);
			}
		}
	}

	private void saveMap(Map<Long, Set<IdAndVersion>> map, PrintStream p, String markup) {
		for (long key : map.keySet()) {
			Set<String> values = new HashSet<>();
			for (IdAndVersion coll : map.get(key)) {
				values.add(collectionDescriptorToString(coll));
			}
			if (!values.isEmpty()) {
				String vals = StringUtils.join(values, ',');
				p.println(markup + ";" + key + ";" + vals);
			}
		}
	}

	private String collectionDescriptorToString(IdAndVersion coll) {
		return coll.getId() + COLL_ID_VERSION_SEPARATOR + coll.getVersion();
	}

	private synchronized void load() {
		File databaseFile = new File(DATABASE_FILENAME);
		try (BufferedReader r = new BufferedReader(new FileReader(databaseFile))) {
			String line;
			int i = 0;
			while ((line = r.readLine()) != null) {
				i++;
				line = line.trim();
				if (line.length() > 0) {
					parseLine(line, i);
				}
			}
		} catch (FileNotFoundException e) {
			logger.error("database file not found: " + databaseFile.getAbsolutePath());
		} catch (IOException e) {
			logger.error("when loading the index database", e);
		}
	}

	private void parseLine(String line, int i) {
		line = line.replace(" ", "");
		String[] p = line.split(";");
		if (p.length != 3) {
			logger.error("invalid number of fields in line #" + i + ": " + line);
			return;
		}

		long a;
		try {
			a = Long.parseLong(p[1]);
		} catch (NumberFormatException e) {
			logger.error("wrong id in line #" + i + " - value " + p[1]);
			return;
		}

		Set<IdAndVersion> b = new HashSet<>();
		String[] bstr = p[2].split(",");
		for (int j = 0; j < bstr.length; j++) {
			if (bstr[j].length() > 0) {
				String val = bstr[j];
				try {
					b.add(parseThirdFieldElement(val));
				} catch (NumberFormatException e) {
					logger.error("wrong ids in line #" + i + " - value " + val);
				}
			}
		}

		if (b.isEmpty()) {
			return;
		}

		if (p[0].equals(WOMI_MARKUP)) {
			if (!womiToCollectionsMap.containsKey(a)) {
				womiToCollectionsMap.put(a, b);
			} else {
				Set<IdAndVersion> collections = womiToCollectionsMap.get(a);
				collections.addAll(b);
			}
		} else {
			logger.error("unsupported markup in line #" + i + ": " + p[0]);
		}
	}

	private IdAndVersion parseThirdFieldElement(String val) {
		String[] p = val.split(COLL_ID_VERSION_SEPARATOR);
		String id = p[0];
		long version;
		if (p.length == 2) {
			version = Long.parseLong(p[1]);
		} else {
			version = 1;
		}
		return new IdAndVersion(id, version);
	}
}
