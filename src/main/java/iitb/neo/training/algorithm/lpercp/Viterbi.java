package main.java.iitb.neo.training.algorithm.lpercp;

import main.java.iitb.neo.training.ds.LRGraph;
import edu.washington.multirframework.multiralgorithm.Model;

public class Viterbi {
	private Scorer parseScorer;
	private Model model;
	
	public Viterbi(Model model, Scorer parseScorer) {
		this.model = model;
		this.parseScorer = parseScorer;
	}
	
	public Parse parse(LRGraph doc, int mention) {
		int numRelations = model.numRelations;

		// relation X argsReversed
		double[] scores = new double[numRelations];
				
		// lookup signature
		for (int s = 0; s < numRelations; s++)
			scores[s] = parseScorer.scoreMentionRelation(doc, mention, s);

		int bestRel = 0;
		for (int r = 0; r < model.numRelations; r++) {
			if (scores[r] > scores[bestRel]) {
				bestRel = r; }
		}
		//run again to set the featureScoreMap to the one for the highest relation
		parseScorer.scoreMentionRelation(doc, mention, bestRel);

		Parse p = new Parse(bestRel, scores[bestRel]);
		p.scores = scores;
		return p;
	}
	public static class Parse {
		// MPE
		public int state;
		public double score;
		
		// scores of all assignments
		public double[] scores;
		
		Parse(int state, double score) {
			this.state = state;
			this.score = score;
		}
	}

}
