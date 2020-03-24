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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

import de.ovgu.featureide.fm.benchmark.properties.IProperty;
import de.ovgu.featureide.fm.benchmark.util.CSVWriter;
import de.ovgu.featureide.fm.benchmark.util.Logger;

/**
 * @author Sebastian Krieter
 */
public abstract class ABenchmark {

	protected final BenchmarkConfig config;

	private final LinkedHashMap<String, CSVWriter> csvWriterList = new LinkedHashMap<>();
	
	protected int systemID;
	protected int systemIteration;

	public ABenchmark(String configPath, String configName) throws Exception {
		config = new BenchmarkConfig(configPath);
		config.readConfig(configName);
	}

	public void init() throws Exception {
		setupDirectories();
		addCSVWriters();
		for (CSVWriter writer : csvWriterList.values()) {
			writer.flush();
		}
		Logger.getInstance().logInfo("Running " + this.getClass().getSimpleName(), false);
	}

	protected void setupDirectories() throws IOException {
		config.setup();
		try {
			Files.createDirectories(config.outputPath);
			Files.createDirectories(config.csvPath);
			Files.createDirectories(config.tempPath);
			Files.createDirectories(config.logPath);
			Logger.getInstance().install(config.logPath, config.verbosity.getValue());
		} catch (IOException e) {
			Logger.getInstance().logError("Could not create output directory.");
			Logger.getInstance().logError(e);
			throw e;
		}
	}

	protected void addCSVWriters() {
	};

	public void dispose() {
		Logger.getInstance().uninstall();
		if (config.debug.getValue() == 0) {
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

	public void run() {
		printConfigFile();
	}

	private void printConfigFile() {
		for (IProperty prop : BenchmarkConfig.propertyList) {
			Logger.getInstance().logInfo(prop.toString(), 1, false);
		}
	}
	
	protected void logSystem() {
		StringBuilder sb = new StringBuilder();
		sb.append("Processing System: ");
		sb.append(config.systemNames.get(systemID));
		sb.append(" (");
		sb.append(systemID + 1);
		sb.append("/");
		sb.append(config.systemNames.size());
		sb.append(")");
		Logger.getInstance().logInfo(sb.toString(), 1, false);
	}

	protected CSVWriter addCSVWriter(String fileName, List<String> csvHeader) {
		final CSVWriter existingCSVWriter = csvWriterList.get(fileName);
		if (existingCSVWriter == null) {
			CSVWriter csvWriter = new CSVWriter();
			csvWriter.setAppend(config.append.getValue());
			csvWriter.setOutputPath(config.csvPath);
			csvWriter.setFileName(fileName);
			csvWriter.setKeepLines(false);
			csvWriter.setHeader(csvHeader);
			csvWriterList.put(fileName, csvWriter);
			return csvWriter;
		} else {
			return existingCSVWriter;
		}
	}

	protected void extendCSVWriter(String fileName, List<String> csvHeader) {
		final CSVWriter existingCSVWriter = csvWriterList.get(fileName);
		if (existingCSVWriter != null) {
			extendCSVWriter(existingCSVWriter, csvHeader);
		}
	}

	protected void extendCSVWriter(CSVWriter writer, List<String> csvHeader) {
		for (String headerValue : csvHeader) {
			writer.addHeaderValue(headerValue);
		}
	}
	
	protected final void writeCSV(CSVWriter writer, Consumer<CSVWriter> writing) {
		writer.createNewLine();
		try {
			writing.accept(writer);
		} catch (Exception e) {
			writer.resetLine();
			throw e;
		}
		writer.flush();
	}

}
