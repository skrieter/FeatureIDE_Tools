package de.ovgu.featureide.fm.benchmark.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CSVWriter {
	private static final String NEWLINE = System.lineSeparator();

	private final List<List<String>> values = new ArrayList<>();

	private String separator = ";";
	private List<String> header = null;

	private Path outputPath = Paths.get("");
	private Path path;
	private boolean dummy = false;
	private boolean keepLines = false;
	private boolean append = false;
	private boolean newFile = true;
	private int nextLine = 0;

	public Path getOutputPath() {
		return outputPath;
	}

	public boolean setOutputPath(Path outputPath) {
		if (Files.isDirectory(outputPath)) {
			this.outputPath = outputPath;
			return true;
		} else if (!Files.exists(outputPath)) {
			try {
				Files.createDirectories(outputPath);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			this.outputPath = outputPath;
			return true;
		} else {
			return false;
		}
	}

	public void setFileName(Path fileName) {
		setPath(outputPath.resolve(fileName));
	}

	public void setFileName(String fileName) {
		setPath(outputPath.resolve(fileName));
	}

	private void setPath(Path path) {
		this.path = path;
		try {
			if (append) {
				newFile = !Files.exists(path);
			} else {
				Files.deleteIfExists(path);
				newFile = true;
			}
			if (newFile) {
				Files.createFile(path);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		reset();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (List<String> line : values) {
			writer(sb, line);
		}
		return sb.toString();
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public List<String> getHeader() {
		return header;
	}

	public void setHeader(List<String> header) {
		this.header = new ArrayList<>(header);
		if (values.isEmpty()) {
			values.add(this.header);
		} else {
			values.set(0, this.header);
		}
		if (!newFile) {
			nextLine = 1;
		}
	}

	public void addHeaderValue(String headerValue) {
		header.add(headerValue);
	}

	public void addLine(List<String> line) {
		if (!dummy) {
			values.add(line);
		}
	}

	public void createNewLine() {
		if (!dummy) {
			values.add(new ArrayList<>());
		}
	}

	public void flush() {
		if (path != null) {
			if (!dummy) {
				final StringBuilder sb = new StringBuilder();
				for (int i = nextLine; i < values.size(); i++) {
					writer(sb, values.get(i));
				}
				try {
					Files.write(path, sb.toString().getBytes(), StandardOpenOption.APPEND);
					if (keepLines) {
						nextLine = values.size();
					} else {
						values.subList(1, values.size()).clear();
						nextLine = 1;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void addValue(Object o) {
		if (!dummy) {
			values.get(values.size() - 1).add(o.toString());
		}
	}

	public List<List<String>> getValues() {
		return values;
	}

	private void writer(StringBuilder sb, List<String> line) {
		for (String value : line) {
			if (value != null) {
				sb.append(value);
			}
			sb.append(separator);
		}
		if (line.isEmpty()) {
			sb.append(NEWLINE);
		} else {
			final int length = sb.length() - 1;
			sb.replace(length, length + separator.length(), NEWLINE);
		}
	}

	public boolean saveToFile(Path p) {
		try {
			Files.write(p, toString().getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void reset() {
		if (!values.isEmpty()) {
			values.subList(1, values.size()).clear();
		}
		nextLine = 0;
	}

	public void resetLine() {
		if (!values.isEmpty()) {
			values.remove(values.size() - 1);
		}
	}

	public boolean isDummy() {
		return dummy;
	}

	public void setDummy(boolean dummy) {
		this.dummy = dummy;
	}

	public boolean isKeepLines() {
		return keepLines;
	}

	public void setKeepLines(boolean keepLines) {
		this.keepLines = keepLines;
	}

	public boolean isAppend() {
		return append;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(path, ((CSVWriter) obj).path);
	}

}
