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
import java.util.Collection;
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
import de.ovgu.featureide.fm.test.Familiar;

/**
 * @author Sebastian Krieter
 */
public class FeatureRemovalTester extends ABenchmark {

	private interface IRemover {
		Node remove(FeatureModel fm, Collection<String> features);
	}

	private static class FIDERemover implements IRemover {
		@Override
		public Node remove(FeatureModel fm, Collection<String> features) {
			return NodeCreator.createNodes(fm, features).toCNF();
		}

		@Override
		public String toString() {
			return "FIDE";
		}
	}

	private static class FamiliarRemover implements IRemover {
		@Override
		public Node remove(FeatureModel fm, Collection<String> features) {
			return Familiar.createNodes(fm, features).toCNF();
		}

		@Override
		public String toString() {
			return "FAMILIAR";
		}
	}

	private static class MyRemover implements IRemover {
		@Override
		public Node remove(FeatureModel fm, Collection<String> features) {
			try {
				return FMCorePlugin.removeFeatures(fm, features);
			} catch (TimeoutException | UnkownLiteralException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public String toString() {
			return "MY";
		}
	}

	private class TestRunner implements Runnable {
		private final FeatureModel fm;
		private final List<String> features;
		private final IRemover remover;

		private Node fmNode = null;

		public TestRunner(FeatureModel fm, List<String> features, IRemover remover) {
			this.fm = fm;
			this.features = features;
			this.remover = remover;
		}

		@Override
		public void run() {
			logger.getTimer().start();
			try {
				setFmNode(remover.remove(fm, features));
			} catch (Throwable e) {
				if (!(e instanceof ThreadDeath)) {
					e.printStackTrace();
					Runtime.getRuntime().halt(-1);
				}
			}
			logger.getTimer().stop();
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
		//		tester.test();
	}

	private static final double[] removeFactors = { 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65
	// , 0.7, 0.75, 0.8, 0.85, 0.9, 0.95
	};
	private static final IRemover[] algo = { new MyRemover(), new FIDERemover()
	//		,new FamiliarRemover() 
	};

	private static final long feasibleTimeout = 1000;
	private static final int randRounds = 1;
	private static final int nonRandRounds = 1;

	private static long maxTimeout = 0;

	@SuppressWarnings("deprecation")
	private Node run(FeatureModel fm, List<String> features, IRemover remover, long timeout) {
		final TestRunner testRunner = new TestRunner(fm, features, remover);
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
		logger.getTimer().stop();
		return testRunner.getFmNode();
	}

	private Node run(FeatureModel fm, List<String> features, IRemover remover) {
		final TestRunner testRunner = new TestRunner(fm, features, remover);
		Thread thread = new Thread(testRunner);
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return testRunner.getFmNode();
	}

	public void run() {
		try {
			logger.getTimer().setVerbose(false);

			for (int i1 = 0; i1 < modelNames.size(); i1++) {
				final String modelName = modelNames.get(i1);

				logger.verbosePrintln("Load model: " + modelName);

				final FeatureModel fm = initModel(i1);
				final Set<String> orgFeatures = new HashSet<>(fm.getFeatureNames());
				//				maxTimeout = 0;
				maxTimeout = feasibleTimeout;

				final long nextGlobalSeed = getNextSeed();

				for (int i2 = 0; i2 < algo.length; i2++) {
					final IRemover algoName = algo[i2];
					csvWriter.setAutoSave(Paths.get("output/" + modelName + "." + algoName + ".csv"));
					long maxTime = 0;
					logger.verbosePrintln("Timeout = " + maxTimeout);

					final Random globalRand = new Random(nextGlobalSeed);

					for (int i3 = 0; i3 < randRounds; i3++) {
						final long nextSeed = globalRand.nextLong();
						final Random rand = new Random(nextSeed);

						logger.verbosePrintln("Random Seed: " + nextSeed);

						List<String> shuffledFeatures = new ArrayList<>(orgFeatures);
						Collections.shuffle(shuffledFeatures, rand);

						for (int i4 = 0; i4 < removeFactors.length; i4++) {

							final double removeFactor = removeFactors[i4];
							final int featureCount = (int) Math.floor(removeFactor * orgFeatures.size());

							final List<String> removeFeatures = Collections.unmodifiableList(shuffledFeatures.subList(0, featureCount));

							logger.verbosePrintln("Remove Factor = " + removeFactor);
							logger.verbosePrintln("\tRemoving the following features:");
							if (logger.isVerbose()) {
								for (String name : removeFeatures) {
									logger.verbosePrintln("\t\t" + name);
								}
							}

							logger.println(modelName + ", dummy round, " + nextSeed);
							run(fm, removeFeatures, algoName, maxTimeout);

							for (int i5 = 0; i5 < nonRandRounds; i5++) {
								logger.println(modelName + ", " + algoName + ", " + (i3 + 1) + "/" + randRounds + ", " + removeFactor + ", " + (i5 + 1) + "/"
										+ nonRandRounds + ", " + nextSeed);

								System.gc();

								final Node fmNode1 = run(fm, removeFeatures, algoName, maxTimeout);

								csvWriter.createNewLine();
								csvWriter.addValue(modelName);
								csvWriter.addValue(removeFactor);
								csvWriter.addValue(i3);
								csvWriter.addValue(nextSeed);
								csvWriter.addValue(logger.getTimer().getLastTime());
								csvWriter.addValue(fmNode1 != null);

								if (i2 == 0) {
									final long lastTime = logger.getTimer().getLastTime();
									if (maxTime < lastTime) {
										maxTime = lastTime;
									}
								}
							}
						}
					}
					csvWriter.createNewLine();
					if (i2 == 0) {
						//						maxTimeout = Math.max(feasibleTimeout, (maxTime / 500000));
					}
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
					for (int l = 0; l < algo.length; l++) {
						logger.println(modelName + ", " + removeFactor + ", " + nextSeed);

						nodeArray[l] = run(fm, features, algo[l]);
					}
					for (int i1 = 0; i1 < algo.length; i1++) {
						for (int i2 = i1; i2 < algo.length; i2++) {
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
									compareFailed();
								} else {
									logger.verbosePrintln("\tTrue!");
								}
								logger.verbosePrintln("\tCompare " + i2 + " with " + i1 + "...");
								if (!ModelComparator.compare(fmNode1, fmNode2)) {
									compareFailed();
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

	private void compareFailed() {
		logger.println("\tFalse!");
		throw new RuntimeException();
	}

}
