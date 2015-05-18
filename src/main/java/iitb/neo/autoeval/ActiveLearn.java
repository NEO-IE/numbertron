package main.java.iitb.neo.autoeval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ActiveLearn {
	
	double margin = 0.20; //the gold db match margin
	double activeLearnThreshold = 0.18; //only get labels for instances 
	int MAX_SAMPLE_PER_RELATION = 1;
	Map<String, Integer> countMap;
	public ActiveLearn() {
		countMap = new HashMap<String, Integer>();
		
		
	}
	/**
	 * 
	 * Appends a feature "hard: relation" to some of the instances
	 * @param instanceFile
	 * @param featureFile
	 * @param featureFileOut, the resulting output file
	 */
	private void activeLearn(String instanceFile, String featureFile, String featureFileOut) {
		try {
			BufferedReader instanceReader = new BufferedReader(new FileReader(
					instanceFile));
			BufferedReader featureReader = new BufferedReader(new FileReader(
					featureFile));
			BufferedWriter newFeatureWriter = new BufferedWriter(new FileWriter(featureFileOut));
			String instanceLine = null;
			String featureLine = null;
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			
			while ((instanceLine = instanceReader.readLine()) != null) {
				featureLine = featureReader.readLine();
				String instanceParts[] = instanceLine.split("\t");
				String sent = instanceParts[10];
				String rel = instanceParts[9];
				String entity = instanceParts[0];
				
				boolean ignore = LabelInstances.confusedPercentCountries.contains(entity)
						&& (rel.equals("INTERNET") || rel.equals("INF"));
				ignore = ignore
						|| (LabelInstances.confusedGOODSGDPCountries.contains(entity) && (rel
								.equals("GOODS") || rel.equals("GDP")));
				ignore = ignore
						|| (LabelInstances.confusedFDIGOODSCountries.contains(entity) && (rel
								.equals("GOODS") || rel.equals("FDI")));
				if (ignore) {
					newFeatureWriter.write(featureLine + "\n");
					continue;
				}
				
				Double value = Double.parseDouble(instanceParts[7]);
				boolean golddbLabel = LabelInstances.distanceLabel(value, rel, entity, margin);
				if(!golddbLabel) { //anyways false, write, and proceed
					newFeatureWriter.write(featureLine + "\n");
				} else { //this could be a possible false positive, get the label from the human oracle
					double matchMargin = LabelInstances.getMatchMargin(value, rel, entity, margin);
					if(matchMargin >= activeLearnThreshold) { //Almost false, take a label, if there are still relations left
						Integer relCount = countMap.get(rel);
						if(relCount == null) {
							countMap.put(rel, 1);
						} else if(relCount >= MAX_SAMPLE_PER_RELATION) {
							newFeatureWriter.write(featureLine + "\n");
							continue;
						} else {
							countMap.put(rel, relCount + 1);
						}
						System.err.println("-------CONFUSED INSTANCE--------");
						System.out.println(sent);
						System.out.println(entity + " - " + rel + " - " + value);
						System.out.println("\n");
						Integer input = Integer.parseInt(in.readLine());
						System.err.println("Label: " + (input == 1 ? "Yes" : "No"));
						String labelFeature = null;
						if(input == 1) {
							labelFeature = "hard: " + rel;
						} else {
							labelFeature = "hard: N_" + rel; 
						}
						newFeatureWriter.write(featureLine + "\t" + labelFeature  + "\n");
					} else { //not so confusing
						newFeatureWriter.write(featureLine + "\n");
					}
				}
			}
			instanceReader.close();
			featureReader.close();
			newFeatureWriter.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	
	}
	
	public static void main(String args[]) {
		String instanceFile = "/mnt/a99/d0/aman/combined_baby_instances.tsv";
		String featureFile = "/mnt/bag/aman/train/features_babycorpus.tsv";
		String newFeatureFile = "/mnt/bag/aman/train/features_babycorpus_active.tsv";
		ActiveLearn alearn = new ActiveLearn();
		alearn.activeLearn(instanceFile, featureFile, newFeatureFile);
	}
}
