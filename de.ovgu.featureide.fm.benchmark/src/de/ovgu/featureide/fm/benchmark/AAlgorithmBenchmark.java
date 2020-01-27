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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import de.ovgu.featureide.fm.benchmark.process.Algorithm;
import de.ovgu.featureide.fm.benchmark.process.ProcessRunner;
import de.ovgu.featureide.fm.benchmark.process.Result;
import de.ovgu.featureide.fm.benchmark.util.CSVWriter;
import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;

/**
 * @author Sebastian Krieter
 */
public abstract class AAlgorithmBenchmark<R, A extends Algorithm<R>> extends ABenchmark {

	protected final LinkedHashMap<Algorithm<R>, Integer> algorithmMap = new LinkedHashMap<>();
	protected final List<Algorithm<R>> algorithmList = new ArrayList<>();

	private CSVWriter dataCSVWriter, modelCSVWriter, algorithmCSVWriter;

	protected int algorithmIndex;
	protected int algorithmIteration;
	protected Result<R> result;
	protected CNF modelCNF;
	protected CNF randomizedModelCNF;

	public AAlgorithmBenchmark(String configPath, String configName) throws Exception {
		super(configPath, configName);
	}

	@Override
	protected void addCSVWriters() {
		super.addCSVWriters();
		dataCSVWriter = addCSVWriter("data.csv", Arrays.asList("ModelID", "AlgorithmID", "SystemIteration",
				"AlgorithmIteration", "InTime", "NoError", "Time"));
		modelCSVWriter = addCSVWriter("models.csv", Arrays.asList("ModelID", "Name"));
		algorithmCSVWriter = addCSVWriter("algorithms.csv", Arrays.asList("AlgorithmID", "Name", "Settings"));
	};

	public void run() {
		super.run();

		if (config.systemIterations.getValue() > 0) {
			Logger.getInstance().logInfo("Start", false);

			final ProcessRunner processRunner = new ProcessRunner();
			processRunner.setTimeout(config.timeout.getValue());

			int maxAlgorithmIndex = 0;
			int systemIndexEnd = config.systemNames.size();

			systemLoop: for (systemIndex = 0; systemIndex < systemIndexEnd; systemIndex++) {
				logSystem();
				final List<A> algorithms;
				try {
					algorithms = prepareAlgorithms();
				} catch (Exception e) {
					Logger.getInstance().logError(e);
					continue systemLoop;
				}
				for (A algorithm : algorithms) {
					if (!algorithmMap.containsKey(algorithm)) {
						algorithmMap.put(algorithm, maxAlgorithmIndex);
						algorithmList.add(algorithm);
						algorithmIndex = maxAlgorithmIndex;
						writeCSV(algorithmCSVWriter, this::writeAlgorithm);
						maxAlgorithmIndex++;
					}
				}
				try {
					modelCNF = prepareModel();
				} catch (Exception e) {
					Logger.getInstance().logError(e);
					continue systemLoop;
				}
				for (systemIteration = 1; systemIteration <= config.systemIterations.getValue(); systemIteration++) {
					try {
						randomizedModelCNF = adaptModel();
						writeCSV(modelCSVWriter, this::writeModel);
					} catch (Exception e) {
						Logger.getInstance().logError(e);
						continue systemLoop;
					}

					algorithmLoop: for (A algorithm : algorithms) {
						algorithmIndex = algorithmMap.get(algorithm);
						for (algorithmIteration = 1; algorithmIteration <= config.algorithmIterations
								.getValue(); algorithmIteration++) {
							try {
								logRun();
								result = new Result<>();
								processRunner.run(algorithm, result);
								writeCSV(dataCSVWriter, this::writeData);
							} catch (Exception e) {
								e.printStackTrace();
								Logger.getInstance().logError(e);
								continue algorithmLoop;
							}
						}
					}
				}
			}
			Logger.getInstance().logInfo("Finished", false);
		} else {
			Logger.getInstance().logInfo("Nothing to do", false);
		}
	}

	protected void writeModel(CSVWriter modelCSVWriter) {
		modelCSVWriter.addValue(systemIndex);
		modelCSVWriter.addValue(config.systemNames.get(systemIndex));
		modelCSVWriter.addValue(-1);
		modelCSVWriter.addValue(randomizedModelCNF.getVariables().size());
		modelCSVWriter.addValue(randomizedModelCNF.getClauses().size());
	}

	protected void writeAlgorithm(CSVWriter algorithmCSVWriter) {
		final Algorithm<?> algorithm = algorithmList.get(algorithmIndex);
		algorithmCSVWriter.addValue(algorithmIndex);
		algorithmCSVWriter.addValue(algorithm.getName());
		algorithmCSVWriter.addValue(algorithm.getParameterSettings());
	}

	protected void writeData(CSVWriter dataCSVWriter) {
		dataCSVWriter.addValue(systemIndex);
		dataCSVWriter.addValue(algorithmIndex);
		dataCSVWriter.addValue(systemIteration);
		dataCSVWriter.addValue(algorithmIteration);
		dataCSVWriter.addValue(result.isTerminatedInTime());
		dataCSVWriter.addValue(result.isNoError());
		dataCSVWriter.addValue(result.getTime());
	}

	private void logRun() {
		StringBuilder sb = new StringBuilder();
		sb.append(systemIndex + 1);
		sb.append("/");
		sb.append(config.systemNames.size());
		sb.append(" | ");
		sb.append(systemIteration);
		sb.append("/");
		sb.append(config.systemIterations.getValue());
		sb.append(" | (");
		sb.append(algorithmIndex + 1);
		sb.append("/");
		sb.append(algorithmList.size());
		sb.append(") ");
		sb.append(algorithmList.get(algorithmIndex).getFullName());
		sb.append(" | ");
		sb.append(algorithmIteration);
		sb.append("/");
		sb.append(config.algorithmIterations.getValue());
		Logger.getInstance().logInfo(sb.toString(), 2, false);
	}

	protected abstract CNF prepareModel() throws Exception;

	protected abstract CNF adaptModel() throws Exception;

	protected abstract List<A> prepareAlgorithms() throws Exception;

	public CSVWriter getDataCSVWriter() {
		return dataCSVWriter;
	}

	public CSVWriter getModelCSVWriter() {
		return modelCSVWriter;
	}

	public CSVWriter getAlgorithmCSVWriter() {
		return algorithmCSVWriter;
	}

}
