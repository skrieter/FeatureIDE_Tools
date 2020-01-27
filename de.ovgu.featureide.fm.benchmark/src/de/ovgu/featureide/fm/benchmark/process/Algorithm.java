package de.ovgu.featureide.fm.benchmark.process;

import java.util.List;
import java.util.Objects;

public abstract class Algorithm<R> {

	public abstract void preProcess() throws Exception;

	public abstract List<String> getCommand();

	public abstract void postProcess() throws Exception;

	public abstract R parseResults() throws Exception;

	public void parseOutput(String line) throws Exception {
	}

	public abstract String getName();

	public abstract String getParameterSettings();

	public String getFullName() {
		return getName() + "_" + getParameterSettings();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		final Algorithm<?> other = (Algorithm<?>) obj;
		return Objects.equals(getFullName(), other.getFullName());
	}

	@Override
	public int hashCode() {
		return getFullName().hashCode();
	}

	@Override
	public String toString() {
		return getFullName();
	}


}
