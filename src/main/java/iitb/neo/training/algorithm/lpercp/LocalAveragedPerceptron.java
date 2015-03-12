package main.java.iitb.neo.training.algorithm.lpercp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import main.java.iitb.neo.NtronExperiment;
import main.java.iitb.neo.training.ds.LRGraph;
import edu.washington.multirframework.multiralgorithm.Dataset;
import edu.washington.multirframework.multiralgorithm.DenseVector;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;
import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;

/**
 * Simple percepton-ish algorithm for learning weights. Averaged because the
 * parameters are averaged across the runs
 * 
 * @author aman/ashish
 * 
 */
public class LocalAveragedPerceptron {
	public int maxIterations = 5;
	public boolean computeAvgParameters = true;
	public boolean updateOnTrueY = true;
	public double delta = 1;

	private Scorer scorer;
	private Model model;
	private Random random;

	public LocalAveragedPerceptron(Model model, Random random) {
		scorer = new Scorer();
		this.model = model;
		this.random = random;
	}

	// the following two are actually not storing weights:
	// the first is storing the iteration in which the average weights were
	// last updated, and the other is storing the next update value
	private Parameters avgParamsLastUpdatesIter;
	private Parameters avgParamsLastUpdates;

	private Parameters avgParameters;
	private Parameters iterParameters;

	public Parameters train(Dataset trainingData) throws IOException {

		if (computeAvgParameters) {
			avgParameters = new Parameters();
			avgParameters.model = model;
			avgParameters.init();

			avgParamsLastUpdatesIter = new Parameters();
			avgParamsLastUpdates = new Parameters();
			avgParamsLastUpdatesIter.model = avgParamsLastUpdates.model = model;
			avgParamsLastUpdatesIter.init();
			avgParamsLastUpdates.init();
		}

		iterParameters = new Parameters();
		iterParameters.model = model;
		iterParameters.init();

		for (int i = 0; i < maxIterations; i++){
			
			trainingIteration(i, trainingData);
			avgParameters.serialize("data/test-extractor/params");
			NtronExperiment.writeFeatureWeights("data/test-extractor/mapping", "data/test-extractor/params", "data/test-extractor/model", "wt_"+i);
		}
		if (computeAvgParameters)
			finalizeRel();

		return (computeAvgParameters) ? avgParameters : iterParameters;
	}

	int avgIteration = 0;

	@SuppressWarnings("unchecked")
	public void trainingIteration(int iteration,
			@SuppressWarnings("rawtypes") Dataset trainingData) {
		System.out.println("iteration " + iteration);

		LRGraph lrg = new LRGraph();

		trainingData.shuffle(random);

		trainingData.reset();

		while (trainingData.next(lrg)) {

			if (lrg.features.length == 0) {
				continue;
			}

			// compute most likely label under current parameters
			Parse predictedParse = FullInference.infer(lrg, scorer,
					iterParameters);
			Parse trueParse = ConditionalInference.infer(lrg, scorer,
					iterParameters);

			if (!NsAgree(predictedParse, trueParse)) {
				// if this is the first avgIteration, then we need to initialize
				// the lastUpdate vector
				if (computeAvgParameters && avgIteration == 0)
					avgParamsLastUpdates.sum(iterParameters, 1.0f);

				System.out.println("Updating params");
				update(predictedParse, trueParse);
			}

			if (computeAvgParameters)
				avgIteration++;
		}
	}

	public void update(Parse predictedParse, Parse trueParse) {
		// if this is the first avgIteration, then we need to initialize
		// the lastUpdate vector
		if (computeAvgParameters && avgIteration == 0)
			avgParamsLastUpdates.sum(iterParameters, 1.0f);
		LRGraph lrg = predictedParse.graph;
		int numMentions = lrg.numMentions;
		for (int i = 0; i < numMentions; i++) {
			SparseBinaryVector v1a = scorer.getMentionRelationFeatures(lrg, i,
					lrg.relNumber);

			if (trueParse.z_states[i] == true) {
				// increase weight for the incorrect mention
				System.out.println("->");
				updateRel(lrg.relNumber, v1a, delta, computeAvgParameters);
				System.out.println(avgParameters);

			}
			if (predictedParse.z_states[i] == true) {
				// decrease weight for the incorrect mention
				updateRel(lrg.relNumber, v1a, -delta, computeAvgParameters);
				System.out.println("<-");
				System.out.println(avgParameters);
			}
		}
	}

	/*
	 * Parse trueParse = GoldDbInference.infer(lrg); if
	 * (!NsAgree(predictedParse, trueParse)) { // if this is the first
	 * avgIteration, then we need to initialize // the lastUpdate vector if
	 * (computeAvgParameters && avgIteration == 0)
	 * avgParamsLastUpdates.sum(iterParameters, 1.0f); int numN =
	 * predictedParse.n_states.length; for(int i = 0; i < numN; i++) {
	 * if(predictedParse.n_states[i] != trueParse.n_states[i]) {
	 * ArrayList<Integer> linkedMentions = trueParse.graph.n[i].zs_linked;
	 * for(int m: linkedMentions) { SparseBinaryVector v1a =
	 * scorer.getMentionRelationFeatures(trueParse.graph, m, lrg.relNumber);
	 * //increase weight for the correct mention
	 * 
	 * //decrease weight for the incorrect mention for(int r = 0; r <
	 * model.numRelations; r++) { if(r == lrg.relNumber) { updateRel(r, v1a,
	 * delta, computeAvgParameters); } else { updateRel(r, v1a, -delta,
	 * computeAvgParameters); } }
	 * 
	 * }
	 * 
	 * } } }
	 */

	private boolean NsAgree(Parse predictedParse, Parse trueParse) {
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

	private void updateRel(int toState, SparseBinaryVector features,
			double delta, boolean useIterAverage) {
		iterParameters.relParameters[toState].addSparse(features, delta);
		// useIterAverage = false;
		if (useIterAverage) {
			DenseVector lastUpdatesIter = (DenseVector) avgParamsLastUpdatesIter.relParameters[toState];
			DenseVector lastUpdates = (DenseVector) avgParamsLastUpdates.relParameters[toState];
			DenseVector avg = (DenseVector) avgParameters.relParameters[toState];
			DenseVector iter = (DenseVector) iterParameters.relParameters[toState];
			for (int j = 0; j < features.num; j++) {
				int id = features.ids[j];
				if (lastUpdates.vals[id] != 0)
					avg.vals[id] += (avgIteration - lastUpdatesIter.vals[id]) * lastUpdates.vals[id];

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
			for (int id = 0; id < avg.vals.length; id++) {
				if (lastUpdates.vals[id] != 0) {
					avg.vals[id] += (avgIteration - lastUpdatesIter.vals[id])
							* lastUpdates.vals[id];
					lastUpdatesIter.vals[id] = avgIteration;
				}
			}
		}
	}
}
