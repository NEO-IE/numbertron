package main.java.iitb.neo.goldDB;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import main.java.iitb.neo.training.algorithm.lpercp.GoldDbInferenceEntityGraph;
import main.java.iitb.neo.training.ds.Number;

/**
 * Iterates over the instances and removes those that are false according to the 
 * database.
 * @author aman
 *
 */
public class PruneFalseInstances {

	GoldDB gdb;
	public PruneFalseInstances() {
		gdb = new GoldDB();
	}
	public void run(String instancesFile, String prunedInstancesFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(instancesFile));
		BufferedWriter bw = new BufferedWriter(new FileWriter(prunedInstancesFile));
		String instanceLine = null;
		int linesProcessed = 0;
		int bugs = 0;
		while((instanceLine = br.readLine()) != null) {
			String instanceLineSplit[] = instanceLine.split("\t");
			linesProcessed++;
			String location = instanceLineSplit[0];
			Double number = Number.getDoubleValue(instanceLineSplit[4]);
			if(number == null) {
				System.out.println("No legal conversion for " + instanceLineSplit[4]);
				bugs++;
				continue;
			}
			String relation  = instanceLineSplit[9];
			if(GoldDbInferenceEntityGraph.closeEnough(number, relation, location)) {
				bw.write(instanceLine + "\n");
			}
			
			if(linesProcessed % 100000 == 0) {
				System.out.println("Lines Processed: " + linesProcessed);
			}
		}
		bw.close();
		br.close();
		System.out.println("Total bugs: " + bugs);
	}
	
	public static void main(String args[]) throws IOException{
		PruneFalseInstances pruner = new PruneFalseInstances();
		String inFile = "data/full_20perc_keywordfeatures_instances.tsv";
		String outFile = "data/full_20perc_keywordfeatures_pruned_instances.tsv";
		pruner.run(inFile, outFile);
	}
}
