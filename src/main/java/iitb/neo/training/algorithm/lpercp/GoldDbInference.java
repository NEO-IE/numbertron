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
		
		
		
		marginMap.put("AGL", xl);
		
		marginMap.put("FDI", xl);
		marginMap.put("GOODS", xl);
		marginMap.put("GDP", xl);
		
		marginMap.put("ELEC", xl);
		
		marginMap.put("CO2", xl);
		
		//unitMap.put("DIESEL", "united states dollar per litre");
		
		marginMap.put("INF", s);
		marginMap.put("INTERNET",s);
		
		marginMap.put("LIFE", regular);
		
		marginMap.put("POP", regular);
		
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
		
		if(rel.split("_").length > 0) { 
			return nullCloseEnough(value, rel, entity);
		}
		rel = rel.split("&")[0];
		ArrayList<Double> goldValues = GoldDB.getGoldDBValue(entity, rel, GoldDB.K);
		/*if(rel.equals("ELEC")){
			System.err.println("Entity: "+entity);
			System.err.println("DBVal: "+goldValues);
			System.err.println("Tvalue: "+value);
		}*/
		for(Double val : goldValues){
			
			Double valueSlack = marginMap.get(rel)* val; // +- 5 percent
			//System.out.print(val + "\t");
			if((value > (val- valueSlack)) && (value < (val + valueSlack))){	
				return true;
			}
		}
	
		return false;
		//return true;
	}

	/**
	 * This is a method that checks whether the null relation is true or not.
	 * The no attachment relation is true if all the relations for which it 
	 * is a true class are false.
	 * @param value
	 * @param rel
	 * @param entity
	 * @return
	 */
	private static boolean nullCloseEnough(Double value, String rel,
			String entity) {
		String rels[] = rel.split("_");
		for(int i = 1, l = rels.length; i < l; i++) {
			if(closeEnough(value, rels[i], entity)) {
				return false;
			}
				
		}
		System.out.println("here");
		//None of the relations had true for this, so call the no attachement true
		return true;
	}

}
