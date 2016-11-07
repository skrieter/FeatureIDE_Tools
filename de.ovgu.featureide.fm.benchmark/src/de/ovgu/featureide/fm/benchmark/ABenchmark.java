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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import de.ovgu.featureide.fm.benchmark.properties.IProperty;
import de.ovgu.featureide.fm.benchmark.properties.Seed;
import de.ovgu.featureide.fm.benchmark.properties.StringProperty;
import de.ovgu.featureide.fm.benchmark.properties.Timeout;
import de.ovgu.featureide.fm.core.Logger;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat;

/**
 * @author Sebastian Krieter
 */
public abstract class ABenchmark {

	private static final String MODELS_ZIP_FILE = "models.zip";
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

	private static final String MODELS_DIRECTORY = "models";
	private static final String CONFIG_DIRECTORY = "config";

	private static final String COMMENT = "#";
	private static final String STOP_MARK = "###";

	protected final static ProgressLogger logger = new ProgressLogger();
	protected final CSVWriter csvWriter = new CSVWriter();

	protected final List<String> modelNames;
	private final Random randSeed;

	private static final Seed seed = new Seed();
	protected static final Timeout timeout = new Timeout();
	protected static final StringProperty outputPath = new StringProperty("output");
	protected static final StringProperty modelsPath = new StringProperty("models");

	protected Path rootOutPath, pathToModels;

	protected final IFeatureModel init(final String name) {
		IFeatureModel fm = null;

		fm = lookUpFile(name, fm);
		fm = lookupZip(name, fm);

		if (fm == null) {
			throw new RuntimeException("Model not found: " + name);
		} else {
			return fm;
		}
	}

	protected IFeatureModel loadFile(final Path path) {
		IFeatureModel fm = FMFactoryManager.getFactory().createFeatureModel();
		FileHandler.load(path, fm, new XmlFeatureModelFormat());
		return fm;
	}

	protected IFeatureModel lookUpFile(final String name, IFeatureModel fm) {
		if (fm != null) {
			return fm;
		} else {
			final Path path = pathToModels.resolve(name).resolve(MODEL_FILE);
			return (Files.exists(path)) ? loadFile(path) : null;
		}
	}

	protected IFeatureModel lookupZip(final String name, IFeatureModel fm) {
		if (fm != null) {
			return fm;
		} else {
			final LinkedList<Path> modelList = new LinkedList<>();
			final String pathName = name + "/";
			final URI uri = URI.create("jar:" + pathToModels.resolve(MODELS_ZIP_FILE).toUri().toString());
			try (final FileSystem zipFs = FileSystems.newFileSystem(uri, Collections.<String, Object> emptyMap())) {
				for (Path root : zipFs.getRootDirectories()) {
					Files.walkFileTree(root, new FileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							final Path fileName = file.getFileName();
							if (fileName != null && MODEL_FILE.equals(fileName.toString())) {
								modelList.add(file);
								return FileVisitResult.TERMINATE;
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							return FileVisitResult.TERMINATE;
						}

						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
								throws IOException {
							final Path fileName = dir.getFileName();
							if (fileName == null || pathName.equals(fileName.toString())) {
								return FileVisitResult.CONTINUE;
							} else {
								return FileVisitResult.SKIP_SUBTREE;
							}
						}

						@Override
						public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}
					});
				}
				return (modelList.isEmpty()) ? null : loadFile(modelList.getFirst());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	public static void addProperty(IProperty property) {
		propertyList.add(property);
	}

	private static void readProperties() {
		final Path path = Paths.get(CONFIG_DIRECTORY + File.separator + "config.properties");
		logger.print("Reading config file. (" + path.toString() + ") ... ");
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
		logger.getTimer().start();
		thread.start();
		try {
			thread.join(timeout);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (thread.isAlive()) {
			thread.stop();
		}
		logger.getTimer().stop();
		return testRunner.getResult();
	}

	public ABenchmark() {
		readProperties();
		initPaths();

		final Path consoleOutputPath = rootOutPath.resolve("console.txt");
		final PrintStream orgConsole = System.out;
		try {
			System.setOut(new PrintStream(new FileOutputStream(consoleOutputPath.toFile()) {
				@Override
				public void flush() throws IOException {
					super.flush();
					orgConsole.flush();
				}

				@Override
				public void write(byte[] buf, int off, int len) throws IOException {
					super.write(buf, off, len);
					orgConsole.write(buf, off, len);
				}

				@Override
				public void write(int b) throws IOException {
					super.write(b);
					orgConsole.write(b);
				}

				@Override
				public void write(byte[] b) throws IOException {
					super.write(b);
					orgConsole.write(b);
				}
			}));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(CONFIG_DIRECTORY + File.separator + "models.txt"), Charset.defaultCharset());
		} catch (IOException e) {
			logger.println("No feature models specified!");
			Logger.logError(e);
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
			modelNames = Collections.<String> emptyList();
		}

		randSeed = new Random(seed.getValue());
	}

	private void initPaths() {
		rootOutPath = Paths.get(((outputPath.getValue().isEmpty()) ? "output" : outputPath.getValue()) + File.separator
				+ (Long.MAX_VALUE - System.currentTimeMillis()));
		try {
			Files.createDirectories(rootOutPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		pathToModels = Paths.get((modelsPath.getValue().isEmpty()) ? MODELS_DIRECTORY : modelsPath.getValue());
	}

	protected final long getNextSeed() {
		return randSeed.nextLong();
	}

	protected final IFeatureModel initModel(int index) {
		return init(modelNames.get(index));
	}

	protected static final String getCurTime() {
		return new SimpleDateFormat("MM/dd/yyyy-hh:mm:ss").format(new Timestamp(System.currentTimeMillis()));
	}

	protected static final void printErr(String message) {
		System.err.println(getCurTime() + ": " + message);
	}

	protected static final void printOut(String message) {
		System.out.println(getCurTime() + ": " + message);
	}

	protected static final void printOut(String message, int tabs) {
		StringBuilder sb = new StringBuilder(getCurTime());
		sb.append(": ");
		for (int i = 0; i < tabs; i++) {
			sb.append('\t');
		}
		sb.append(message);
		System.out.println(sb.toString());
	}

}
