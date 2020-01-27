package de.ovgu.featureide.fm.benchmark.process;

public class Result<R> {

	private boolean terminatedInTime = false;
	private boolean noError = false;
	private long time = -1;
	private R result = null;

	public boolean isTerminatedInTime() {
		return terminatedInTime;
	}

	public void setTerminatedInTime(boolean terminatedInTime) {
		this.terminatedInTime = terminatedInTime;
	}

	public boolean isNoError() {
		return noError;
	}

	public void setNoError(boolean noError) {
		this.noError = noError;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public R getResult() {
		return result;
	}

	public void setResult(R result) {
		this.result = result;
	}

}
