package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.HashSet;
import java.util.List;

import edu.washington.multirframework.multiralgorithm.Parameters;
import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.ds.Number;

public class ActiveInference implements ConditionalInference {

	public Parse infer(LRGraph lrg, Scorer scorer, Parameters params) {
		scorer.setParameters(params);
		Parse p = new GoldDbInference().infer(lrg, scorer, params);
		
		
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
		return p;
	}

}
