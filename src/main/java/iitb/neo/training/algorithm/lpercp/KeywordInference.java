package main.java.iitb.neo.training.algorithm.lpercp;

import iitb.rbased.meta.KeywordData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.washington.multirframework.multiralgorithm.Parameters;
import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.ds.Number;
import main.java.iitb.neo.util.StemUtils;

/**
 * Sets the value of n nodes based on the value pulled from the gold db
 * 
 * @author ashish
 * 
 */
public class KeywordInference implements ConditionalInference {

	public static HashMap<String, Double> marginMap;
	
	static {
		marginMap = new HashMap<String, Double>();
	}

	public Parse infer(LRGraph lrg, Scorer scorer, Parameters params) {
		scorer.setParameters(params);
		Parse p = new Parse();
		p.graph = lrg;
		p.z_states = new boolean[lrg.Z.length];
		p.n_states = new boolean[lrg.n.length];
		int numN = lrg.n.length;
		for (int n_i = 0; n_i < numN; n_i++) {
			HashSet<Integer> feats = new HashSet<>();
			Number n = lrg.n[n_i];
			List<Integer> z_s = n.zs_linked;
			for(Integer z: z_s){
				for(int id: lrg.features[z].ids){
					feats.add(id);
				}
			}
			//Make the number true if one of the Z nodes attached expresses the relation
			p.n_states[n_i] = hasKeyword(feats, lrg.relation);
		}
		return p;
	}
	
	public static boolean hasKeyword(HashSet<Integer> feats, String rel){
		List<String>  relKey = KeywordData.REL_KEYWORD_MAP.get(rel);
		for(String key : relKey){
			String stemKey = StemUtils.getStemWord(key.toLowerCase());
			Integer featID = LocalAveragedPerceptron.featNameNumMapping.get("key: "+stemKey);
			if(featID != null){
				if(feats.contains(featID)){
					return true;
				}
			}
		}
		return false;
	}
}
