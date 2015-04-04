package main.java.iitb.neo.autoeval;

import iitb.rbased.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import main.java.iitb.neo.NtronExperiment;

import org.apache.commons.io.IOUtils;

import com.cedarsoftware.util.io.JsonReader;

import edu.washington.multir.extractor.ExtractFromCorpus;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.Extraction;

/**
 * Evalaute a model specified by the json file, calculate the precision recall score
 * @author aman
 *
 */
public class EvaluateModel {
	String trueFile;
	String docName;
	ExtractFromCorpus efc;
	HashSet<Extraction> trueExtractions;
	public EvaluateModel(String propertiesFile) throws FileNotFoundException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		String jsonProperties = IOUtils.toString(new FileInputStream(new File(propertiesFile)));
		Map<String, Object> properties = JsonReader.jsonToMaps(jsonProperties);
		docName = NtronExperiment.getStringProperty(properties, "docName");
		trueFile = NtronExperiment.getStringProperty(properties, "trueFile");
		efc = new ExtractFromCorpus(propertiesFile);
		readTrueExtractions();
	}
	private void readTrueExtractions() throws IOException {
		assert(trueExtractions == null);
		trueExtractions = new HashSet<Extraction>();
		BufferedReader br = new BufferedReader(new FileReader(trueFile));
		String trueLine = null;
		while(null != (trueLine = br.readLine())) {
			String lineSplit[] = trueLine.split("\t");
			String arg1Name = lineSplit[3];
			int arg1StartOff = Integer.parseInt(lineSplit[1]);
			int arg1EndOff = Integer.parseInt(lineSplit[2]);
			
			Argument arg1 = new Argument(arg1Name, arg1StartOff, arg1EndOff);
		
			String arg2Name = lineSplit[7];
			int arg2StartOff = Integer.parseInt(lineSplit[5]);
			int arg2EndOff = Integer.parseInt(lineSplit[6]);
			Argument arg2 = new Argument(arg2Name, arg2StartOff, arg2EndOff);
			
			Integer sendId = Integer.parseInt(lineSplit[8]);
			
			String relName = lineSplit[9];
			
			String senText = lineSplit[10];
			trueExtractions.add(new Extraction(arg1, arg2, docName, relName, sendId, senText));
		}
	}
	
	private Pair<Double, Double> precisionRecall(List<Extraction> modelExtractions) {
		int total = modelExtractions.size();
		
		int correct = 0;
		for(Extraction e: modelExtractions) {
			if(trueExtractions.contains(e)) {
				correct++;
			}
		}
		Pair<Double, Double> pr = new Pair<Double, Double>((correct * 1.0) / modelExtractions.size(), (correct * 1.0) / trueExtractions.size());
		return pr;
	}
	
	
}
