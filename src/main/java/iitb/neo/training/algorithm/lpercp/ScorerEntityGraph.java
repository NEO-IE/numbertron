package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.Map;

import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.ds.Graph;
import edu.washington.multirframework.multiralgorithm.DenseVector;
import edu.washington.multirframework.multiralgorithm.Parameters;
import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;

public class ScorerEntityGraph {
	private Parameters params;

	public ScorerEntityGraph() {
	}

	// scoring on mention documents, all 2*numRelation
	public double scoreMentionRelation(EntityGraph egraph, int m, int rel,
			Map<Integer, Double> featureScoreMap) {
		double sum = 0;
		DenseVector p = params.relParameters[rel];
		sum += p.dotProduct(egraph.s[m].features, featureScoreMap);
		return sum;
	}
	// scoring on mention documents, all 2*numRelation
		public double scoreMentionRelation(EntityGraph doc, int m, int rel) {
			double sum = 0;
			DenseVector p = params.relParameters[rel];
			sum += p.dotProduct(doc.s[m].features);
			return sum;
		}


	// need to consider additional features that are dependent on rel ...
	public SparseBinaryVector getMentionRelationFeatures(EntityGraph doc, int m,
			int rel) {
		return doc.s[m].features;
	}


	/**
	 * returns the features stored in the mth instance of the Graph
	 * @param doc
	 * @param m
	 * @param rel
	 * @return
	 */
	public SparseBinaryVector getMentionFeatures(EntityGraph doc, int m) {
		return doc.s[m].features;
	}

	public void setParameters(Parameters params) {
		this.params = params;
	}
}
