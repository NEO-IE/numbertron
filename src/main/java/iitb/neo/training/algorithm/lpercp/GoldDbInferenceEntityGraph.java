package main.java.iitb.neo.training.algorithm.lpercp;

import iitb.rbased.util.Pair;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import main.java.iitb.neo.goldDB.GoldDB;
import main.java.iitb.neo.pretrain.process.MakeEntityGraph;
import main.java.iitb.neo.training.ds.EntityGraph;
import meta.RelationMetaData;
import edu.washington.multirframework.multiralgorithm.Mappings;

/**
 * Sets the value of n nodes based on the value pulled from the gold db
 * 
 * @author aman
 * 
 */
public class GoldDbInferenceEntityGraph {

	private static HashMap<Pair<String, String>, Integer> trueCountMap = new HashMap<>();

	private static HashMap<Pair<String, String>, Integer> falseCountMap = new HashMap<>();

	public static HashMap<String, Double> marginMap;
	static {
		marginMap = new HashMap<String, Double>();
		Double xs = 0.05;
		Double s = 0.1;
		Double regular = 0.2;
		Double xl = 0.3;
		Double xxl = 0.5;

		marginMap.put("AGL", xl);

		marginMap.put("FDI", xxl);
		marginMap.put("GOODS", xl);
		marginMap.put("GDP", xl);

		marginMap.put("ELEC", xl);

		marginMap.put("CO2", xxl);

		// unitMap.put("DIESEL", "united states dollar per litre");

		marginMap.put("INF", s);
		marginMap.put("INTERNET",xl);

		marginMap.put("LIFE", regular);

		marginMap.put("POP",  xl);

	}

	public static EntityGraphParse infer(EntityGraph egraph) {
		EntityGraphParse p = new EntityGraphParse();
		p.graph = egraph;
		p.z_states = new int[egraph.Z.length];
		p.n_states = new boolean[egraph.n.length][RelationMetaData.NUM_RELATIONS + 1];
		int numN = egraph.numNodesCount;
		Mappings m = MakeEntityGraph.mapping;
		
		for (int n_i = 0; n_i < numN; n_i++) { //for all the number nodes
			double value = egraph.n[n_i][1].value;
			for(String relation: RelationMetaData.relationNames) {
				int relNumber = m.getRelationID(relation, false);
				if (closeEnough(value, relation, egraph.entity)) {
					p.n_states[n_i][relNumber] = true;
				} else {
					p.n_states[n_i][relNumber] = false;
				}
			}
		}
		return p;
	}

	/**
	 * match with a specified cutoff
	 * 
	 * @param value
	 * @param rel
	 * @param entity
	 * @param margin
	 * @return
	 */
	public static boolean closeEnough(Double value, String rel, String entity,
			double margin) {
		double bu = GoldDB.MARGIN;
		GoldDB.MARGIN = margin;
		boolean res = closeEnough(value, rel, entity);
		GoldDB.MARGIN = bu;
		return res;
	}

	public static boolean closeEnough(Double value, String rel, String entity) {
		
		Pair<String, String> locationRelation = new Pair<String, String>(
				entity, rel);
		
		rel = rel.split("&")[0];
		ArrayList<Double> goldValues = GoldDB.getGoldDBValue(entity, rel,
				GoldDB.K);
		/*
		 * if(rel.equals("ELEC")){ System.err.println("Entity: "+entity);
		 * System.err.println("DBVal: "+goldValues);
		 * System.err.println("Tvalue: "+value); }
		 */
		for (Double val : goldValues) {

			// Double valueSlack = marginMap.get(rel)* val; // +- 5 percent
			Double valueSlack = GoldDB.MARGIN * val; // +- 5 percent
			// System.out.print(val + "\t");
			if ((value > (val - valueSlack)) && (value < (val + valueSlack))) {
				if (trueCountMap.get(locationRelation) == null) {
					trueCountMap.put(locationRelation, 1);
				} else {
					int currCount = trueCountMap.get(locationRelation);
					trueCountMap.put(locationRelation, currCount + 1);
				}
				return true;
			}
		}
		if (falseCountMap.get(locationRelation) == null) {
			falseCountMap.put(locationRelation, 1);
		} else {
			int currCount = falseCountMap.get(locationRelation);
			falseCountMap.put(locationRelation, currCount + 1);
		}
		return false;
		 
	}

	/**
	 * prints the stats on the number of Z nodes that are truth and false for
	 * every location relation graph
	 * 
	 */
	public static void printMatchStats(PrintWriter pw) {
		for (Pair<String, String> lrPair : trueCountMap.keySet()) {
			String location = lrPair.first;
			String relation = lrPair.second;
			Integer hits = trueCountMap.get(lrPair);
			Integer misses = falseCountMap.get(lrPair);
			
				if(misses == null) {
					misses  = 0;
				}
				double hitPerc = null == hits ? 0 : ((hits * 1.0) / (hits + misses));
				double missPerc = 1 - hitPerc;
				pw.println("Location = " + location + " Relation  = "
						+ relation + " hits = " + hitPerc + " misses = "
						+ missPerc + "\n");
			

		}
	}
}
