/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.benchmark;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

import de.ovgu.featureide.fm.benchmark.process.Algorithm;
import de.ovgu.featureide.fm.benchmark.process.ProcessRunner;
import de.ovgu.featureide.fm.benchmark.process.ProcessRunner.Result;
import de.ovgu.featureide.fm.benchmark.properties.IProperty;
import de.ovgu.featureide.fm.benchmark.util.CSVWriter;
import de.ovgu.featureide.fm.benchmark.util.FeatureModelReader;
import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.base.IFeatureModel;

/**
 * @author Sebastian Krieter
 */
public abstract class ABenchmark<A extends Algorithm> {

	protected final BenchmarkConfig config;

	private final CSVWriter dataCSVWriter = new CSVWriter();
	private final CSVWriter modelCSVWriter = new CSVWriter();
	private final CSVWriter algorithmCSVWriter = new CSVWriter();

	public ABenchmark(String configPath) throws Exception {
		config = new BenchmarkConfig(configPath);
		init();
	}

	public void init() throws Exception {
		try {
			Files.createDirectories(config.outputPath);
			Files.createDirectories(config.csvPath);
			Files.createDirectories(config.tempPath);
			Logger.getInstance().install(config.outputPath, config.verbosity.getValue());
		} catch (IOException e) {
			Logger.getInstance().logError("Could not create output directory.");
			Logger.getInstance().logError(e);
			throw e;
		}

		initCSVWriter(dataCSVWriter, "data.csv", Arrays.asList("ModelID", "AlgorithmID", "SystemIteration",
				"AlgorithmIteration", "InTime", "NoError", "Time"));
		initCSVWriter(modelCSVWriter, "models.csv", Arrays.asList("ModelID", "Name"));
		initCSVWriter(algorithmCSVWriter, "algorithms.csv", Arrays.asList("AlgorithmID", "Name", "Settings"));
		initCSVWriters();
		dataCSVWriter.flush();
		modelCSVWriter.flush();
		algorithmCSVWriter.flush();
	}

	public void dispose() {
		Logger.getInstance().uninstall();
		if (!config.debug.getValue()) {
			deleteTempFolder();
		}
	}

	private void deleteTempFolder() {
		try {
			Files.walkFileTree(config.tempPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.deleteIfExists(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public final IFeatureModel init(final String name) {
		FeatureModelReader featureModelReader = new FeatureModelReader();
		return featureModelReader.read(name);
	}

	public final void run() {
		printConfigFile();
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int startSystemIndex = 0;

		Path progressFile;
		if (config.enableBreaks.getValue()) {
			try {
				progressFile = config.configPath.resolve(".progress");
				if (Files.exists(progressFile)) {
					List<String> lines = Files.readAllLines(progressFile);
					if (!lines.isEmpty()) {
						startSystemIndex = Integer.parseInt(lines.get(0).trim());
					}
				}
			} catch (Exception e) {
				progressFile = null;
			}
		} else {
			progressFile = null;
		}

		Logger.getInstance().logInfo("Start");

		final ProcessRunner processRunner = new ProcessRunner();
		processRunner.setTimeout(config.timeout.getValue());

		final List<String> systemNames = config.systemNames.subList(startSystemIndex, config.systemNames.size());
		int systemIndex = startSystemIndex;

		systemLoop: for (String systemName : systemNames) {
			if (progressFile != null) {
				try {
					Files.write(progressFile, Integer.toString(startSystemIndex).getBytes());
				} catch (IOException e) {
					Logger.getInstance().logError(e);
				}
			}
			systemIndex++;
			logSystem(systemName, systemIndex);
			final List<A> algorithms;
			try {
				algorithms = prepareAlgorithms(systemIndex);
			} catch (Exception e) {
				continue systemLoop;
			}
			for (int systemIteration = 1; systemIteration <= config.systemIterations.getValue(); systemIteration++) {
				try {
					prepareModel(systemName, systemIndex, systemIteration);
				} catch (Exception e) {
					continue systemLoop;
				}
				int algorithmIndex = 0;
				algorithmLoop: for (A algorithm : algorithms) {
					for (int algorithmIteration = 1; algorithmIteration <= config.algorithmIterations
							.getValue(); algorithmIteration++) {
						dataCSVWriter.createNewLine();
						dataCSVWriter.addValue(systemIndex);
						dataCSVWriter.addValue(algorithmIndex);
						dataCSVWriter.addValue(systemIteration);
						dataCSVWriter.addValue(algorithmIteration);
						logRun(algorithms, systemIteration, algorithmIndex, algorithm, algorithmIteration);
						Result result = processRunner.run(algorithm, dataCSVWriter);
						dataCSVWriter.addValue(result.isTerminatedInTime());
						dataCSVWriter.addValue(result.isNoError());
						dataCSVWriter.addValue(result.getTime());
						try {
							algorithm.parseResults();
							writeData(systemName, algorithm, systemIteration, algorithmIteration, dataCSVWriter);
						} catch (Exception e) {
							dataCSVWriter.resetLine();
							continue algorithmLoop;
						}
						dataCSVWriter.flush();
					}
					algorithmIndex++;
				}
			}
		}

		if (progressFile != null) {
			try {
				Files.deleteIfExists(progressFile);
			} catch (IOException e) {
				Logger.getInstance().logError(e);
			}
		}

		Logger.getInstance().logInfo("Finished");
	}

	private void logSystem(String systemName, int systemIndex) {
		StringBuilder sb = new StringBuilder();
		sb.append("Processing System: ");
		sb.append(systemName);
		sb.append(" (");
		sb.append(systemIndex);
		sb.append("/");
		sb.append(config.systemNames.size());
		sb.append(")");
		Logger.getInstance().logInfo(sb.toString(), 1, false);
	}

	private void logRun(List<A> algorithms, int systemIteration, int algorithmIndex, A algorithm,
			int algorithmIteration) {
		StringBuilder sb = new StringBuilder();
		sb.append(systemIteration);
		sb.append("/");
		sb.append(config.systemIterations.getValue());
		sb.append(" | ");
		sb.append(algorithm.getFullName());
		sb.append(" (");
		sb.append(algorithmIndex + 1);
		sb.append("/");
		sb.append(algorithms.size());
		sb.append(") | ");
		sb.append(algorithmIteration);
		sb.append("/");
		sb.append(config.algorithmIterations.getValue());
		Logger.getInstance().logInfo(sb.toString(), 1, false);
	}

	protected abstract void prepareModel(String systemName, int id, int systemIteration) throws Exception;

	protected abstract List<A> prepareAlgorithms(int systemIndex) throws Exception;

	protected void initCSVWriters() {
	};

	protected void writeData(String systemName, A algorithm, int systemIteration, int algorithmIteration,
			CSVWriter dataCSVWriter) {
	}

	protected CSVWriter createCSVWriter(String fileName, List<String> csvHeader) {
		CSVWriter csvWriter = new CSVWriter();
		initCSVWriter(csvWriter, fileName, csvHeader);
		csvWriter.flush();
		return csvWriter;
	}

	protected final void extendDataCSVWriter(List<String> csvHeader) {
		extendCSVWriter(dataCSVWriter, csvHeader);
	}

	protected final void extendModelCSVWriter(List<String> csvHeader) {
		extendCSVWriter(modelCSVWriter, csvHeader);
	}

	protected final void extendAlgorithmCSVWriter(List<String> csvHeader) {
		extendCSVWriter(algorithmCSVWriter, csvHeader);
	}

	public CSVWriter getDataCSVWriter() {
		return dataCSVWriter;
	}

	public CSVWriter getModelCSVWriter() {
		return modelCSVWriter;
	}

	public CSVWriter getAlgorithmCSVWriter() {
		return algorithmCSVWriter;
	}

	private void initCSVWriter(CSVWriter csvWriter, String fileName, List<String> csvHeader) {
		csvWriter.setOutputPath(config.csvPath);
		csvWriter.setFileName(fileName);
		csvWriter.setKeepLines(false);
		csvWriter.setHeader(csvHeader);
	}

	private void extendCSVWriter(CSVWriter csvWriter, List<String> csvHeader) {
		for (String headerValue : csvHeader) {
			csvWriter.addHeaderValue(headerValue);
		}
	}

	private void printConfigFile() {
		for (IProperty prop : BenchmarkConfig.propertyList) {
			StringBuilder sb = new StringBuilder();
			sb.append("\t").append(prop.getKey()).append(" = ");
			sb.append(prop.getValue().toString());
			Logger.getInstance().logInfo(sb.toString());
		}
	}

}
