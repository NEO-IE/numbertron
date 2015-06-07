package main.java.iitb.neo.training.algorithm.lpercp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.ds.LRGraph;
import edu.washington.multirframework.multiralgorithm.Dataset;
import edu.washington.multirframework.multiralgorithm.DenseVector;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;
import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;

/**
 * Simple percepton-ish algorithm for learning weights. Averaged because the
 * parameters are averaged across the runs Almost a copy paste from
 * AveragedPerceptron from multirframework
 * 
 * Adopted for the lightweight entity graph implementation
 * @author aman/ashish
 * 
 */
public class LocalAveragedPerceptronEntityGraph {

	public int numIterations;

	public boolean computeAvgParameters = true;
	public boolean finalAverageCalc;
	private double delta = 1;
	private double regulaizer;
	private Scorer scorer;
	private Model model;
	private Random random;

	/*
	 * variables added for debugging purposes.
	 */
	public static HashMap<Integer, String> relNumNameMapping;
	public static HashMap<String, Integer> featNameNumMapping;
	HashMap<Integer, String> featureList;
	Integer numRelation;
	Integer numFeatures;
	String outputFile = "verbose_iteration_updates_key_area_1";
	BufferedWriter obw;
	boolean debug = false;
	boolean readMapping = true;

	public LocalAveragedPerceptronEntityGraph(Model model, Random random,
			int maxIterations, double regularizer, boolean finalAverageCalc, String mappingFile)
			throws NumberFormatException, IOException {
		scorer = new Scorer();
		this.model = model;
		this.random = random;
		this.numIterations = maxIterations;
		this.regulaizer = regularizer;
		this.finalAverageCalc = finalAverageCalc;

		if (readMapping) {
			relNumNameMapping = new HashMap<Integer, String>();
			featNameNumMapping = new HashMap<String, Integer>();
			featureList = new HashMap<Integer, String>();
			BufferedReader featureReader = new BufferedReader(new FileReader(
					mappingFile));
			Integer numRel = Integer.parseInt(featureReader.readLine());
			for (int i = 0; i < numRel; i++) {
				// skip relation names
				String rel = featureReader.readLine().trim();
				relNumNameMapping.put(i, rel);
			}
			int numFeatures = Integer.parseInt(featureReader.readLine());
			String ftr = null;
			featureList = new HashMap<Integer, String>();
			int fno = 0;
			while (fno < numFeatures) {
				ftr = featureReader.readLine().trim();
				String parts[] = ftr.split("\t");
				featNameNumMapping.put(parts[1], Integer.parseInt(parts[0]));
				featureList.put(fno, ftr);
				fno++;
			}
			featureReader.close();
		}

	}

	// the following two are actually not storing weights:
	// the first is storing the iteration in which the average weights were
	// last updated, and the other is storing the next update value
	private Parameters avgParamsLastUpdatesIter;
	private Parameters avgParamsLastUpdates;

	// The following parameter array stores the number of times a particular
	// parameter
	// has been updated, used for regularization in some sense.
	private Parameters lastZeroIter;
	private Parameters countZeroIter;

	private Parameters avgParameters;
	private Parameters iterParameters;
	
	//The following parameter array stores the number of times a particular 
	//parameter was updated, this will help in smoothing weights that are updated a lot (well that's the hope)
	private Parameters countUpdates;
	int avgIteration = 0;

	public Parameters train(Dataset<EntityGraph> trainingData) throws IOException {

		if (computeAvgParameters) {
			avgParameters = new Parameters();
			avgParameters.model = model;
			avgParameters.init();

			avgParamsLastUpdatesIter = new Parameters();
			avgParamsLastUpdates = new Parameters();
			avgParamsLastUpdatesIter.model = avgParamsLastUpdates.model = model;
			avgParamsLastUpdatesIter.init();
			avgParamsLastUpdates.init();

			lastZeroIter = new Parameters();
			lastZeroIter.model = model;
			lastZeroIter.init();

			countZeroIter = new Parameters();
			countZeroIter.model = model;
			countZeroIter.init();
			

			countUpdates = new Parameters();
			countUpdates.model = model;
			countUpdates.init();

		}

		iterParameters = new Parameters();
		iterParameters.model = model;
		iterParameters.init();

		if (debug) {
			obw = new BufferedWriter(new FileWriter(outputFile));
		}
		for (int i = 0; i < numIterations; i++) {
			if (debug) {

				obw.write("#######################################\n");
				obw.write("Iteration : " + i + "\n");
				obw.write("#######################################\n");

			}
			System.out.println("Iteration: " + i);
			trainingIteration(i, trainingData);
		}
		if (computeAvgParameters) {
			finalizeRel();
		}

		// GoldDbInference.printMatchStat();
		if (debug) {
			obw.close();
		}
		return (computeAvgParameters) ? avgParameters : iterParameters;
	}

	public void trainingIteration(int iteration, Dataset<EntityGraph> trainingData)
			throws IOException {

		EntityGraph egraph = new EntityGraph();

		trainingData.shuffle(random);

		trainingData.reset();
		while (trainingData.next(egraph)) {
			if (egraph.features.length == 0) {
				continue;
			}
			// compute most likely label under current parameters
			EntityGraphParse predictedParse = FullInferenceEntityGraph.infer(egraph, scorer,
					iterParameters);
			EntityGraphParse trueParse = ConditionalInferenceEntityGraph.infer(egraph, scorer,
					iterParameters);

			if (!NsAgree(predictedParse, trueParse)) {
				// if this is the first avgIteration, then we need to initialize
				// the lastUpdate vector
				if (computeAvgParameters && avgIteration == 0) {
					avgParamsLastUpdates.sum(iterParameters, 1.0f);
				}

				update(predictedParse, trueParse);
			}

			if (computeAvgParameters) {
				avgIteration++;
			}
		}
	}

	public void update(EntityGraphParse predictedParse, EntityGraphParse trueParse)
			throws IOException {
		// if this is the first avgIteration, then we need to initialize
		// the lastUpdate vector
		if (computeAvgParameters && avgIteration == 0)
			avgParamsLastUpdates.sum(iterParameters, 1.0f);
		EntityGraph egraph = predictedParse.graph;

		int numMentions = egraph.numMentions;
		for (int i = 0; i < numMentions; i++) {
			//
			// /*
			// * get the numeric features.
			// */
			// SparseBinaryVector v2a =
			// scorer.getMentionNumRelationFeatures(lrg, i, lrg.relNumber);
			//
			// Number n = lrg.n[i];
			// ArrayList<Integer> z_s = n.zs_linked;
			// for (Integer z : z_s) {
			// SparseBinaryVector v1a = scorer.getMentionRelationFeatures(lrg,
			// z, lrg.relNumber);
			//
			// if (trueParse.z_states[z] == true) {
			// // increase weight for the incorrect mention
			//
			// updateRel(lrg.relNumber, v1a, delta, computeAvgParameters);
			// updateRel(lrg.relNumber, v2a, delta, computeAvgParameters);
			// }
			// if (predictedParse.z_states[z] == true) {
			// // decrease weight for the incorrect mention
			// updateRel(lrg.relNumber, v1a, -delta, computeAvgParameters);
			// updateRel(lrg.relNumber, v2a, -delta, computeAvgParameters);
			//
			// }
			SparseBinaryVector v1a = scorer.getMentionFeatures(egraph, i);
			if (trueParse.z_states[i] > 0) {
				updateRel(trueParse.z_states[i], v1a, delta, computeAvgParameters);

			}
			if (predictedParse.z_states[i] > 0 && (predictedParse.z_states[i] != trueParse.z_states[i])) {
				updateRel(trueParse.z_states[i], v1a, -delta, computeAvgParameters);
			}

		}
	}

	private boolean NsAgree(EntityGraphParse predictedParse, EntityGraphParse trueParse) {
		int numN = predictedParse.n_states.length;
		if (numN != trueParse.n_states.length) {
			throw new IllegalArgumentException(
					"Something is not right in LocalAveragedPerceptron");
		}
		for (int i = 0; i < numN; i++) {
			if (predictedParse.n_states[i] != trueParse.n_states[i]) {
				return false;
			}
		}
		return true;
	}

	private void updateRel(int relNumber, SparseBinaryVector features,
			double delta, boolean useIterAverage) throws IOException {

		iterParameters.relParameters[relNumber].addSparse(features, delta);
		/*
		 * updating numeric features.
		 */

		// useIterAverage = false;
		if (useIterAverage) {

			DenseVector lastUpdatesIter = (DenseVector) avgParamsLastUpdatesIter.relParameters[relNumber];
			DenseVector lastUpdates = (DenseVector) avgParamsLastUpdates.relParameters[relNumber];
			DenseVector avg = (DenseVector) avgParameters.relParameters[relNumber];
			DenseVector iter = (DenseVector) iterParameters.relParameters[relNumber];

			DenseVector lastZeroIteration = (DenseVector) lastZeroIter.relParameters[relNumber];
			DenseVector zeroIterationCount = (DenseVector) countZeroIter.relParameters[relNumber];
			
			DenseVector updateCountVector = (DenseVector) countUpdates.relParameters[relNumber]; 
			
			for (int j = 0; j < features.num; j++) {
				int id = features.ids[j];
				updateCountVector.vals[id] += 1;
				if (lastUpdates.vals[id] != 0) {
					// avg.vals[id] += (avgIteration - lastUpdatesIter.vals[id])
					// * lastUpdates.vals[id];
					
				
					int notUpdatedWindow = avgIteration
							- (int) lastUpdatesIter.vals[id];
					avg.vals[id] = Math.pow(regulaizer, notUpdatedWindow)
							* avg.vals[id] + notUpdatedWindow
							* lastUpdates.vals[id];

					if (iter.vals[id] == 0) { // present 0
						assert (lastZeroIteration.vals[id] == -1);
						lastZeroIteration.vals[id] = avgIteration;
					} else if (lastZeroIteration.vals[id] != -1) {
						zeroIterationCount.vals[id] += (avgIteration - lastZeroIteration.vals[id]);
						lastZeroIteration.vals[id] = -1;
					}

					if (debug) {
						if (id == 527682) {
							obw.write("\n" + relNumNameMapping.get(relNumber)
									+ "--> " + delta + "\n");
							obw.write(lastUpdatesIter.vals[id] + "-->"
									+ avgIteration + "\n");
							obw.write(featureList.get(id) + " : "
									+ avg.vals[id] + "\n");
							obw.write("Iterval : " + iter.vals[id] + "\n");
							obw.write("*************************************\n");
						}
					}
				}
				lastUpdatesIter.vals[id] = avgIteration;
				lastUpdates.vals[id] = iter.vals[id];
			}
		}
	}

	private void finalizeRel() {
		for (int s = 0; s < model.numRelations; s++) {
			DenseVector lastUpdatesIter = (DenseVector) avgParamsLastUpdatesIter.relParameters[s];
			DenseVector lastUpdates = (DenseVector) avgParamsLastUpdates.relParameters[s];
			DenseVector avg = (DenseVector) avgParameters.relParameters[s];
			DenseVector zeroIterationCountRel = (DenseVector) countZeroIter.relParameters[s];
			
			DenseVector updateCountVector = (DenseVector) countUpdates.relParameters[s]; 
			
			for (int id = 0; id < avg.vals.length; id++) {
				if (lastUpdates.vals[id] != 0) {
					int notUpdatedWindow = avgIteration
							- (int) lastUpdatesIter.vals[id];
					avg.vals[id] = Math.pow(regulaizer, notUpdatedWindow)
							* avg.vals[id] + notUpdatedWindow
							* lastUpdates.vals[id];

					int nonZeroIteration = avgIteration
							- (int) zeroIterationCountRel.vals[id];
					// System.out.println("Nonziteration  = " + nonZeroIteration
					// );
					if (this.finalAverageCalc) {
						avg.vals[id] = updateCountVector.vals[id] == 0 ? avg.vals[id] : (avg.vals[id] / updateCountVector.vals[id]);
					}
					lastUpdatesIter.vals[id] = avgIteration;
				}
			}
		}
	}
}
