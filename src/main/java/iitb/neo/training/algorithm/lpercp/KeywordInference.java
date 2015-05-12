package main.java.iitb.neo.training.algorithm.lpercp;

import iitb.rbased.meta.KeywordData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.ds.Number;
import main.java.iitb.neo.util.LemmaUtils;
import main.java.iitb.neo.util.StemUtils;
import main.java.org.tartarus.snowball.SnowballProgram;
import main.java.org.tartarus.snowball.SnowballStemmer;

/**
 * Sets the value of n nodes based on the value pulled from the gold db
 * 
 * @author aman
 * 
 */
public class KeywordInference {

	public static HashMap<String, Double> marginMap;
	
	static {
		marginMap = new HashMap<String, Double>();
	}

	public static Parse infer(LRGraph lrg) {
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
			p.n_states[n_i] = hasKeyword(feats, lrg.relation);
		}
		return p;
	}
	
	public static boolean hasKeyword(HashSet<Integer> feats, String rel){
		List<String>  relKey = KeywordData.REL_KEYWORD_MAP.get(rel);
		for(String key : relKey){
			String stemKey = StemUtils.getStemWord(key.toLowerCase());
			System.err.println(stemKey);
			Integer featID = LocalAveragedPerceptron.featNameNumMapping.get("key: "+stemKey);
			if(featID != null){
				if(feats.contains(featID)){
					return true;
				}
			}else{
				//System.err.println("key: " + key.toLowerCase());
			}
		}
		return false;
	}
}