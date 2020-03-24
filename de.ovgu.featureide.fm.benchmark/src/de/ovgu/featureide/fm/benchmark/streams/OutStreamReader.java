package de.ovgu.featureide.fm.benchmark.streams;

import de.ovgu.featureide.fm.benchmark.util.Logger;

public class OutStreamReader implements IOutputReader {

	@Override
	public void readOutput(String line) throws Exception {
		Logger.getInstance().logInfo(line, 1);
		
	}

}
