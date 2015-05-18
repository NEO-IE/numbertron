package main.java.iitb.neo.training.algorithm.lpercp;

import main.java.iitb.neo.training.ds.LRGraph;

public class ActiveInference {

	public static Parse infer(LRGraph lrg) {
		Parse p = new Parse();
		p.graph = lrg;
		p.z_states = new boolean[lrg.Z.length];
		p.n_states = new boolean[lrg.n.length];
		int numN = lrg.n.length;
		for (int n_i = 0; n_i < numN; n_i++) {
			if (GoldDbInference.closeEnough(lrg.n[n_i].value, lrg.relation, lrg.location)) {
				
				p.n_states[n_i] = true;
			} else {
				p.n_states[n_i] = false;
			}
		}
		return p;
	}

}
