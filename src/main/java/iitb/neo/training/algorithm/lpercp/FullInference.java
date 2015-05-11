package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.util.MapUtils;
import edu.washington.multirframework.multiralgorithm.Parameters;

/**
 * Assigns a binary label to all the nodes given the current weights;
 * 
 * @author aman
 * 
 */
public class FullInference {
	public static Parse infer(LRGraph lrg, Scorer scorer, Parameters params) {
		Parse p = new Parse();
		/* setup what we already know about the parse */
		p.graph = lrg;
		scorer.setParameters(params);
		p.z_states = new boolean[lrg.Z.length];
		p.n_states = new boolean[lrg.n.length];
		/* iterate over the Z nodes and set them to true whenever applicable */
		int numZ = lrg.Z.length;
		for (int z = 0; z < numZ; z++) {
			double bestScore = 0.0;

			// There can be multiple "best" relations. It is okay if we get
			// anyone of them
			ArrayList<Integer> bestRels = new ArrayList<Integer>();
			for (int r = 0; r < params.model.numRelations; r++) {
				double currScore = scorer.scoreMentionRelation(lrg, z, r);
				if (currScore > bestScore) {
					bestRels.clear();
					bestRels.add(r);
					bestScore = currScore;
				} else if (bestScore > 0 && currScore == bestScore) {
					bestRels.add(r);
				}
			}
			if (bestRels.contains(lrg.relNumber)) {
				p.z_states[z] = true;
			} else {
				p.z_states[z] = false;
			}
		}

		/* now flip n nodes accordingly: OR */
		int numN = lrg.n.length;
		for (int n_i = 0; n_i < numN; n_i++) {
			ArrayList<Integer> attachedZ = lrg.n[n_i].zs_linked;
			p.n_states[n_i] = false;
			for (Integer z : attachedZ) { // iterate over all the attached Z
											// nodes
				if (p.z_states[z]) { // if any of them is one, set the number
										// node to 1
					p.n_states[n_i] = true;
					break;
				}
			}
		}
		return p;
	}

	

	/**
	 * Returns a map with score of all the relations
	 * 
	 * @param lrg
	 * @param scorer
	 * @param params
	 * @return
	 */
	public static Map<Integer, Double> getRelationScoresPerMention(LRGraph lrg, Scorer scorer, Parameters params) {
		Parse p = new Parse();
		/* setup what we already know about the parse */
		p.graph = lrg;
		scorer.setParameters(params);
		p.z_states = new boolean[lrg.Z.length];

		/* iterate over the Z nodes and set them to true whenever applicable */
		int numZ = lrg.Z.length;
		assert (numZ == 1);
		HashMap<Integer, Double> relationScoreMap = new HashMap<Integer, Double>();
		for (int r = 0; r < params.model.numRelations; r++) {
			relationScoreMap.put(r, scorer.scoreMentionRelation(lrg, 0, r));
		}
		
		return MapUtils.sortByValue(relationScoreMap);
	}
}
