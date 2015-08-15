package main.java.iitb.neo.pretrain.spotting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import edu.washington.multirframework.multiralgorithm.MILDocument;
import main.java.iitb.neo.goldDB.GoldDB;
import main.java.iitb.neo.training.algorithm.lpercp.GoldDbInference;

public class TrueInstancePruner {

	public static void main(String[] args) throws IOException {

		String instanceFile = "/mnt/bag/aman/train/instances_deldanger.tsv";
		String featureFile = "/mnt/bag/aman/train/features_mintz_handkeywords_numbers_fresh.tsv";
		String outputInstanceFile = "/mnt/bag/aman/train/instances_multir.tsv";
		String outputFeatureFile = "/mnt/bag/aman/train/features_multir.tsv";
		
		int topk = 3;
		double margin = 0.2;
		GoldDB.initializeGoldDB(
				"/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/kb-worldbank-SI.tsv",
				topk, margin);
		PrintWriter instanceWriter = new PrintWriter(new FileWriter(outputInstanceFile));
		PrintWriter featureWriter = new PrintWriter(new FileWriter(outputFeatureFile));

		BufferedReader fbr = new BufferedReader(new FileReader(featureFile));
		BufferedReader br = new BufferedReader(new FileReader(instanceFile));
		String instanceLine = null;
		String featureLine = null;

		int lineNum = 0;
		while ((instanceLine = br.readLine()) != null
				&& (featureLine = fbr.readLine()) != null) {
			System.out.println("Processing line = " + lineNum++);
			
			String instanceLineSplit[] = instanceLine.split("\t");
			Double value = Double.parseDouble(instanceLineSplit[4]);
			String relString = instanceLineSplit[9];
			// int sentid = Integer.parseInt(instanceLineSplit[8]);
			String countryId = instanceLineSplit[0];
			boolean closeEnough = GoldDbInference.closeEnough(value, relString,
					countryId, margin);
			if(closeEnough) {
				instanceWriter.print(instanceLine + "\n");
				featureWriter.print(featureLine + "\n");
			}
		}
		instanceWriter.close();
		featureWriter.close();
		br.close();
		fbr.close();
	}

}
