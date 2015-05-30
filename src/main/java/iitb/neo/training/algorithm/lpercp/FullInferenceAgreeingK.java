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
import main.java.iitb.neo.util.MathUtils;
import edu.washington.multirframework.multiralgorithm.Parameters;

/**
 * Assigns a binary label to all the nodes given the current weights;
 *For numbers, we want the one-labeled nodes to be proximal.
 * In this scheme we start with the \textbf{Atleast-K} assignment 
 * (call it $\bar{\vn}$) and set to zero any $n^r_q$ outside a range of
 *  $\pm \delta_r\%$ of a chosen central value.  We choose the central value 
 *  $c$ for which $\bar{n}_c^r=1$ and which causes smallest number of
 *   $\bar{n}_q^r=1$ to set to zero. 
 * @author aman
 * 
 */

public class FullInferenceAgreeingK {
	
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
		double LEAST_Z_FLIPPED_COUNT = 0.5;
		
		/**
		 * Now get the Atleast-K assignments to the number nodes
		 */
		int numN = lrg.n.length;
		for (int n_i = 0; n_i < numN; n_i++) {
			ArrayList<Integer> attachedZ = lrg.n[n_i].zs_linked;
			
			int totalZ = attachedZ.size();
			p.n_states[n_i] = false;
			int trueAttachedZCount = 0;
			for (Integer z : attachedZ) { // iterate over all the attached Z
											// nodes
				if (p.z_states[z]) { // if any of them is one, set the number // node to 1
					trueAttachedZCount++;
					p.n_states[n_i] = (((trueAttachedZCount * 1.0) / (totalZ)) >= LEAST_Z_FLIPPED_COUNT);
				}
			}
		}
		
		/*
		 * Now start the agreeing k 
		 */
		//Different deltas for different relations
		double delta = GoldDbInference.marginMap.get(lrg.relation);
		int leastFlips = Integer.MAX_VALUE;
		double bestCentralValue = -1;
		/**
		 * Find the optimal central value
		 */
		for (int n_i = 0; n_i < numN; n_i++) {
			if(p.n_states[n_i]) { //potential central value?
				int flipsCaused = 0;
				double centralValue = lrg.n[n_i].value;
				for (int n_c = 0; n_c < numN; n_c++) {
					if(n_c == n_i) {
						continue;
					}
					if(p.n_states[n_c] && !(MathUtils.within(lrg.n[n_c].value, centralValue, delta))) { //check if this guy must be turned off
						flipsCaused++;
					}
				}
				if(flipsCaused < leastFlips) {
					leastFlips = flipsCaused;
					bestCentralValue = centralValue;
				}
					
			}
		}
		assert(bestCentralValue != -1);
		//complete by actually flipping the n nodes that do not agree
		for (int n_i = 0; n_i < numN; n_i++) {
			if(p.n_states[n_i] && !(MathUtils.within(lrg.n[n_i].value, bestCentralValue, delta))) { //check if this guy must be turned off
				p.n_states[n_i] = false;
			}
		}
		
		//Also set to 1 those n nodes that are zero but witnin the limits
		for (int n_i = 0; n_i < numN; n_i++) {
			if(!p.n_states[n_i] && (MathUtils.within(lrg.n[n_i].value, bestCentralValue, delta))) { //check if this guy must be turned off
				p.n_states[n_i] = true;
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
