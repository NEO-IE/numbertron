package main.java.iitb.neo.autoeval;

import iitb.rbased.meta.RelationMetadata;
import iitb.rbased.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import main.java.iitb.neo.NtronExperiment;
import main.java.iitb.neo.extract.ExtractFromCorpus;

import org.apache.commons.io.IOUtils;

import com.cedarsoftware.util.io.JsonReader;

import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.Extraction;

/**
 * Evalaute a model specified by the json file, calculate the precision recall score
 * @author aman
 *
 */
public class EvaluateModel {
	ExtractFromCorpus efc;
	HashSet<Extraction> trueExtractions;
	EvaluateModel.Results  res;
	private class Results
	{
		HashMap<String, Integer> perRelationCorrect;
		HashMap<String, Integer> perRelationTrue;
		HashMap<String, Integer> perRelationExtracted;
		int totalExtracted;
		int totalFacts;
		int totalCorrectExtracted;
		
		public void dumpResults() {
			Double precision = (totalCorrectExtracted * 1.0) / totalExtracted;
			Double recall = (totalCorrectExtracted * 1.0) / totalFacts;
			System.out.println("Precision = " + precision + ", Recall = " + recall + ", F-Score = " + f(precision, recall));
			System.out.println("----Per Relation P/R");
			for(String relName: RelationMetadata.getRelations()) {
				Integer totalCorr = perRelationCorrect.get(relName);
				Integer totalExtr = perRelationExtracted.get(relName);
				Integer totalTrue = perRelationTrue.get(relName);
				totalCorr = (totalCorr == null) ? 0 : (totalCorr);
				if(null == totalExtr) {
					precision = -1.0;
				} else {
					precision = (totalCorr * 1.0) / totalExtr;
				}
				recall = (totalCorr * 1.0) / (null == totalTrue ? 1 : totalTrue);
				System.out.println("Relation = " + relName + ", Precision = " + precision + ", Recall = " + recall + ", F-Score = " + f(precision, recall));
					
				
			}
		}
		
		double f(double p, double r) {
			assert(p + r > 0);
			if(p + r == 0) {
				return 0;
			}
			return (2 * p * r) / (p + r);
		}
	}
	
	public EvaluateModel(String propertiesFile) throws Exception {
		res = new Results();
		res.perRelationCorrect = new HashMap<String, Integer>();
		res.perRelationExtracted = new HashMap<String, Integer>();
		res.perRelationTrue = new HashMap<String, Integer>();
			
		String jsonProperties = IOUtils.toString(new FileInputStream(new File(propertiesFile)));
		Map<String, Object> properties = JsonReader.jsonToMaps(jsonProperties);
		String trueFile = NtronExperiment.getStringProperty(properties, "trueFile");
		efc = new ExtractFromCorpus(propertiesFile);
		
		readTrueExtractions(trueFile);
	}
	private void readTrueExtractions(String trueFile) throws IOException {
		assert(trueExtractions == null);
		trueExtractions = new HashSet<Extraction>();
		BufferedReader br = new BufferedReader(new FileReader(trueFile));
		String trueLine = null;
		while(null != (trueLine = br.readLine())) {
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
			
			Integer currCount = res.perRelationTrue.get(relName);
			res.perRelationTrue.put(relName, null == currCount ? 1 : currCount + 1);
			
			trueExtractions.add(new Extraction(arg1, arg2, docName, relName, sendId, senText));
		}
	}
	
	private void fillResult(List<Extraction> modelExtractions) {

		assert(modelExtractions.size() > 0);
		int correct = 0;
		for(Extraction e: modelExtractions) {
			String relName = e.getRelation();
			Integer currCount = res.perRelationExtracted.get(relName);
			res.perRelationExtracted.put(relName, null == currCount ? 1 : currCount + 1);
			if(isTrueExtr(e)) {
				
				currCount = res.perRelationCorrect.get(relName);
				res.perRelationCorrect.put(relName, null == currCount ? 1 : currCount + 1);
				correct++;
			}
		}
		res.totalFacts = trueExtractions.size();
		res.totalCorrectExtracted = correct;
		res.totalExtracted = modelExtractions.size();
	}
	
	boolean isTrueExtr(Extraction e) {
		for(Extraction t: trueExtractions) {
			if(t.equals(e)) {
				return true;
			}
		}
		return false;
	}
	
	public void evaluate() throws SQLException, IOException {
		List<Extraction> modelExtractions = efc.getExtractions("extrs2", true);
		fillResult(modelExtractions);
		res.dumpResults();
	}
	public static void main(String args[]) throws Exception {
		EvaluateModel emodel = new EvaluateModel(args[0]);
		emodel.evaluate();
	}
	
	
}
