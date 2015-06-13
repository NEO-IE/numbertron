package main.java.iitb.neo.training.algorithm.lpercp;

import iitb.rbased.meta.KeywordData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import main.java.iitb.neo.pretrain.process.MakeEntityGraph;
import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.ds.Number;
import main.java.iitb.neo.util.StemUtils;
import meta.RelationMetaData;

/**
 * Sets the value of n nodes based on the value pulled from the gold db
 * 
 * @author ashish
 * 
 */
public class KeywordInferenceEntityGraph {

	public static HashMap<String, Double> marginMap;

	static {
		marginMap = new HashMap<String, Double>();
	}

	public static EntityGraphParse infer(EntityGraph egraph) {
		EntityGraphParse p = new EntityGraphParse();
		p.graph = egraph;
		p.z_states = new int[egraph.numMentions];
		p.n_states = new boolean[egraph.numNodesCount][RelationMetaData.NUM_RELATIONS + 1];
		int numN = egraph.numNodesCount;
		for (int n_i = 0; n_i < numN; n_i++) {
			HashSet<Integer> feats = new HashSet<>();
			Number n = egraph.n[n_i];
			List<Integer> z_s = n.zs_linked;
			for (Integer z : z_s) {
				for (int id : egraph.s[z].features.ids) {
					feats.add(id);
				}
			}
			// Make the number true if one of the Z nodes attached expresses the
			// relation
			List<Integer> validIdx = RelationMetaData.unitRelationMap
					.get(n.unit);
			for (String rel : RelationMetaData.relationNames) {
				int relNumber = MakeEntityGraph.mapping.getRelationID(rel, false);
				if (!validIdx.contains(relNumber)) {
					continue;
				}
				p.n_states[n_i][relNumber] = hasKeyword(feats, rel);
			}
		}
		return p;
	}

	public static boolean hasKeyword(HashSet<Integer> feats, String rel) {
		List<String> relKey = KeywordData.REL_KEYWORD_MAP.get(rel);
		for (String key : relKey) {
			String stemKey = StemUtils.getStemWord(key.toLowerCase());
			Integer featID = MakeEntityGraph.mapping.getFt2ftId().get("key: " + stemKey);
			
			if (featID != null) {
				if (feats.contains(featID)) {
					return true;
				}
			}
		}
		return false;
	}
}
