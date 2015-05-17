package main.java.iitb.neo.autoeval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ActiveLearn {
	
	double margin = 0.20; //the gold db match margin
	double activeLearnThreshold = 0.18; //only get labels for instances 
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
			Console console = System.console();
			
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
					newFeatureWriter.write(featureLine);
					continue;
				}
				
				Double value = Double.parseDouble(instanceParts[7]);
				boolean golddbLabel = LabelInstances.distanceLabel(value, rel, entity, margin);
				if(!golddbLabel) { //anyways false, write, and proceed
					newFeatureWriter.write(featureLine);
				} else { //this could be a possible false positive, get the label from the human oracle
					double matchMargin = LabelInstances.getMatchMargin(value, rel, entity, margin);
					if(matchMargin > activeLearnThreshold) { //Almost false, take a label
						System.err.println("------ATTENTION ORACLE--------");
						System.out.println(sent);
						System.out.println(entity + " - " + rel + " - " + value);
						System.out.println("\n");
						String input = console.readLine("[1 (yes) / 2 (no)]: ");
						String labelFeature = null;
						if(input.equals("1")) {
							labelFeature = "hard: " + rel;
							
						} else {
							labelFeature = "hard: N_" + rel; 
						}
						newFeatureWriter.write(featureLine + "\t" + labelFeature);
					} else { //not so confusing
						newFeatureWriter.write(featureLine);
					}
				}
			}
		} catch (IOException ioe) {

		}
	}
}
