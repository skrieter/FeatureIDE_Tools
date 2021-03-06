package de.ovgu.featureide.fm.benchmark.process;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sk.utils.Logger;

import de.ovgu.featureide.fm.benchmark.streams.ErrStreamCollector;
import de.ovgu.featureide.fm.benchmark.streams.ErrStreamReader;
import de.ovgu.featureide.fm.benchmark.streams.OutStreamReader;
import de.ovgu.featureide.fm.benchmark.streams.StreamRedirector;

public class ProcessRunner<R, A extends Algorithm<R>, K extends Result<R>> {

	private long timeout = Long.MAX_VALUE;

	public void run(A algorithm, K result) {
		boolean terminatedInTime = false;
		long startTime = 0, endTime = 0;
		try {
			System.gc();
			algorithm.preProcess();

			Logger.getInstance().logInfo(algorithm.getCommand(), 1);

			final List<String> command = algorithm.getCommandElements();
			if (!command.isEmpty()) {
				final ProcessBuilder processBuilder = new ProcessBuilder(command);
				Process process = null;

				final ErrStreamCollector errStreamCollector = new ErrStreamCollector();
				final StreamRedirector errRedirector = new StreamRedirector(
						Arrays.asList(new ErrStreamReader(), errStreamCollector));
				final StreamRedirector outRedirector = new StreamRedirector(
						Arrays.asList(new OutStreamReader(), algorithm));
				final Thread outThread = new Thread(outRedirector);
				final Thread errThread = new Thread(errRedirector);
				try {
					startTime = System.nanoTime();
					process = processBuilder.start();

					outRedirector.setInputStream(process.getInputStream());
					errRedirector.setInputStream(process.getErrorStream());
					outThread.start();
					errThread.start();

					terminatedInTime = process.waitFor(timeout, TimeUnit.MILLISECONDS);
					endTime = System.nanoTime();
					result.setTerminatedInTime(terminatedInTime);
					result.setNoError(errStreamCollector.getErrList().isEmpty());
					result.setTime((endTime - startTime) / 1_000_000L);
				} finally {
					if (process != null) {
						process.destroyForcibly();
					}
				}
			} else {
				result.setTerminatedInTime(false);
				result.setNoError(false);
				result.setTime(Result.INVALID_TIME);
			}
		} catch (Exception e) {
			Logger.getInstance().logError(e, 1);
			result.setTerminatedInTime(false);
			result.setNoError(false);
			result.setTime(Result.INVALID_TIME);
		}
		try {
			setResult(algorithm, result);
		} catch (Exception e) {
			Logger.getInstance().logError(e, 1);
			if (terminatedInTime) {
				result.setNoError(false);
			}
		}
		try {
			algorithm.postProcess();
		} catch (Exception e) {
			Logger.getInstance().logError(e);
		}
	}

	protected void setResult(A algorithm, K result) throws IOException {
		result.setResult(algorithm.parseResults());
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
}
