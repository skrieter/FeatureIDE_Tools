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
package de.ovgu.featureide.fm.benchmark.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.init.LibraryManager;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;

/**
 * @author Sebastian Krieter
 */
public class FeatureModelReader {

	static {
		LibraryManager.registerLibrary(FMCoreLibrary.getInstance());
	}

	protected String modelFileName = "model.xml";
	protected Path pathToModels;

	public final IFeatureModel read(final String name) {
		IFeatureModel fm = null;

		fm = readFromFolder(pathToModels, name, fm);
		if (fm != null) {
			return fm;
		}

		fm = readFromFile(pathToModels, name, fm);
		if (fm != null) {
			return fm;
		}

		fm = readFromZip(pathToModels, name, fm);

		return fm;
	}

	public Path getPathToModels() {
		return pathToModels;
	}

	public void setPathToModels(Path pathToModels) {
		this.pathToModels = pathToModels;
	}

	public IFeatureModel loadFile(final Path path) {
		final FileHandler<IFeatureModel> fh = FeatureModelManager.getFileHandler(path);
		return fh.getLastProblems().containsError() ? null : fh.getObject();
	}

	public IFeatureModel readFromFolder(final Path rootPath, final String name, IFeatureModel fm) {
		Path modelFolder = rootPath.resolve(name);
		if (Files.exists(modelFolder) && Files.isDirectory(modelFolder)) {
			final Path path = modelFolder.resolve(modelFileName);
			if (Files.exists(path)) {
				return loadFile(path);
			} else {
				return readFromFile(modelFolder, "model", fm);
			}
		} else {
			return null;
		}
	}

	public IFeatureModel readFromFile(final Path rootPath, final String name, IFeatureModel fm) {
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
			Logger.getInstance().logError(e);
		}
		return null;
	}

	protected IFeatureModel readFromZip(final Path rootPath, final String name, IFeatureModel fm) {
		final Filter<Path> fileFilter = file -> Files.isReadable(file) && Files.isRegularFile(file)
				&& file.getFileName().toString().matches(".*[.]zip\\Z");
		try (DirectoryStream<Path> files = Files.newDirectoryStream(rootPath, fileFilter)) {
			for (Path path : files) {
				final URI uri = URI.create("jar:" + path.toUri().toString());
				try (final FileSystem zipFs = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap())) {
					for (Path root : zipFs.getRootDirectories()) {
						fm = readFromFolder(root, name, fm);
						fm = readFromFile(root, name, fm);
					}
					if (fm != null) {
						return fm;
					}
				}
			}
		} catch (IOException e) {
			Logger.getInstance().logError(e);
		}
		return null;
	}

	public void dispose() {
		Logger.getInstance().uninstall();
	}

}
