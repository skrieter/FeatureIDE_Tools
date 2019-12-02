package de.ovgu.featureide.fm.benchmark.streams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import de.ovgu.featureide.fm.benchmark.process.Algorithm;
import de.ovgu.featureide.fm.benchmark.util.Logger;

public class StreamRedirector2 implements Runnable {

	private final InputStream in;
	private final PrintStream out;
	private final Algorithm algorithm;

	public StreamRedirector2(InputStream in, PrintStream out, Algorithm algorithm) {
		this.in = in;
		this.out = out;
		this.algorithm = algorithm;
	}

	@Override
	public void run() {

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				algorithm.parseOutput(line);
				out.print("\t");
				out.println(line);
			}
		} catch (IOException e) {
			Logger.getInstance().logError(e);
		}
	}

}
