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
	
	private static int K = 10;
	private static double MARGIN = 0.2; //allow true values to be within 20%
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

	/**
	 * match with a specified cutoff
	 * @param value
	 * @param rel
	 * @param entity
	 * @param margin
	 * @return
	 */
	public static boolean closeEnough(Double value, String rel, String entity, double margin) {
		double bu = MARGIN;
		MARGIN = margin;
		boolean res = closeEnough(value, rel, entity);
		MARGIN = bu;
		return res;
	}
	
	public static boolean closeEnough(Double value, String rel, String entity) {
		// TODO Auto-generated method stub
		rel = rel.split("&")[0];
		ArrayList<Double> goldValues = GoldDB.getGoldDBValue(entity, rel, K);
		
		for(Double val : goldValues){
			
			Double valueSlack = MARGIN * val; // +- 5 percent
			//System.out.print(val + "\t");
			if((value > (val- valueSlack)) && (value < (val + valueSlack))){	
				return true;
			}
		}
	
		return false;
		//return true;
	}

}
