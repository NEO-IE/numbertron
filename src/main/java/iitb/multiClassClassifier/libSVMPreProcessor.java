package main.java.iitb.multiClassClassifier;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import main.java.iitb.neo.goldDB.GoldDB;
import main.java.iitb.neo.training.algorithm.lpercp.GoldDbInference;
import main.java.iitb.neo.training.algorithm.lpercp.KeywordInference;
import main.java.iitb.neo.util.JsonUtils;

import org.apache.commons.lang.NotImplementedException;

public class libSVMPreProcessor {

	public String instanceFile;
	public String mappingFile;
	public String featureFile;
	
	public String goldDBFile;
	public Integer topKGoldDb;
	public Double margin;
	
	public String libSVMFile;
	public HashMap<String, Integer> relNameNumMapping;
	public HashMap<String, Integer> featNameNumMapping;
	
	public boolean includeNA = true;
	public boolean test = false;
	
	public libSVMPreProcessor(String propertiesFile) throws FileNotFoundException, IOException{
		Map<String, Object> properties = JsonUtils.getJsonMap(propertiesFile);
		
		instanceFile = JsonUtils.getStringProperty(properties, "instanceFile");
		featureFile = JsonUtils.getStringProperty(properties, "featureFile");
		mappingFile = JsonUtils.getStringProperty(properties, "mappingFile");
		libSVMFile = JsonUtils.getStringProperty(properties, "libSVMFile");
		
		goldDBFile = JsonUtils.getStringProperty(properties, "kbRelFile");
		topKGoldDb = JsonUtils.getIntegerProperty(properties, "topKGoldDb");
		margin = JsonUtils.getDoubleProperty(properties, "margin");
		
		System.err.println("Initializing Gold DB");
		GoldDB.initializeGoldDB(goldDBFile, topKGoldDb, margin);
		
		includeNA = JsonUtils.getBooleanProperty(properties, "includeNA");
		test = JsonUtils.getBooleanProperty(properties, "test");
		
		/*
		 * read mapping
		 */
		System.err.println("Reading Mapping file");
		relNameNumMapping = new HashMap<String, Integer>();
		featNameNumMapping = new HashMap<String, Integer>();
		BufferedReader featureReader = new BufferedReader(new FileReader(mappingFile));
		Integer numRel = Integer.parseInt(featureReader.readLine());
		for (int i = 0; i < numRel; i++) {
			// skip relation names
			String relLine = featureReader.readLine().trim();
			String relParts[] = relLine.split("\t");
			relNameNumMapping.put(relParts[1], i);
		}
		int numFeatures = Integer.parseInt(featureReader.readLine());
		String ftr = null;
		int fno = 0;
		while (fno < numFeatures) {
			ftr = featureReader.readLine().trim();
			String parts[] = ftr.split("\t");
			featNameNumMapping.put(parts[1], Integer.parseInt(parts[0]));
			fno++;
		}
		featureReader.close();

	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException{
		libSVMPreProcessor svmpp = new libSVMPreProcessor(args[0]);
		svmpp.run();
	}

	private void run() throws IOException {
		
		System.err.println("Reading instances and feature file");
		BufferedReader ir = new BufferedReader(new FileReader(instanceFile));
		BufferedReader fr = new BufferedReader(new FileReader(featureFile));
		
		PrintWriter pw = new PrintWriter(new FileWriter(libSVMFile));
		
		String instanceLine, featureLine;
		
		int count = 1;
		while( (instanceLine = ir.readLine()) != null){
			featureLine = fr.readLine();
			if(featureLine == null){
				ir.close();
				fr.close();
				pw.close();
				throw new NotImplementedException("Length of instance file and feature file do not match!");				
			}
			
			if(count % 100000 == 0){
				System.err.println(count + "  sentences processed");
			}
			count++;
			
			String instanceParts[] = instanceLine.split("\t");
			String entity = instanceParts[0];
			Double value = Double.parseDouble(instanceParts[4]);
			String relation = instanceParts[9];
			
			String featureParts[] = featureLine.split("\t");
			HashSet<String> features = new HashSet<String>();
			TreeSet<Integer> featIDs = new TreeSet<Integer>();
			for(int i = 4; i < featureParts.length; i++){
				features.add(featureParts[i]);
				Integer id = this.featNameNumMapping.get(featureParts[i]);
				if(id != null){
					featIDs.add(id);     //add the feature to a sorted list.
				}
			}
			
			if(test){
				writeToLibSVMFile(pw, "NA", featIDs);  //NA is dummy
			}else{
				Boolean goldInference = GoldDbInference.closeEnough(value, relation, entity);
				Boolean keywordInference = KeywordInference.hasKeyword_new(features, relation);
				if(goldInference && keywordInference){ //value is within limits.
					writeToLibSVMFile(pw, relation, featIDs);
				}else if(includeNA){
					writeToLibSVMFile(pw, "NA", featIDs);
				}else{
					//ignore the Negative examples.
				}
			}
			
		}
		
		if(fr.readLine() != null){
			ir.close();
			fr.close();
			pw.close();
			throw new NotImplementedException("Length of instance file and feature file do not match!");
		}
		
		pw.close();
		fr.close();
		ir.close();
		
	}

	private void writeToLibSVMFile(PrintWriter pw, TreeSet<Integer> featIDs) {
		// TODO Auto-generated method stub
		if(featIDs.size() == 0){
			return;
		}
		String outputString = ""; 
		for(Integer feat: featIDs){
			outputString += +feat+":1\t";
		}
		outputString.trim();
		outputString += "\n";
		
		pw.write(outputString);
	}

	private void writeToLibSVMFile(PrintWriter pw, String relation, TreeSet<Integer> featIDs) {
		// TODO Auto-generated method stub
		Integer relationID = relNameNumMapping.get(relation);
		if(includeNA && relationID == null){
			relationID = 10; 
		}
		String outputString = relationID.toString();
		for(Integer feat: featIDs){
			outputString += "\t"+feat+":1";
		}
		outputString += "\n";
		
		pw.write(outputString);
	}
}
