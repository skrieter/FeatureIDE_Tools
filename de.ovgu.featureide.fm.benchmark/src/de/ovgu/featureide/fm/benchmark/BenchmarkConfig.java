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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import de.ovgu.featureide.fm.benchmark.properties.IProperty;
import de.ovgu.featureide.fm.benchmark.properties.IntProperty;
import de.ovgu.featureide.fm.benchmark.properties.LongProperty;
import de.ovgu.featureide.fm.benchmark.properties.Seed;
import de.ovgu.featureide.fm.benchmark.properties.StringProperty;
import de.ovgu.featureide.fm.benchmark.util.CSVWriter;
import de.ovgu.featureide.fm.benchmark.util.FeatureModelReader;
import de.ovgu.featureide.fm.benchmark.util.Logger;

/**
 * @author Sebastian Krieter
 */
public class BenchmarkConfig {

	private static final String DEFAULT_RESOURCE_DIRECTORY = "resources";
	private static final String DEFAULT_MODELS_DIRECTORY = "models";
	private static final String DEFAULT_CONFIG_DIRECTORY = "config";

	private static final String COMMENT = "#";
	private static final String STOP_MARK = "###";

	protected static final List<IProperty> propertyList = new LinkedList<>();

	protected final StringProperty outputPathProperty = new StringProperty("output");
	protected final StringProperty modelsPathProperty = new StringProperty("models");
	protected final StringProperty resourcesPathProperty = new StringProperty("resources");

	protected final IntProperty debug = new IntProperty("debug");
	protected final IntProperty enableBreaks = new IntProperty("enableBreaks");
	protected final IntProperty verbosity = new IntProperty("verbosity");
	protected final LongProperty timeout = new LongProperty("timeout", Long.MAX_VALUE);
	public final Seed randomSeed = new Seed();

	protected final IntProperty systemIterations = new IntProperty("systemIterations", 1);
	protected final IntProperty algorithmIterations = new IntProperty("algorithmIterations", 1);

	protected final CSVWriter csvWriter = new CSVWriter();
	protected final FeatureModelReader featureModelReader = new FeatureModelReader();

	public Path configPath;
	public Path outputPath;
	public Path modelPath;
	public Path resourcePath;
	public Path csvPath;
	public Path tempPath;
	protected List<String> systemNames;

	public static void addProperty(IProperty property) {
		propertyList.add(property);
	}

	public BenchmarkConfig() {
		this(null);
	}

	public BenchmarkConfig(String configPath) {
		initConfigPath(configPath);
		initOutputPath();
		initModelPath();
	}

	private void initConfigPath(String configPath) {
		try {
			if (configPath != null) {
				this.configPath = Paths.get(configPath).resolve("config.properties");
			} else {
				this.configPath = Paths.get(DEFAULT_CONFIG_DIRECTORY).resolve("config.properties");
			}
			readConfigFile(this.configPath);
		} catch (Exception e) {
		}
	}

	private void initOutputPath() {
		outputPath = Paths.get(((outputPathProperty.getValue().isEmpty()) ? "output" : outputPathProperty.getValue())
				+ File.separator + (Long.MAX_VALUE - System.currentTimeMillis()));
		csvPath = outputPath.resolve("data");
		tempPath = outputPath.resolve("temp");
	}

	private void initModelPath() {
		resourcePath = Paths.get((resourcesPathProperty.getValue().isEmpty()) ? DEFAULT_RESOURCE_DIRECTORY
				: resourcesPathProperty.getValue());

		modelPath = resourcePath.resolve(
				(modelsPathProperty.getValue().isEmpty()) ? DEFAULT_MODELS_DIRECTORY : modelsPathProperty.getValue());

		List<String> lines = null;
		try {
			lines = Files.readAllLines(configPath.resolve("models.txt"), Charset.defaultCharset());
		} catch (IOException e) {
			Logger.getInstance().logError("No feature models specified!");
		}

		if (lines != null) {
			boolean pause = false;
			systemNames = new ArrayList<>(lines.size());
			for (String modelName : lines) {
				// modelName = modelName.trim();
				if (!modelName.trim().isEmpty()) {
					if (!modelName.startsWith("\t")) {

						if (modelName.startsWith(COMMENT)) {
							if (modelName.equals(STOP_MARK)) {
								pause = !pause;
							}
						} else if (!pause) {
							systemNames.add(modelName.trim());
						}
					}
				}
			}
		} else {
			systemNames = Collections.<String>emptyList();
		}
	}

	private Properties readConfigFile(final Path path) throws Exception {
		Logger.getInstance().logInfo("Reading config file. (" + path.toString() + ") ... ");
		final Properties properties = new Properties();
		try {
			properties.load(Files.newInputStream(path));
			for (IProperty prop : propertyList) {
				prop.setValue(properties.getProperty(prop.getKey()));
			}
			Logger.getInstance().logInfo("Success!");
			return properties;
		} catch (IOException e) {
			Logger.getInstance().logInfo("Fail! -> " + e.getMessage());
			throw e;
		}
	}

}
