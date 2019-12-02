package de.ovgu.featureide.fm.benchmark.process;

import java.util.List;

public interface Algorithm {

	void preProcess();

	List<String> getCommand();

	void postProcess();

	boolean parseResults();

	default void parseOutput(String line) {
	}

	String getName();

	String getParameterSettings();

	default String getFullName() {
		return getName() + "_" + getParameterSettings();
	}

}
