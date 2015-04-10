package main.java.iitb.neo.goldDB;
import iitb.rbased.meta.RelationMetadata;
import iitb.rbased.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GoldDB {
	
	/*
	 * @Todo: Read the gold database path from the json file.
	 */
	
	private static String goldDBFileLoc = null;
	private static HashMap<Pair<String,String>,ArrayList<Double>> goldDB;
	private static HashSet<String> countries;
	
	public static void initializeGoldDB(String goldDBName){
		goldDBFileLoc = goldDBName;
		goldDB = new HashMap<Pair<String,String>,ArrayList<Double>>();
		countries = new HashSet<String>();

		File goldDBFile = new File(goldDBFileLoc);
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(goldDBFile));
			String line = br.readLine();
	        while (line != null) {
	        	String parts[] = line.split("\\t");
	        	if(parts.length == 3){
	        		String rel = RelationMetadata.getShortenedRelation(parts[2]);
	        		countries.add(parts[0]);
	        		Double value = Double.parseDouble(parts[1]);
	        		Pair<String,String> entityRel = new Pair<String, String>(parts[0], rel);
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
			ArrayList<Double> resultList = new ArrayList<Double>(goldDB.get(entityRel));
			int max_val = (k < resultList.size()) ? k : resultList.size();
			return new ArrayList<Double>(resultList.subList(0, max_val));
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
	
	public static HashSet<String> getCountries() {
		return countries;
	}
	public static void main(String args[]){
		System.out.println(GoldDB.getGoldDBValue("/m/01z88t","FDI"));
	}
}
