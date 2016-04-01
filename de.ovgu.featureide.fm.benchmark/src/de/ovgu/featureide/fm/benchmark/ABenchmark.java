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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import de.ovgu.featureide.fm.benchmark.properties.IProperty;
import de.ovgu.featureide.fm.benchmark.properties.Seed;
import de.ovgu.featureide.fm.core.FMCorePlugin;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.io.UnsupportedModelException;
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelReader;

/**
 * @author Sebastian Krieter
 */
public abstract class ABenchmark {

	private static final String MODELS_DIRECTORY = "de/ovgu/featureide/fm/benchmark/models";
	private static final String CONFIG_DIRECTORY = "config/";

	private static final String COMMENT = "#";
	private static final String STOP_MARK = "###";

	private static final Path MODELS_PATH;

	static {
		Path path = null;
		try {
			path = Paths.get(ClassLoader.getSystemResource(MODELS_DIRECTORY).toURI());
		} catch (URISyntaxException e) {
		}
		MODELS_PATH = path;
	}

	protected final ProgressLogger logger = new ProgressLogger();
	protected final CSVWriter csvWriter = new CSVWriter();

	protected final List<String> modelNames;
	private final Random randSeed;
	private final Seed seed = new Seed();

	private final static FeatureModel init(final String name) {
		FeatureModel fm = new FeatureModel();

		Path p = MODELS_PATH.resolve(name).resolve("model.xml");
		if (Files.exists(p)) {
			try {
				new XmlFeatureModelReader(fm).readFromFile(p.toFile());
			} catch (FileNotFoundException | UnsupportedModelException e) {
				e.printStackTrace();
			}
		} else {
			throw new RuntimeException(p.toString());
		}

		return fm;
	}
	
	private final List<IProperty> propertyList = new LinkedList<>();
	
	protected void addProperty(IProperty property) {
		propertyList.add(property);
	}
	
	private void readProperties() {
		final Path path = Paths.get(CONFIG_DIRECTORY + "config.properties");
		logger.print("Reading config file. (" + path.toString()+ ") ... ");
		final Properties properties = new Properties();
		try {
			properties.load(Files.newInputStream(path));
			logger.println("Success!");
		} catch (IOException e) {
			logger.println("Fail! -> " + e.getMessage());
		}
		for (IProperty prop : propertyList) {
			logger.print("\t" + prop.getKey() + " = ");
			boolean success = prop.setValue(properties.getProperty(prop.getKey()));
			logger.print(prop.getValue().toString());
			logger.println(success ? "" : " (default value!)");
		}
	}

	protected abstract void createProperties();
	
	public ABenchmark() {
		addProperty(seed);
		createProperties();
		readProperties();

		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(CONFIG_DIRECTORY + "models.txt"), Charset.defaultCharset());
		} catch (IOException e) {
			logger.println("No feature models specified!");
			FMCorePlugin.getDefault().logError(e);
		}

		if (lines != null) {
			boolean pause = false;
			modelNames = new ArrayList<>(lines.size());
			for (String modelName : lines) {
				modelName = modelName.trim();
				if (!modelName.isEmpty()) {
					if (modelName.startsWith(COMMENT)) {
						if (modelName.equals(STOP_MARK)) {
							pause = !pause;
						}
					} else if (!pause) {
						modelNames.add(modelName);
					}
				}
			}
		} else {
			modelNames = Collections.<String> emptyList();
		}

		randSeed = new Random(seed.getValue());
	}

	protected final long getNextSeed() {
		return randSeed.nextLong();
	}

	protected final FeatureModel initModel(int index) {
		return init(modelNames.get(index));
	}

}
