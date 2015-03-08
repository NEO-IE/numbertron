package main.java.iitb.neo.goldDB;
import iitb.rbased.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import iitb.rbased.meta.RelationMetadata;

public class GoldDB {
	/*
	 * @Todo: Read the gold database path from the json file.
	 */
	
	private static String goldDBloc = "data/kb-facts-train.tsv";
	private static HashMap<Pair<String,String>,ArrayList<Double>> goldDB;
	
	
	static{
		goldDB = new HashMap<Pair<String,String>,ArrayList<Double>>();
		
		File goldDBFile = new File(goldDBloc);
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(goldDBFile));
			String line = br.readLine();
	        while (line != null) {
	        	String parts[] = line.split("\\t");
	        	if(parts.length == 3){
	        		String rel = RelationMetadata.getShortenedRelation(parts[2]);
	        		Double value = Double.parseDouble(parts[1]);
	        		Pair<String,String> entityRel = new Pair<String,String>(parts[0],rel);
	        		if(goldDB.containsKey(entityRel)){
	        			goldDB.get(entityRel).add(0, value); //insert at front, so that latest values are in front.
	        		}else{
	        			ArrayList<Double> valueList = new ArrayList<Double>();
	        			valueList.add(value);
	        			goldDB.put(entityRel, valueList);
	        		}
	        	}
	            line = br.readLine();
	        }
	        br.close();
		}catch(Exception e){
			e.printStackTrace();
		}	
	}
	
	/*
	 * function returns list of gold values for entity, relation pair
	 */
	public static ArrayList<Double> getGoldDBValue(String entity, String rel){
		Pair<String, String> entityRel = new Pair<String, String>(entity, rel);
		if(goldDB.containsKey(entityRel)){
			return goldDB.get(entityRel);
		}
		return null;
	}
	
	/*
	 * function returns top K of gold values for entity, relation pair
	 */
	public static ArrayList<Double> getGoldDBValue(String entity, String rel, int k){
		Pair<String, String> entityRel = new Pair<String, String>(entity, rel);
		if(goldDB.containsKey(entityRel)){
			return new ArrayList<Double>(goldDB.get(entityRel).subList(0, k));
		}
		return new ArrayList<Double>();
	}
	
	/*
	 * function returns top gold value for entity, relation pair
	 */
	public static Double getTopGoldDBValue(String entity, String rel){
		Pair<String, String> entityRel = new Pair<String, String>(entity, rel);
		if(goldDB.containsKey(entityRel)){
			return goldDB.get(entityRel).get(0);
		}
		return null;
	}
	
	public static void main(String args[]){
		System.out.println(GoldDB.getGoldDBValue("/m/01z88t","FDI"));
	}
}
