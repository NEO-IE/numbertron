package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.washington.multirframework.multiralgorithm.Mappings;
import main.java.iitb.neo.pretrain.process.MakeEntityGraph;
import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.ds.Number;
import meta.RelationMetaData;

/**
 * Sets the value of n nodes based on the value pulled from the gold db
 * 
 * @author ashish
 * 
 */
public class GoldDBKeywordInferenceEntityGraph {

	public static HashMap<String, Double> marginMap;

	static {
		marginMap = new HashMap<String, Double>();
	}

	public static EntityGraphParse infer(EntityGraph lrg) {
		EntityGraphParse p = new EntityGraphParse();
		p.graph = lrg;
		p.z_states = new int[lrg.numMentions];
		p.n_states = new boolean[lrg.numNodesCount][RelationMetaData.NUM_RELATIONS + 1];
		int numN = lrg.numNodesCount;
		for (int n_i = 0; n_i < numN; n_i++) {
			HashSet<Integer> feats = new HashSet<>();
			Number n = lrg.n[n_i];
			List<Integer> z_s = n.zs_linked;
			for (Integer z : z_s) {
				for (int id : lrg.s[z].features.ids) {
					feats.add(id);
				}
			}
			Mappings m = MakeEntityGraph.mapping;
			ArrayList<Integer> validIdx = RelationMetaData.unitRelationMap
					.get(n.unit);
			for (String relation : RelationMetaData.relationNames) {
				int relNumber = m.getRelationID(relation, false);
				if (!validIdx.contains(relNumber)) {
					continue;
				}
				if (GoldDbInference.closeEnough(n.value, relation, lrg.entity)) {
					p.n_states[n_i][relNumber] = KeywordInference.hasKeyword(
							feats, relation);
				} else {
					p.n_states[n_i][relNumber] = false;
				}

			}
		}
		return p;
	}
}
