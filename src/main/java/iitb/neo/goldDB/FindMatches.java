package main.java.iitb.neo.goldDB;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import main.java.iitb.neo.training.algorithm.lpercp.GoldDbInference;

public class FindMatches {

	public static void main(String[] args) throws IOException {
		String instanceFile = "data/instances_flat.tsv";
		BufferedReader br = new BufferedReader(new FileReader(instanceFile));
		String instanceLine = null;
		HashMap<String, ArrayList<Integer> > hits = new HashMap<>();
		int topk = 3;
		double margin = 0.2;
		GoldDB.initializeGoldDB("/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/kb-worldbank-SI.tsv", topk, margin);
		
		while((instanceLine = br.readLine()) != null) {
			String instanceLineSplit[] = instanceLine.split("\t");
			Double value = Double.parseDouble(instanceLineSplit[4]);
			String relString = instanceLineSplit[9];
			int sentid = Integer.parseInt(instanceLineSplit[8]);
			String countryId = instanceLineSplit[0];
			if(GoldDbInference.closeEnough(value, relString, countryId, 0.5)) {
				if(null == hits.get(relString)) {
					ArrayList<Integer> sentList = new ArrayList<Integer>();
					sentList.add(sentid);
					hits.put(relString, sentList);
				} else {
					hits.get(relString).add(sentid);
				}
			}
		}
		
		for(String rel: hits.keySet()) {
			System.err.println("Proccesing: "+rel);
			PrintWriter pw = new PrintWriter(new FileWriter("data/sentence_"+rel+".txt"));
			pw.write("(");
			int i = 0;
			ArrayList<Integer> h = hits.get(rel);
			for(Integer sentid: h) {
				if(i == (h.size()-1)){
					pw.write(sentid+" ");
				}else{
					pw.write(sentid + ", ");
				}
				i++;
			}
			pw.write(")\n");
			pw.close();
		}
		br.close();
	}

}
