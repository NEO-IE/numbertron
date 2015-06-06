package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import main.java.iitb.neo.pretrain.process.MakeEntityGraph;
import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.util.MapUtils;
import meta.RelationMetaData;
import edu.washington.multirframework.multiralgorithm.Mappings;
import edu.washington.multirframework.multiralgorithm.Parameters;

/**
 * Assigns a label to all the nodes given the current weights;
 * builds on the entity graph implementation rather than the 
 * entity-location implementation used earlier
 * 
 * @author aman
 * 
 */
public class FullInferenceEntityGraph {
	public static EntityGraphParse infer(EntityGraph egraph, Scorer scorer, Parameters params) {
		EntityGraphParse p = new EntityGraphParse();
		/* setup what we already know about the parse */
		p.graph = egraph;
		scorer.setParameters(params);
		p.z_states = new int[egraph.Z.length];
		p.n_states = new boolean[egraph.numNodesCount][RelationMetaData.NUM_RELATIONS + 1];
		/* iterate over the Z nodes and set them to true whenever applicable */
		int numZ = egraph.Z.length;
		for (int z = 0; z < numZ; z++) {
			double bestScore = 0.0;
			// There can be multiple "best" relations. It is okay if we get
			// anyone of them
			ArrayList<Integer> bestRels = new ArrayList<Integer>();
			for (int r = 0; r < params.model.numRelations; r++) {
				double currScore = scorer.scoreMentionRelation(egraph, z, r);
				if (currScore > bestScore) {
					bestRels.clear();
					bestRels.add(r);
					bestScore = currScore;
					p.z_states[z] = r;
				} else if (bestScore > 0 && currScore == bestScore) {
					bestRels.add(r);
				}
			}
			
		}
		Mappings m = MakeEntityGraph.mapping;
		
		double LEAST_Z_FLIPPED_COUNT = 0.5;
		/* now flip n nodes accordingly: OR */
		int numN = egraph.n.length;
		for (int n_i = 0; n_i < numN; n_i++) {
			//the set of Zs attached will be same for all the numbers, so just take anyone and work
			ArrayList<Integer> attachedZ = egraph.n[n_i][0].zs_linked;
			int totalZ = attachedZ.size();
			for(String relation: RelationMetaData.relationNames) {
				int relNumber = m.getRelationID(relation, false);
				p.n_states[n_i][relNumber] = false;
				
				
				for (Integer z : attachedZ) { // iterate over all the attached Z nodes
					if (p.z_states[z] > 0) { //0 is reserved for the relation NA
						p.n_states[n_i][p.z_states[z]] = true;
					}
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
