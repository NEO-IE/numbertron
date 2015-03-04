package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.ArrayList;

import main.java.iitb.neo.goldDB.GoldDB;
import main.java.iitb.neo.training.ds.LRGraph;

/**
 * Sets the value of n nodes based on the value pulled from the gold db
 * @author aman
 *
 */
public class GoldDbInference {
	
	private static int K = 3;
	
	public static Parse infer(LRGraph lrg) {
		Parse p = new Parse();
		p.graph = lrg;
		p.z_states = new boolean[lrg.Z.length];
		p.n_states = new boolean[lrg.n.length];
		int numN = lrg.n.length;
		for(int n_i = 0; n_i < numN; n_i++) {
			if(closeEnough(lrg.n[n_i].value, lrg.relation, lrg.location)) {
				p.n_states[n_i] = true;
			} else {
				p.n_states[n_i] = false;
			}
		}
		return p;
	}

	private static boolean closeEnough(Double value, String rel, String entity) {
		// TODO Auto-generated method stub
		
//		ArrayList<Double> goldValues = GoldDB.getGoldDBValue(entity, rel, K);
//		for(Double val : goldValues){
//			Double valueSlack = 0.05 * val; // +- 5 percent
//			if((value > (val- valueSlack)) && (value < (val + valueSlack))){
//					return true;
//			}
//		}
		return true;
		//return false;
	}

}
