package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.ds.Number;
import scala.actors.threadpool.Arrays;
import edu.stanford.nlp.util.ArrayUtils;
import edu.washington.multirframework.multiralgorithm.Parameters;

/**
 * Sets the value of n nodes based on the value pulled from the gold db
 * 
 * @author ashish
 * 
 */
public class GoldDBKeywordInference implements ConditionalInference {

	public static HashMap<String, Double> marginMap;

	static {
		marginMap = new HashMap<String, Double>();
	}

	/**
	 * The number should be close enough, and one of the Z nodes should have a
	 * keyword
	 */
	public Parse infer(LRGraph lrg, Scorer scorer, Parameters params) {
		scorer.setParameters(params);
		Parse p = new Parse();
		p.graph = lrg;
		p.z_states = new boolean[lrg.Z.length];
		p.n_states = new boolean[lrg.n.length];
		int numN = lrg.n.length;
		// first set the Z nodes
		for (int n_i = 0; n_i < numN; n_i++) {
			HashSet<Integer> feats = new HashSet<>();
			Number n = lrg.n[n_i];
			List<Integer> z_s = n.zs_linked;

			for (Integer z : z_s) {
				HashSet<Integer> zfeats = new HashSet<Integer>();
				for (int id : lrg.features[z].ids) {
					feats.add(id);
					zfeats.add(id);
				}
				p.z_states[z] = KeywordInference.hasKeyword(zfeats,
						lrg.relation);
			}
			if (GoldDbInference.closeEnough(lrg.n[n_i].value, lrg.relation,
					lrg.location)) {
				p.n_states[n_i] = KeywordInference.hasKeyword(feats,
						lrg.relation);
			} else {
				p.n_states[n_i] = false;
			}
		}

	
		return p;
	}
}
