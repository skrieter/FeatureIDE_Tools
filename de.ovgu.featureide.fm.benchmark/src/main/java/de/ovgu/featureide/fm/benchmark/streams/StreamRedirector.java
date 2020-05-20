package de.ovgu.featureide.fm.benchmark.streams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.sk.utils.Logger;

public class StreamRedirector implements Runnable {

	private final List<IOutputReader> outputReaderList;
	private InputStream in;

	public StreamRedirector(List<IOutputReader> outputReaderList) {
		this.outputReaderList = outputReaderList;
	}

	public void setInputStream(InputStream in) {
		this.in = in;
	}

	@Override
	public void run() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				for (IOutputReader outputReader : outputReaderList) {
					try {
						outputReader.readOutput(line);
					} catch (Exception e) {
					}
				}
			}
		} catch (IOException e) {
			Logger.getInstance().logError(e);
		}
	}

}
