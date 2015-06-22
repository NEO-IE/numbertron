package main.java.iitb.neo.autoeval;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import main.java.iitb.neo.extract.NumbertronExtractFromCorpusEntityGraph;
import main.java.iitb.neo.util.JsonUtils;
import edu.washington.multirframework.data.Extraction;

/**
 * Evalaute a model specified by the json file, calculate the precision recall
 * score
 * 
 * @author aman
 * 
 */
public class EvaluateModel {

	NumbertronExtractFromCorpusEntityGraph efc; //returns the extractions given a model
	
	public Results res; //stores results, generates stats

	String modelName; // model to be evaluated
	boolean verbose; // verbose extractions?
	String verboseFile; // verboseFile
	private boolean writeExtractions;
	double cutoff_confidence;

	


	public EvaluateModel(String propertiesFile) throws Exception {
	
	

		Map<String, Object> properties = JsonUtils.getJsonMap(propertiesFile);
		String trueFile = JsonUtils.getStringProperty(properties, "trueFile");
		res = new Results(trueFile); //initialize a result object
		
		modelName = JsonUtils.getListProperty(properties, "models").get(0);

		verbose = JsonUtils.getBooleanProperty(properties, "verbose");
		verboseFile = JsonUtils.getStringProperty(properties, "verboseFile");
		efc = new NumbertronExtractFromCorpusEntityGraph(propertiesFile);
		
		cutoff_confidence = JsonUtils.getDoubleProperty(properties, "cutoff_confidence");

		
	}

	public void evaluate(NumbertronExtractFromCorpusEntityGraph efc, boolean verbose, PrintWriter resultWriter, double w_m, double w_k, double w_n) throws SQLException, IOException {
		List<Extraction> modelExtractions = efc.getExtractions("_results_", false, verbose, verboseFile, w_m, w_k, w_n);
		res.fillResult(modelExtractions);
		res.dumpResults(resultWriter);
		
	}
	
	public void evaluate(NumbertronExtractFromCorpusEntityGraph efc, boolean verbose, PrintWriter resultWriter) throws SQLException, IOException {
	
		List<Extraction> modelExtractions = efc.getExtractions("_results_", false, verbose, verboseFile, 1, 1, 1);
		res.fillResult(modelExtractions);
		res.dumpResults(resultWriter);
		
	}
	
	
	

	

	public void evaluate() throws SQLException, IOException {

		List<Extraction> modelExtractions = efc.getExtractions("_results_", false, verbose, verboseFile, 1, 1, 0);
		//List<Extraction> modelExtractions = efc.getExtractions();
		
		res.fillResult(modelExtractions);
		System.out.println("Confidence: " + cutoff_confidence);
		res.dumpResults();
	}

	public static void main(String args[]) throws Exception {
		EvaluateModel emodel = new EvaluateModel(args[0]);
		emodel.evaluate();
	}

}
