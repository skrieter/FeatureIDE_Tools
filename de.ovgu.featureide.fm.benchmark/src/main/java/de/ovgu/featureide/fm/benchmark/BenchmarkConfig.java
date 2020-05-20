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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.sk.utils.Logger;
import org.sk.utils.io.NameListReader;

import de.ovgu.featureide.fm.benchmark.properties.BoolProperty;
import de.ovgu.featureide.fm.benchmark.properties.IProperty;
import de.ovgu.featureide.fm.benchmark.properties.IntProperty;
import de.ovgu.featureide.fm.benchmark.properties.LongProperty;
import de.ovgu.featureide.fm.benchmark.properties.Seed;
import de.ovgu.featureide.fm.benchmark.properties.StringProperty;

/**
 * @author Sebastian Krieter
 */
public class BenchmarkConfig {

	private static final String DEFAULT_RESOURCE_DIRECTORY = "resources";
	private static final String DEFAULT_MODELS_DIRECTORY = "models";
	private static final String DEFAULT_CONFIG_DIRECTORY = "config";

	protected static final List<IProperty> propertyList = new LinkedList<>();

	public final StringProperty outputPathProperty = new StringProperty("output");
	public final StringProperty modelsPathProperty = new StringProperty("models");
	public final StringProperty resourcesPathProperty = new StringProperty("resources");

	public final BoolProperty append = new BoolProperty("append");
	public final IntProperty debug = new IntProperty("debug");
	public final IntProperty verbosity = new IntProperty("verbosity");
	public final LongProperty timeout = new LongProperty("timeout", Long.MAX_VALUE);
	public final Seed randomSeed = new Seed();

	public final IntProperty systemIterations = new IntProperty("systemIterations", 1);
	public final IntProperty algorithmIterations = new IntProperty("algorithmIterations", 1);

	public Path configPath;
	public Path outputPath;
	public Path outputRootPath;
	public Path modelPath;
	public Path resourcePath;
	public Path csvPath;
	public Path tempPath;
	public Path logPath;
	public List<String> systemNames;
	public List<Integer> systemIDs;

	public static void addProperty(IProperty property) {
		propertyList.add(property);
	}

	public BenchmarkConfig() {
		this.configPath = Paths.get(DEFAULT_CONFIG_DIRECTORY);
	}

	public BenchmarkConfig(String configPath) {
		this.configPath = Paths.get(configPath);
	}

	public void readConfig(String name) {
		initConfigPath("paths");
		if (name != null) {
			initConfigPath(name);
		}
		initPaths();
	}

	private void initPaths() {
		outputRootPath = Paths
				.get((outputPathProperty.getValue().isEmpty()) ? "output" : outputPathProperty.getValue());
		resourcePath = Paths.get((resourcesPathProperty.getValue().isEmpty()) ? DEFAULT_RESOURCE_DIRECTORY
				: resourcesPathProperty.getValue());

		modelPath = resourcePath.resolve(
				(modelsPathProperty.getValue().isEmpty()) ? DEFAULT_MODELS_DIRECTORY : modelsPathProperty.getValue());
	}

	public void setup() {
		initOutputPath();
		readSystemNames();
	}

	private void initConfigPath(String configName) {
		try {
			readConfigFile(this.configPath.resolve(configName + ".properties"));
		} catch (Exception e) {
		}
	}

	private long getOutputID() {
		return Long.MAX_VALUE - System.currentTimeMillis();
	}

	private void initOutputPath() {
		Path currentOutputMarkerFile = outputRootPath.resolve(".current");
		String currentOutputMarker = null;
		if (Files.isReadable(currentOutputMarkerFile)) {
			List<String> lines;
			try {
				lines = Files.readAllLines(currentOutputMarkerFile);

				if (!lines.isEmpty()) {
					String firstLine = lines.get(0);
					currentOutputMarker = firstLine.trim();
				}
			} catch (Exception e) {
				Logger.getInstance().logError(e);
			}
		}
		if (currentOutputMarker == null) {
			currentOutputMarker = Long.toString(getOutputID());
			try {
				Files.write(currentOutputMarkerFile, currentOutputMarker.getBytes());
			} catch (IOException e) {
				Logger.getInstance().logError(e);
			}
		}
		outputPath = outputRootPath.resolve(currentOutputMarker);
		csvPath = outputPath.resolve("data");
		tempPath = outputPath.resolve("temp");
		logPath = outputPath.resolve("log-" + System.currentTimeMillis());
	}

	private void readSystemNames() {
		try {
			NameListReader nameListReader = new NameListReader();
			nameListReader.read(configPath.resolve("models.txt"));
			systemNames = nameListReader.getNames();
			systemIDs = nameListReader.getIDs();
		} catch (IOException e) {
			Logger.getInstance().logError("No feature models specified!");
			Logger.getInstance().logError(e);
		}
	}

	private Properties readConfigFile(final Path path) throws Exception {
		Logger.getInstance().logInfo("Reading config file. (" + path.toString() + ") ... ", 0);
		final Properties properties = new Properties();
		try {
			properties.load(Files.newInputStream(path));
			for (IProperty prop : propertyList) {
				String value = properties.getProperty(prop.getKey());
				if (value != null) {
					prop.setValue(value);
				}
			}
			Logger.getInstance().logInfo("Success!", 0);
			return properties;
		} catch (IOException e) {
			Logger.getInstance().logInfo("Fail! -> " + e.getMessage(), 0);
			Logger.getInstance().logError(e);
			throw e;
		}
	}

}
