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
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import de.ovgu.featureide.fm.benchmark.properties.IProperty;
import de.ovgu.featureide.fm.benchmark.properties.IntProperty;
import de.ovgu.featureide.fm.benchmark.properties.Seed;
import de.ovgu.featureide.fm.benchmark.properties.StringProperty;
import de.ovgu.featureide.fm.benchmark.properties.Timeout;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.init.LibraryManager;

/**
 * @author Sebastian Krieter
 */
public abstract class ABenchmark {
	
	static {
		LibraryManager.registerLibrary(FMCoreLibrary.getInstance());
	}

	private static final String MODEL_FILE = "model.xml";

	private static final List<IProperty> propertyList = new LinkedList<>();

	protected static abstract class ATestRunner<T> implements Runnable {

		private T result;

		@Override
		public void run() {
			result = null;
			try {
				result = execute();
			} catch (Throwable e) {
				if (!(e instanceof ThreadDeath)) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		}

		protected abstract T execute();

		public T getResult() {
			return result;
		}

	}

	private static final String DEFAULT_MODELS_DIRECTORY = "models";
	private static final String DEFAULT_CONFIG_DIRECTORY = "config";

	private static final String COMMENT = "#";
	private static final String STOP_MARK = "###";

	protected static final Timeout timeout = new Timeout();
	protected static final StringProperty outputPath = new StringProperty("output");
	protected static final StringProperty modelsPath = new StringProperty("models");
	protected static final Seed seed = new Seed();
	protected static final IntProperty verboseLevel = new IntProperty("verboseLevel");

	protected final CSVWriter csvWriter = new CSVWriter();
	private final Random randSeed;

	protected Path rootOutPath, pathToModels;
	protected List<String> modelNames;

	public final IFeatureModel init(final String name) {
		IFeatureModel fm = null;

		fm = lookUpFolder(pathToModels, name, fm);
		fm = lookUpFile(pathToModels, name, fm);
		fm = lookUpZip(pathToModels, name, fm);

		if (fm == null) {
			throw new RuntimeException("Model not found: " + name);
		} else {
			return fm;
		}
	}

	protected IFeatureModel loadFile(final Path path) {
		final FileHandler<IFeatureModel> fh = FeatureModelManager.getFileHandler(path);
		return fh.getLastProblems().containsError() ? null : fh.getObject();
	}

	protected IFeatureModel lookUpFolder(final Path rootPath, final String name, IFeatureModel fm) {
		if (fm != null) {
			return fm;
		} else {
			Path modelFolder = rootPath.resolve(name);
			if (Files.exists(modelFolder) && Files.isDirectory(modelFolder)) {
				final Path path = modelFolder.resolve(MODEL_FILE);
				if (Files.exists(path)) {
					return loadFile(path);
				} else {
					return lookUpFile(modelFolder, "model", fm);
				}
			} else {
				return null;
			}
		}
	}

	protected IFeatureModel lookUpFile(final Path rootPath, final String name, IFeatureModel fm) {
		if (fm != null) {
			return fm;
		} else {
			final Filter<Path> fileFilter = file -> Files.isReadable(file) && Files.isRegularFile(file)
					&& file.getFileName().toString().matches("^" + name + "\\.\\w+$");
			try (DirectoryStream<Path> files = Files.newDirectoryStream(rootPath, fileFilter)) {
				final Iterator<Path> iterator = files.iterator();
				while (iterator.hasNext()) {
					Path next = iterator.next();
					IFeatureModel loadedFm = loadFile(next);
					if (loadedFm != null) {
						return loadedFm;
					}
				}
				return null;
			} catch (IOException e) {
				printErr(e.getMessage());
			}
			return null;
		}
	}

	protected IFeatureModel lookUpZip(final Path rootPath, final String name, IFeatureModel fm) {
		if (fm != null) {
			return fm;
		} else {
			final Filter<Path> fileFilter = file -> Files.isReadable(file) && Files.isRegularFile(file)
					&& file.getFileName().toString().matches(".*[.]zip\\Z");
			try (DirectoryStream<Path> files = Files.newDirectoryStream(rootPath, fileFilter)) {
				for (Path path : files) {
					final URI uri = URI.create("jar:" + path.toUri().toString());
					try (final FileSystem zipFs = FileSystems.newFileSystem(uri,
							Collections.<String, Object>emptyMap())) {
						for (Path root : zipFs.getRootDirectories()) {
							fm = lookUpFolder(root, name, fm);
							fm = lookUpFile(root, name, fm);
						}
						if (fm != null) {
							return fm;
						}
					}
				}
			} catch (IOException e) {
				printErr(e.getMessage());
			}
			return null;
		}
	}

	public static void addProperty(IProperty property) {
		propertyList.add(property);
	}

	protected final <T> T run(ATestRunner<T> testRunner) {
		final Thread thread = new Thread(testRunner);
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return testRunner.getResult();
	}

	@SuppressWarnings("deprecation")
	protected final <T> T run(ATestRunner<T> testRunner, long timeout) {
		final Thread thread = new Thread(testRunner);
		Logger.getInstance().printOut("Start");
		thread.start();
		try {
			thread.join(timeout);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (thread.isAlive()) {
			thread.stop();
		}
		Logger.getInstance().printOut("Finished");
		return testRunner.getResult();
	}

	public ABenchmark() {
		this(null);
	}

	public ABenchmark(String configPath) {
		initConfigPath(configPath);
		initOutputPath();
		initModelPath();
		randSeed = new Random(seed.getValue());
	}

	public void dispose() {
		Logger.getInstance().uninstall();
	}

	private void initConfigPath(String configPath) {
		try {
			if (configPath != null) {
				readConfigFile(Paths.get(configPath).resolve("config.properties"));
			} else {
				readConfigFile(Paths.get(DEFAULT_CONFIG_DIRECTORY).resolve("config.properties"));
			}
		} catch (Exception e) {
		}
	}

	private static Properties readConfigFile(final Path path) throws Exception {
		Logger.getInstance().printOut("Reading config file. (" + path.toString() + ") ... ");
		final Properties properties = new Properties();
		try {
			properties.load(Files.newInputStream(path));
			Logger.getInstance().printOut("Success!");
			printConfigFile(properties);
			return properties;
		} catch (IOException e) {
			Logger.getInstance().printOut("Fail! -> " + e.getMessage());
			throw e;
		}
	}

	private static void printConfigFile(final Properties properties) {
		for (IProperty prop : propertyList) {
			StringBuilder sb = new StringBuilder();
			sb.append("\t").append(prop.getKey()).append(" = ");
			boolean success = prop.setValue(properties.getProperty(prop.getKey()));
			sb.append(prop.getValue().toString());
			sb.append(success ? "" : " (default value!)");
			Logger.getInstance().printOut(sb.toString());
		}
	}

	private void initOutputPath() {
		rootOutPath = Paths.get(((outputPath.getValue().isEmpty()) ? "output" : outputPath.getValue()) + File.separator
				+ (Long.MAX_VALUE - System.currentTimeMillis()));
		try {
			Files.createDirectories(rootOutPath);
			Logger.getInstance().install(rootOutPath, verboseLevel.getValue());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initModelPath() {
		pathToModels = Paths.get((modelsPath.getValue().isEmpty()) ? DEFAULT_MODELS_DIRECTORY : modelsPath.getValue());

		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(DEFAULT_CONFIG_DIRECTORY + File.separator + "models.txt"),
					Charset.defaultCharset());
		} catch (IOException e) {
			Logger.getInstance().printErr("No feature models specified!");
		}

		if (lines != null) {
			boolean pause = false;
			modelNames = new ArrayList<>(lines.size());
			for (String modelName : lines) {
				// modelName = modelName.trim();
				if (!modelName.trim().isEmpty()) {
					if (!modelName.startsWith("\t")) {

						if (modelName.startsWith(COMMENT)) {
							if (modelName.equals(STOP_MARK)) {
								pause = !pause;
							}
						} else if (!pause) {
							modelNames.add(modelName.trim());
						}
					}
				}
			}
		} else {
			modelNames = Collections.<String>emptyList();
		}

	}

	protected final long getNextSeed() {
		return randSeed.nextLong();
	}

	protected final IFeatureModel initModel(int index) {
		return init(modelNames.get(index));
	}

	protected static final void printErr(String message) {
		Logger.getInstance().printErr(message);
	}

	protected static final void printOut(String message) {
		Logger.getInstance().printOut(message);
	}

	protected static final void printOut(String message, int tabs) {
		Logger.getInstance().printOut(message, tabs);
	}

}
