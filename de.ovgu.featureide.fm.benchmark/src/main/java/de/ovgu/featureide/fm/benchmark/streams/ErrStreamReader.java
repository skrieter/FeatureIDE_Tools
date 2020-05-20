package de.ovgu.featureide.fm.benchmark.streams;

import org.sk.utils.Logger;

public class ErrStreamReader implements IOutputReader {

	@Override
	public void readOutput(String line) throws Exception {
		Logger.getInstance().logError(line, 1);
	}

}
