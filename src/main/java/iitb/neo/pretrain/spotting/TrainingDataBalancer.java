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

public class TrainingDataBalancer {

	public static void main(String[] args) throws IOException {

		String instanceFile = "/mnt/bag/aman/train/instances_deldanger.tsv";
		String featureFile = "/mnt/bag/aman/train/features_mintz_handkeywords_numbers_fresh.tsv";
		String outputInstanceFile = "/mnt/bag/aman/train/instances_multir.tsv";
		String outputFeatureFile = "/mnt/bag/aman/train/instances_deldanger.tsv";
		boolean generateStats = false;
		HashMap<String, String> instance_featureMap = new HashMap<String, String>();
		int topk = 3;
		double margin = 0.2;
		GoldDB.initializeGoldDB(
				"/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/kb-worldbank-SI.tsv",
				topk, margin);
		PrintWriter pw = new PrintWriter(new FileWriter(outputInstanceFile));
		PrintWriter opw = new PrintWriter(new FileWriter(outputFeatureFile));

		BufferedReader fbr = new BufferedReader(new FileReader(featureFile));
		BufferedReader br = new BufferedReader(new FileReader(instanceFile));
		String instanceLine = null;
		String featureLine = null;
		HashMap<String, ArrayList<String>> hits = new HashMap<>();
		HashMap<String, ArrayList<String>> flops = new HashMap<>();

		int lineNum = 0;
		while ((instanceLine = br.readLine()) != null
				&& (featureLine = fbr.readLine()) != null) {
			System.out.println("Processing line = " + lineNum++);
			instance_featureMap.put(instanceLine, featureLine);
			String instanceLineSplit[] = instanceLine.split("\t");
			Double value = Double.parseDouble(instanceLineSplit[4]);
			String relString = instanceLineSplit[9];
			// int sentid = Integer.parseInt(instanceLineSplit[8]);
			String countryId = instanceLineSplit[0];
			double closeEnough = GoldDbInference.closeEnough(value, relString,
					countryId, 0.5);
			if (generateStats) {
				if (closeEnough) {
					if (null == hits.get(relString)) {
						ArrayList<String> sentList = new ArrayList<String>();
						sentList.add(instanceLine);
						hits.put(relString, sentList);
					} else {
						hits.get(relString).add(instanceLine);
					}
				} else {
					if (null == flops.get(relString)) {
						ArrayList<String> sentList = new ArrayList<String>();
						sentList.add(instanceLine);
						flops.put(relString, sentList);
					} else {
						flops.get(relString).add(instanceLine);
					}
				}
			} else { 
				
			}

		}

		if(generateStats)
		for (String rel : hits.keySet()) {
			System.err.println("Processing: " + rel);
			System.out.println("True Positives: " + hits.get(rel).size());
			ArrayList<String> outputSentences = new ArrayList<String>(
					hits.get(rel)); // add all the correct sentences.

			if (flops.containsKey(rel)) {
				System.out.println("False positives: " + flops.get(rel).size());
				ArrayList<String> flopSentences = flops.get(rel);

				double trueProb = (outputSentences.size() * 1.0)
						/ (outputSentences.size() + flopSentences.size());
				System.out.println("prob: " + trueProb);
				for (String sent : flopSentences) {
					double rand = Math.random();
					if (rand <= trueProb) {
						outputSentences.add(sent);
					}
				}
			} else {
				System.err.println("No Negative sentence for : " + rel);
			}

			String[] sentences = (String[]) outputSentences
					.toArray(new String[outputSentences.size()]);
			shuffle(sentences);
			for (String sentence : sentences) {
				pw.write(sentence + "\n");
				opw.write(instance_featureMap.get(sentence) + "\n");
			}
			System.err.println("Total sentences in balanced: "
					+ sentences.length);
		}

		opw.close();
		pw.close();
		br.close();
		fbr.close();
	}

	public static void shuffle(String[] sentences) {
		Random random = new Random(1);
		for (int i = 0; i < sentences.length; i++) {
			// pick element that we want to swap with
			int e = i + random.nextInt(sentences.length - i);
			String tmp = sentences[e];
			sentences[e] = sentences[i];
			sentences[i] = tmp;
		}
	}
}
