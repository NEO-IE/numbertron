package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.HashSet;
import java.util.List;

import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.ds.Number;

public class ActiveInference {

	public static Parse infer(LRGraph lrg) {
		Parse p = new Parse();
		p.graph = lrg;
		p.z_states = new boolean[lrg.Z.length];
		p.n_states = new boolean[lrg.n.length];
		int numN = lrg.n.length;
		Integer labelTrue = LocalAveragedPerceptron.featNameNumMapping.get("hard: "+ lrg.relation);
		Integer labelFalse = LocalAveragedPerceptron.featNameNumMapping.get("hard: N_"+ lrg.relation);
		for (int n_i = 0; n_i < numN; n_i++) {
			if (GoldDbInference.closeEnough(lrg.n[n_i].value, lrg.relation, lrg.location)) {
				HashSet<Integer> feats = new HashSet<>();
				Number n = lrg.n[n_i];
				List<Integer> z_s = n.zs_linked;
				for(Integer z: z_s) {
					for(int id: lrg.features[z].ids){
						feats.add(id);
					}
				}
				if(feats.contains(labelTrue)) {
					p.n_states[n_i] = true;
				} else if (feats.contains(labelFalse)){
					p.n_states[n_i] = false;
				} else {
					p.n_states[n_i] = true;
				}
				
			} else {
				p.n_states[n_i] = false;
			}
		}
		return p;
	}

}
