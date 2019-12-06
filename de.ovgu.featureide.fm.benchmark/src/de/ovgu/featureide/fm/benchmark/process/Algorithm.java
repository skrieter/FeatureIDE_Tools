package de.ovgu.featureide.fm.benchmark.process;

import java.util.List;

public interface Algorithm {

	void preProcess() throws Exception;

	List<String> getCommand();

	void postProcess() throws Exception;

	boolean parseResults() throws Exception;

	default void parseOutput(String line) throws Exception {
	}

	String getName();

	String getParameterSettings();

	default String getFullName() {
		return getName() + "_" + getParameterSettings();
	}

}
