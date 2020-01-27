package de.ovgu.featureide.fm.benchmark.process;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ovgu.featureide.fm.benchmark.streams.StreamRedirector;
import de.ovgu.featureide.fm.benchmark.streams.StreamRedirector2;
import de.ovgu.featureide.fm.benchmark.util.Logger;

public class ProcessRunner {

	private long timeout = Long.MAX_VALUE;

	public <R> void run(Algorithm<R> algorithm, Result<R> result) {
		try {
			System.gc();
			algorithm.preProcess();
			final List<String> command = algorithm.getCommand();
			Logger.getInstance().logInfo(command.toString(), 1, true);

			final ProcessBuilder processBuilder = new ProcessBuilder(command);
			Process process = null;
			long startTime = 0, endTime = 0;
			try {
				startTime = System.nanoTime();
				process = processBuilder.start();

				new Thread(new StreamRedirector2(process.getInputStream(), System.out, algorithm)).start();
				new Thread(new StreamRedirector(process.getErrorStream(), System.err)).start();
				boolean terminatedInTime = process.waitFor(timeout, TimeUnit.MILLISECONDS);
				endTime = System.nanoTime();
				result.setTerminatedInTime(terminatedInTime);
				result.setNoError(true);
				result.setTime((endTime - startTime) / 1_000_000L);
			} catch (Exception e) {
				Logger.getInstance().logError(e);
			} finally {
				if (process != null) {
					process.destroyForcibly();
				}
			}
		} catch (Exception e) {
			Logger.getInstance().logError(e);
		}
		try {
			result.setResult(algorithm.parseResults());
		} catch (Exception e) {
			Logger.getInstance().logError(e);
		}
		try {
			algorithm.postProcess();
		} catch (Exception e) {
			Logger.getInstance().logError(e);
		}
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
}
