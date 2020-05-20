package de.ovgu.featureide.fm.benchmark.streams;

import java.util.ArrayList;
import java.util.List;

public class ErrStreamCollector implements IOutputReader {

	private final List<String> errList = new ArrayList<>();

	@Override
	public void readOutput(String line) throws Exception {
		errList.add(line);
	}

	public List<String> getErrList() {
		return errList;
	}

}
