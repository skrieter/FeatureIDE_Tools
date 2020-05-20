package de.ovgu.featureide.fm.benchmark;

import java.nio.file.Files;

public class OutputCleaner extends ABenchmark {

	public OutputCleaner(String configPath) throws Exception {
		super(configPath, null);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Configuration path not specified!");
		}
		final OutputCleaner evaluator = new OutputCleaner(args[0]);
		Files.deleteIfExists(evaluator.config.outputRootPath.resolve(".current"));
		System.out.println("Reset current output path.");
	}

}
