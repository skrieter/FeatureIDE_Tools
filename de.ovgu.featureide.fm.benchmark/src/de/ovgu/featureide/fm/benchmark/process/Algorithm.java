package de.ovgu.featureide.fm.benchmark.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.ovgu.featureide.fm.benchmark.streams.IOutputReader;

public abstract class Algorithm<R> implements IOutputReader {
	
	protected int iterations = -1;

	protected final ArrayList<String> commandElements = new ArrayList<>();

	public abstract void postProcess() throws Exception;

	public abstract R parseResults() throws IOException;

	public void readOutput(String line) throws Exception {
	}

	public abstract String getName();

	public abstract String getParameterSettings();

	public void preProcess() throws Exception {
		commandElements.clear();
		addCommandElements();
	}

	protected abstract void addCommandElements() throws Exception;

	public void addCommandElement(String parameter) {
		commandElements.add(parameter);
	}

	public List<String> getCommandElements() {
		return commandElements;
	}

	public String getCommand() {
		StringBuilder commandBuilder = new StringBuilder();
		for (String commandElement : commandElements) {
			commandBuilder.append(commandElement);
			commandBuilder.append(' ');
		}
		return commandBuilder.toString();
	}

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

	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

}
