package main.java.iitb.neo.autoeval;

import iitb.rbased.meta.RelationMetadata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.Extraction;


class Results {
	public double precision;
	public double recall;
	//The following 2 maps keep a track of the extractions
	HashMap<String, Integer> perRelationCorrectExtracted;
	HashMap<String, Integer> perRelationTotalExtracted;
	
	HashSet<Extraction> trueExtractions; //read from the truth
	HashMap<String, Integer> perRelationTrue;  //keeps a count of per relation truth to generate per relation stat
	
	int totalExtracted;
	int totalFacts;
	int totalCorrectExtracted;
	
	Results(String truthFileName) throws IOException {
		perRelationCorrectExtracted = new HashMap<String, Integer>();
		perRelationTotalExtracted = new HashMap<String, Integer>();
		readTrueExtractions(truthFileName); //fill the true extractions
	}
	public void dumpResults() {
		PrintWriter pw = new PrintWriter(System.out, true);
		dumpResults(pw);
		pw.close();
	}
	public void dumpResults(PrintWriter pw) {
		pw.write("Total Extracted: " + totalExtracted + "\n");
		pw.write("Total Correct: " + totalCorrectExtracted + "\n");
		pw.write("Total Facts in the Corpus: " + totalFacts + "\n");
		precision = (totalCorrectExtracted * 1.0) / totalExtracted;
		recall = (totalCorrectExtracted * 1.0) / totalFacts;
		pw.write("Precision = " + precision + ", Recall = " + recall + ", F-Score = "
				+ f(precision, recall) + "\n");
		pw.write("----Per Relation P/R\n");
		for (String relName : RelationMetadata.getRelations()) {
			Integer totalCorr = perRelationCorrectExtracted.get(relName);
			Integer totalExtr = perRelationTotalExtracted.get(relName);
			Integer totalTrue = perRelationTrue.get(relName);
			totalCorr = (totalCorr == null) ? 0 : (totalCorr);
			if (null == totalExtr) {
				precision = -1.0;
			} else {
				precision = (totalCorr * 1.0) / totalExtr;
			}
			if(totalTrue == null){
				continue;
			}
			recall = (totalCorr * 1.0) / (totalTrue);
			pw.write("Relation = " + relName + ", Precision = " + precision + ", Recall = " + recall
					+ ", F-Score = " + f(precision, recall) + " (" + totalCorr + ", " + totalExtr + ", "
					+ totalTrue + ")\n");

		}
	}

	public double f(double p, double r) {
		assert (p + r > 0);
		if (p + r == 0) {
			return 0;
		}
		return (2 * p * r) / (p + r);
	}
	public void fillResult(List<Extraction> modelExtractions) {

		assert (modelExtractions.size() > 0);
		int correct = 0;
		for (Extraction e : modelExtractions) {
			String relName = e.getRelation();
			Integer currCount = perRelationTotalExtracted.get(relName);
			perRelationTotalExtracted.put(relName, null == currCount ? 1 : currCount + 1);
			if (isTrueExtr(e)) {

				currCount = perRelationCorrectExtracted.get(relName);
				perRelationCorrectExtracted.put(relName, null == currCount ? 1 : currCount + 1);
				correct++;
			}
		}
		totalFacts = trueExtractions.size();
		totalCorrectExtracted = correct;
		totalExtracted = modelExtractions.size();
	}
	boolean isTrueExtr(Extraction e) {
		for (Extraction t : trueExtractions) {
			if (t.equals(e)) {
				return true;
			}
		}
		return false;
	}

	private void readTrueExtractions(String trueFile) throws IOException {
		assert (trueExtractions == null);
		trueExtractions = new HashSet<Extraction>();
		perRelationTrue = new HashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(trueFile));
		String trueLine = null;
		while (null != (trueLine = br.readLine())) {
			String lineSplit[] = trueLine.split("\t");
			String arg1Name = lineSplit[0];
			int arg1StartOff = Integer.parseInt(lineSplit[1]);
			int arg1EndOff = Integer.parseInt(lineSplit[2]);

			Argument arg1 = new Argument(arg1Name, arg1StartOff, arg1EndOff);

			String arg2Name = lineSplit[3];
			int arg2StartOff = Integer.parseInt(lineSplit[4]);
			int arg2EndOff = Integer.parseInt(lineSplit[5]);

			Integer sendId = Integer.parseInt(lineSplit[6]);

			String docName = lineSplit[7];

			Argument arg2 = new Argument(arg2Name, arg2StartOff, arg2EndOff);

			String relName = lineSplit[8];
			String senText = lineSplit[10];
			Integer currCount = perRelationTrue.get(relName);
			perRelationTrue.put(relName, null == currCount ? 1 : currCount + 1);

			trueExtractions.add(new Extraction(arg1, arg2, docName, relName, sendId, senText));
		}
		br.close();
	}

}