package main.java.iitb.neo.autoeval;

import iitb.rbased.main.RuleBasedDriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import main.java.iitb.neo.NtronExperiment;
import main.java.iitb.neo.pretrain.spotting.Spotting;

import org.apache.commons.io.IOUtils;

import com.cedarsoftware.util.io.JsonReader;

import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;

/**
 * This class creates a gold test set to be fed to an automatic evaluator
 * @author aman
 *
 */
public class CreateGoldTruthFile {
	
	private String corpusPath;
	private CorpusInformationSpecification cis;
	private RuleBasedDriver rbased;
	private String outFile;
	public CreateGoldTruthFile(String propertiesFile) throws FileNotFoundException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		String jsonProperties = IOUtils.toString(new FileInputStream(new File(propertiesFile)));
		Map<String, Object> properties = JsonReader.jsonToMaps(jsonProperties);

		rbased = new RuleBasedDriver(true);
		corpusPath = NtronExperiment.getStringProperty(properties, "corpusPath");
		cis = new CustomCorpusInformationSpecification();

		String altCisString = NtronExperiment.getStringProperty(properties, "cis");
		if (altCisString != null) {
			cis = (CustomCorpusInformationSpecification) ClassLoader.getSystemClassLoader().loadClass(altCisString)
					.newInstance();
		}
		outFile = NtronExperiment.getStringProperty(properties, "resultFile");
		
	}
	public void run() throws SQLException, IOException {
		Spotting spotter = new Spotting(corpusPath, cis, rbased);
		Corpus c = new Corpus(corpusPath, cis, true);
		spotter.iterateAndSpot(outFile, c);
		
	}
	
	public static void main(String args[]) throws FileNotFoundException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException {
		CreateGoldTruthFile createTruth = new CreateGoldTruthFile(args[0]);
		createTruth.run();
	}
	
}
