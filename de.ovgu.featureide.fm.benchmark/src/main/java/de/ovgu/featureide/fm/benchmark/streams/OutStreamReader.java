package de.ovgu.featureide.fm.benchmark.streams;

import org.sk.utils.Logger;

public class OutStreamReader implements IOutputReader {

	@Override
	public void readOutput(String line) throws Exception {
		Logger.getInstance().logInfo(line);
		
	}

}
