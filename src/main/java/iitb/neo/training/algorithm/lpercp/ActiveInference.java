package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.HashSet;
import java.util.List;

import edu.washington.multirframework.multiralgorithm.Parameters;
import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.ds.Number;

public class ActiveInference implements ConditionalInference {

	public Parse infer(LRGraph lrg, Scorer scorer, Parameters params) {
		scorer.setParameters(params);
		Parse p = new Parse();
		p.graph = lrg;
		p.z_states = new boolean[lrg.Z.length];
		p.n_states = new boolean[lrg.n.length];
		int numN = lrg.n.length;
		Integer labelTrue = LocalAveragedPerceptron.featNameNumMapping
				.get("hard: " + lrg.relation);
		Integer labelFalse = LocalAveragedPerceptron.featNameNumMapping
				.get("hard: N_" + lrg.relation);

		for (int z = 0; z < lrg.Z.length; z++) {
			HashSet<Integer> feats = new HashSet<>();

			for (int id : lrg.features[z].ids) {
				feats.add(id);
			}

			if (feats.contains(labelTrue)) {
				p.z_states[z] = true;
			} else if (feats.contains(labelFalse)) {
				p.z_states[z] = false;
			}
		}
		for (int n_i = 0; n_i < numN; n_i++) {
			if (GoldDbInference.closeEnough(lrg.n[n_i].value, lrg.relation,
					lrg.location)) {
				Number n = lrg.n[n_i];
				List<Integer> z_s = n.zs_linked;
				p.n_states[n_i] = false;
				for(int z: z_s) {
					if(p.z_states[z] == true) {
						p.n_states[n_i] = true;
						break;
					}
				}

			} else {
				p.n_states[n_i] = false;
			}
		}
		return p;
	}

}
