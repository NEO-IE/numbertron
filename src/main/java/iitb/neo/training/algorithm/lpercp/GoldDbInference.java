package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.ArrayList;
import java.util.HashMap;

import main.java.iitb.neo.goldDB.GoldDB;
import main.java.iitb.neo.training.ds.LRGraph;

/**
 * Sets the value of n nodes based on the value pulled from the gold db
 * @author aman
 *
 */
public class GoldDbInference {


	public static HashMap<String, Integer> countRel = new HashMap<String, Integer>();
	public static HashMap<String, Double> marginMap;
	static 
	{
		marginMap  = new HashMap<String, Double>();
		Double xs = 0.05;
		Double s = 0.1;
		Double regular = 0.2;
		Double xl = 0.3;
		Double xxl = 0.5;
		
		
		
		marginMap.put("AGL", xs);
		
		marginMap.put("FDI", s);
		marginMap.put("GOODS", s);
		marginMap.put("GDP", s);
		
		marginMap.put("ELEC", s);
		
		marginMap.put("CO2", s);
		
		//unitMap.put("DIESEL", "united states dollar per litre");
		
		marginMap.put("INF", xxl);
		marginMap.put("INTERNET", xxl);
		
		marginMap.put("LIFE", regular);
		
		marginMap.put("POP", s);
		
	}
	public static Parse infer(LRGraph lrg) {
		Parse p = new Parse();
		p.graph = lrg;
		p.z_states = new boolean[lrg.Z.length];
		p.n_states = new boolean[lrg.n.length];
		int numN = lrg.n.length;
		for(int n_i = 0; n_i < numN; n_i++) {
			if(closeEnough(lrg.n[n_i].value, lrg.relation, lrg.location)) {
//				if(countRel.containsKey(lrg.relation)){
//					countRel.put(lrg.relation, countRel.get(lrg.relation)+1);
//				}else{
//					countRel.put(lrg.relation, 1);
//				}
				p.n_states[n_i] = true;
			} else {
				p.n_states[n_i] = false;
			}
		}
		return p;
	}
	
	public static void printMatchStat(){
		for(String key: countRel.keySet()){
			System.out.println(key+":"+countRel.get(key));
		}
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
		double bu = GoldDB.MARGIN;
		GoldDB.MARGIN = margin;
		boolean res = closeEnough(value, rel, entity);
		GoldDB.MARGIN = bu;
		return res;
	}

	
	public static boolean closeEnough(Double value, String rel, String entity) {
		// TODO Auto-generated method stub
		rel = rel.split("&")[0];
		ArrayList<Double> goldValues = GoldDB.getGoldDBValue(entity, rel, GoldDB.K);
		/*if(rel.equals("ELEC")){
			System.err.println("Entity: "+entity);
			System.err.println("DBVal: "+goldValues);
			System.err.println("Tvalue: "+value);
		}*/
		for(Double val : goldValues){
			
			Double valueSlack = GoldDB.MARGIN * val; // +- 5 percent
			//System.out.print(val + "\t");
			if((value > (val- valueSlack)) && (value < (val + valueSlack))){	
				return true;
			}
		}
	
		return false;
		//return true;
	}

}
