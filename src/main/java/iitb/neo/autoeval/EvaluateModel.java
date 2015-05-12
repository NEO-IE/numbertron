package main.java.iitb.neo.autoeval;

import iitb.rbased.meta.RelationMetadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import main.java.iitb.neo.extract.ExtractFromCorpus;
import main.java.iitb.neo.util.JsonUtils;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.Extraction;

/**
 * Evalaute a model specified by the json file, calculate the precision recall
 * score
 * 
 * @author aman
 * 
 */
public class EvaluateModel {
	ExtractFromCorpus efc;
	HashSet<Extraction> trueExtractions;
	EvaluateModel.Results res;
	HashMap<String, Integer> perRelationTrue;
	String modelName; // model to be evaluated
	boolean verbose; // verbose extractions?
	String verboseFile; // verboseFile
	private boolean writeExtractions;

	private class Results {
		HashMap<String, Integer> perRelationCorrect;
	
		HashMap<String, Integer> perRelationExtracted;
		int totalExtracted;
		int totalFacts;
		int totalCorrectExtracted;
		Results() {
			perRelationCorrect = new HashMap<String, Integer>();
			perRelationExtracted = new HashMap<String, Integer>();
			
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
			Double precision = (totalCorrectExtracted * 1.0) / totalExtracted;
			Double recall = (totalCorrectExtracted * 1.0) / totalFacts;
			pw.write("Precision = " + precision + ", Recall = " + recall + ", F-Score = "
					+ f(precision, recall) + "\n");
			pw.write("----Per Relation P/R\n");
			for (String relName : RelationMetadata.getRelations()) {
				Integer totalCorr = perRelationCorrect.get(relName);
				Integer totalExtr = perRelationExtracted.get(relName);
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

		double f(double p, double r) {
			assert (p + r > 0);
			if (p + r == 0) {
				return 0;
			}
			return (2 * p * r) / (p + r);
		}
		private void fillResult(List<Extraction> modelExtractions) {

			assert (modelExtractions.size() > 0);
			int correct = 0;
			for (Extraction e : modelExtractions) {
				String relName = e.getRelation();
				Integer currCount = perRelationExtracted.get(relName);
				perRelationExtracted.put(relName, null == currCount ? 1 : currCount + 1);
				if (isTrueExtr(e)) {

					currCount = perRelationCorrect.get(relName);
					perRelationCorrect.put(relName, null == currCount ? 1 : currCount + 1);
					correct++;
				}
			}
			totalFacts = trueExtractions.size();
			totalCorrectExtracted = correct;
			totalExtracted = modelExtractions.size();
		}
	}

	public EvaluateModel(String propertiesFile) throws Exception {
		res = new Results();
	

		Map<String, Object> properties = JsonUtils.getJsonMap(propertiesFile);
		String trueFile = JsonUtils.getStringProperty(properties, "trueFile");
		modelName = JsonUtils.getListProperty(properties, "models").get(0);

		verbose = JsonUtils.getBooleanProperty(properties, "verbose");
		verboseFile = JsonUtils.getStringProperty(properties, "verboseFile");
		efc = new ExtractFromCorpus(propertiesFile);

		readTrueExtractions(trueFile);
	}

	public void evaluate(ExtractFromCorpus efc, boolean verbose, PrintWriter resultWriter, double w_m, double w_k, double w_n) throws SQLException, IOException {
		Results r = new Results();
	
		List<Extraction> modelExtractions = efc.getExtractions("_results_", false, verbose, verboseFile, w_m, w_k, w_n);
		r.fillResult(modelExtractions);
		r.dumpResults(resultWriter);
		
	}
	
	public void evaluate(ExtractFromCorpus efc, boolean verbose, PrintWriter resultWriter) throws SQLException, IOException {
		Results r = new Results();
	
		List<Extraction> modelExtractions = efc.getExtractions("_results_", false, verbose, verboseFile, 1, 1, 1);
		r.fillResult(modelExtractions);
		r.dumpResults(resultWriter);
		
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

	

	boolean isTrueExtr(Extraction e) {
		for (Extraction t : trueExtractions) {
			if (t.equals(e)) {
				return true;
			}
		}
		return false;
	}

	public void evaluate() throws SQLException, IOException {
		List<Extraction> modelExtractions = efc.getExtractions("_results_" + new File(modelName).getName(), writeExtractions,
				verbose, verboseFile, 0, 1, 0);
		res.fillResult(modelExtractions);
		res.dumpResults();
	}

	public static void main(String args[]) throws Exception {
		EvaluateModel emodel = new EvaluateModel(args[0]);
		emodel.evaluate();
	}

}
