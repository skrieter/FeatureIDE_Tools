package de.ovgu.featureide.fm.benchmark.streams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import de.ovgu.featureide.fm.benchmark.util.Logger;

public class StreamRedirector implements Runnable {

	private final InputStream in;
	private final PrintStream out;

	public StreamRedirector(InputStream in, PrintStream out) {
		this.in = in;
		this.out = out;
	}

	@Override
	public void run() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				out.println(line);
			}
		} catch (IOException e) {
			Logger.getInstance().logError(e);
		}
	}

}
