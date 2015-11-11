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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.prop4j.Node;
import org.sat4j.specs.TimeoutException;

import de.ovgu.featureide.fm.core.FMCorePlugin;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.editing.NodeCreator;
import de.ovgu.featureide.fm.core.editing.cnf.ModelComparator;
import de.ovgu.featureide.fm.core.editing.cnf.UnkownLiteralException;

/**
 * @author Sebastian Krieter
 */
public class FeatureRemovalTester extends ABenchmark {

	private static class TestRunner implements Runnable {
		private final FeatureModel fm;
		private final List<String> features;
		private final int mode;

		private Node fmNode = null;

		public TestRunner(FeatureModel fm, List<String> features, int mode) {
			this.fm = fm;
			this.features = features;
			this.mode = mode;
		}

		@Override
		public void run() {
			switch (mode) {
			case 0:
				try {
					setFmNode(FMCorePlugin.removeFeatures(fm, features));
				} catch (TimeoutException | UnkownLiteralException e) {
					e.printStackTrace();
				}
				break;
			case 1:
				setFmNode(NodeCreator.createNodes(fm, features).toCNF());
				break;
			default:
				break;
			}

		}

		public Node getFmNode() {
			return fmNode;
		}

		private void setFmNode(Node fmNode) {
			this.fmNode = fmNode;
		}

	}

	public static void main(String[] args) {
		FeatureRemovalTester tester = new FeatureRemovalTester();
		tester.run();
	}

	private static final double[] removeFactors = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 };
	private static final String[] algo = { "MY", "FIDE" };

	private static final long feasibleTimeout = 10000;
	private static final int rounds = 24;

	private static long maxTimeout = 0;

	@SuppressWarnings("deprecation")
	private Node run(FeatureModel fm, List<String> features, int mode, long timeout) {
		final TestRunner testRunner = new TestRunner(fm, features, mode);
		Thread thread = new Thread(testRunner);
		thread.start();
		try {
			thread.join(timeout);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (thread.isAlive()) {
			thread.stop();
		}
		csvWriter.addValue(logger.getTimer().stop());
		return testRunner.getFmNode();
	}

	private Node run(FeatureModel fm, List<String> features, int mode) {
		final TestRunner testRunner = new TestRunner(fm, features, mode);
		Thread thread = new Thread(testRunner);
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return testRunner.getFmNode();
	}

	@Override
	public void run() {
		try {
			logger.getTimer().setVerbose(false);

			for (int j = 0; j < modelNames.size(); j++) {
				final String modelName = modelNames.get(j);

				logger.verbosePrintln("Load model: " + modelName);

				final FeatureModel fm = initModel(j);
				final Set<String> orgFeatures = new HashSet<>(fm.getFeatureNames());
				maxTimeout = 0;

				for (int l = 0; l < algo.length; l++) {
					final String algoName = algo[l];
					csvWriter.setAutoSave(Paths.get("results/" + modelName + "." + algoName + ".csv"));
					long maxTime = 0;
					logger.verbosePrintln("Timeout = " + maxTimeout);
					
					for (int k = 0; k < removeFactors.length; k++) {
						final double removeFactor = removeFactors[k];
						final int featureCount = (int) Math.floor(removeFactor * orgFeatures.size());

						logger.verbosePrintln("Remove Factor = " + removeFactor);

						for (int i = 0; i < rounds; i++) {
							final long nextSeed = getNextSeed();
							final Random rand = new Random(nextSeed);

							logger.verbosePrintln("Random Seed: " + nextSeed);

							logger.verbosePrintln("\tRemoving the following features:");

							List<String> features = new ArrayList<>(orgFeatures);
							Collections.shuffle(features, rand);
							features.subList(featureCount, orgFeatures.size()).clear();
							features = Collections.unmodifiableList(features);

							if (logger.isVerbose()) {
								for (String name : features) {
									logger.verbosePrintln("\t\t" + name);
								}
							}

							logger.println(modelName + ", " + removeFactor + ", " + i + ", " + nextSeed);

							csvWriter.createNewLine();
							csvWriter.addValue(modelName);
							csvWriter.addValue(removeFactor);
							csvWriter.addValue(i);
							csvWriter.addValue(nextSeed);

							logger.getTimer().start();

							final Node fmNode1 = run(fm, features, l, maxTimeout);
							csvWriter.addValue(fmNode1 != null);
							
							if (l == 0) {
								final long lastTime = logger.getTimer().getLastTime();
								if (maxTime < lastTime) {
									maxTime = lastTime;
								}
							}
						}
					}
					if (l == 0) {
						maxTimeout = Math.max(feasibleTimeout, (maxTime / 250000));
					}
					csvWriter.createNewLine();
				}
			}
			logger.println("\nDone!.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void test() {
		try {
			logger.getTimer().setVerbose(false);

			for (int j = 0; j < modelNames.size(); j++) {
				final String modelName = modelNames.get(j);

				logger.verbosePrintln("Load model: " + modelName);

				final FeatureModel fm = initModel(j);
				final Set<String> orgFeatures = new HashSet<>(fm.getFeatureNames());

				for (int k = 0; k < removeFactors.length; k++) {
					final double removeFactor = removeFactors[k];
					final int featureCount = (int) Math.floor(removeFactor * orgFeatures.size());

					logger.verbosePrintln("Remove Factor = " + removeFactor);

					Node[] nodeArray = new Node[algo.length];
					for (int l = 0; l < algo.length; l++) {
						for (int i = 0; i < rounds; i++) {
							final long nextSeed = getNextSeed();
							final Random rand = new Random(nextSeed);

							logger.verbosePrintln("Random Seed: " + nextSeed);
							logger.verbosePrintln("\tRemoving the following features:");

							List<String> features = new ArrayList<>(orgFeatures);
							Collections.shuffle(features, rand);
							features.subList(featureCount, orgFeatures.size()).clear();
							features = Collections.unmodifiableList(features);

							if (logger.isVerbose()) {
								for (String name : features) {
									logger.verbosePrintln("\t\t" + name);
								}
							}

							logger.println(modelName + ", " + removeFactor + ", " + i + ", " + nextSeed);

							nodeArray[l] = run(fm, features, l);
						}
					}
					for (int i1 = 0; i1 < algo.length; i1++) {
						for (int i2 = 0; i2 < algo.length; i2++) {
							final Node fmNode1 = nodeArray[i1];
							final Node fmNode2 = nodeArray[i2];

							if (fmNode1 == null) {
								logger.verbosePrintln("\tNode" + i1 + " = null!");
								logger.verbosePrintln("\t -> Not Compared!");
							} else if (fmNode2 == null) {
								logger.verbosePrintln("\tNode" + i2 + " = null!");
								logger.verbosePrintln("\t -> Not Compared!");
							} else {

								logger.verbosePrintln("\tCompare " + i1 + " with " + i2 + "...");
								if (!ModelComparator.compare(fmNode2, fmNode1)) {
									logger.println("\tFalse!");
								} else {
									logger.verbosePrintln("\tTrue!");
								}
								logger.verbosePrintln("\tCompare " + i2 + " with " + i1 + "...");
								if (!ModelComparator.compare(fmNode1, fmNode2)) {
									logger.println("\tFalse!");
								} else {
									logger.verbosePrintln("\tTrue!");
								}
							}
						}
					}
				}
			}
			logger.println("\nDone!.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
