package de.ovgu.featureide.fm.benchmark.streams;

import de.ovgu.featureide.fm.benchmark.util.Logger;

public class ErrStreamReader implements IOutputReader {

	@Override
	public void readOutput(String line) throws Exception {
		Logger.getInstance().logError(line, true);
	}

}
