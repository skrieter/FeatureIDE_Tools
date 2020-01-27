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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import de.ovgu.featureide.fm.benchmark.streams.MultiStream;

/**
 * @author Sebastian Krieter
 */
public class Logger {

	private static final Logger INSTANCE = new Logger();

	private Logger() {
		orgOut = System.out;
		orgErr = System.err;
		outStream = orgOut;
		errStream = orgErr;
	}

	public static final Logger getInstance() {
		return INSTANCE;
	}

	private final PrintStream orgOut;
	private final PrintStream orgErr;

	private PrintStream outStream;
	private PrintStream errStream;

	public int verboseLevel = 0;

	public void install(Path outputPath, int verboseLevel) throws FileNotFoundException {
		this.verboseLevel = verboseLevel;

		FileOutputStream outFileStream = new FileOutputStream(outputPath.resolve("console_log.txt").toFile());
		FileOutputStream errFileStream = new FileOutputStream(outputPath.resolve("error_log.txt").toFile());
		FileOutputStream outReducedFileStream = new FileOutputStream(
				outputPath.resolve("console_log_reduced.txt").toFile());
		FileOutputStream errReducedFileStream = new FileOutputStream(
				outputPath.resolve("error_log_reduced.txt").toFile());

		outStream = new PrintStream(new MultiStream(orgOut, outFileStream, outReducedFileStream));
		errStream = new PrintStream(new MultiStream(orgErr, errFileStream, errReducedFileStream));

		if (verboseLevel >= 2) {
			System.setOut(new PrintStream(new MultiStream(orgOut, outFileStream)));
			System.setErr(new PrintStream(new MultiStream(orgErr, errFileStream)));
		} else {
			System.setOut(new PrintStream(outFileStream));
			System.setErr(new PrintStream(errFileStream));
		}
	}

	public void uninstall() {
		System.setOut(orgOut);
		System.setErr(orgErr);
		outStream = orgOut;
		errStream = orgErr;
	}

	public int isVerbose() {
		return verboseLevel;
	}

	public static final String getCurTime() {
		return new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss").format(new Timestamp(System.currentTimeMillis()));
	}

	public final void logError(String message) {
		println(errStream, message, true);
	}

	public final void logError(Throwable error) {
		println(errStream, error, false);
	}

	public final void logError(String message, boolean onlyVerbose) {
		println(errStream, message, onlyVerbose);
	}

	public final void logInfo(String message) {
		println(outStream, message, true);
	}

	public final void logInfo(String message, boolean onlyVerbose) {
		logInfo(message, 0, onlyVerbose);
	}

	public final void logInfo(String message, int tabs, boolean onlyVerbose) {
		if (tabs > 0) {
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < tabs; i++) {
				sb.append('\t');
			}
			sb.append(message);
			println(outStream, sb.toString(), onlyVerbose);
		} else {
			println(outStream, message, onlyVerbose);
		}
	}

	private void println(PrintStream stream, String message, boolean onlyVerbose) {
		if (verboseLevel > 0 || !onlyVerbose) {
			stream.println(getCurTime() + " " + message);
		}
	}

	private void println(PrintStream stream, Throwable error, boolean onlyVerbose) {
		if (verboseLevel > 0 || !onlyVerbose) {
			stream.print(getCurTime() + ":");
			error.printStackTrace(stream);
			stream.println(getCurTime());
		}
	}

	public final void logInfo(String message, int tabs) {
		final StringBuilder sb = new StringBuilder(getCurTime());
		sb.append(" ");
		for (int i = 0; i < tabs; i++) {
			sb.append('\t');
		}
		sb.append(message);
		logInfo(sb.toString());
	}

}
