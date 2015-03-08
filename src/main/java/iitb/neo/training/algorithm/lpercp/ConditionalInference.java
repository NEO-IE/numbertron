package main.java.iitb.neo.training.algorithm.lpercp;

import main.java.iitb.neo.training.ds.LRGraph;
import edu.washington.multirframework.multiralgorithm.Parameters;



/*
 * @author ashish
 */
public class ConditionalInference {
	public static Parse infer(LRGraph lrg, Scorer scorer, Parameters params) {
				
			System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
			Parse p = new Parse();
			p.graph = lrg;
			scorer.setParameters(params);
			int numMentions = lrg.numMentions;
			
			Parse trueParse = GoldDbInference.infer(lrg);
			
			Viterbi viterbi = new Viterbi(params.model, scorer);
			Viterbi.Parse[] vp = new Viterbi.Parse[numMentions];
			for (int m = 0; m < numMentions; m++) {
				vp[m] = viterbi.parse(lrg, m);
			}
			
			// each mention can be linked to one of the numbers
			int numRelevantNumbers = lrg.n.length;
			
			// solve bipartite graph matching problem ??
			// Edge[] es = new Edge[numMentions * numRelevantNumbers];
			for (int m = 0; m < numMentions; m++) {
				for (int no = 0; no < numRelevantNumbers; no++)
					if(lrg.n[no].zs_linked.contains(m)){
						
					}
			}
			
			
			
			
			return null;
	}
	static class Edge {
		int m;
		int y;
		double score;		
		Edge(int m, int y, double score) {
			this.m = m;
			this.y = y;
			this.score = score;
		}
	}
}
