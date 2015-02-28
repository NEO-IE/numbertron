//sg
/**
 * An attempt at integrating rule based system with multir
 */
package main.java.iitb.neo.pretrain.spotting;

import iitb.rbased.main.RuleBasedDriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.RelationMatching;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.corpus.DocumentInformationI;
import edu.washington.multirframework.corpus.SentInformationI;
import edu.washington.multirframework.corpus.TokenInformationI;
import edu.washington.multirframework.distantsupervision.NegativeExampleCollection;
import edu.washington.multirframework.featuregeneration.FeatureGeneration;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;

public class NtronExperiment {
	private String corpusPath;
	private String typeRelMapPath;
	private ArgumentIdentification ai;
	private FeatureGenerator fg;
	private List<SententialInstanceGeneration> sigs;
	private List<String> DSFiles;
	private List<String> oldFeatureFiles;
	private List<String> featureFiles;
	private List<String> multirDirs;
	private List<String> oldMultirDirs;
	private RelationMatching rm;
	private NegativeExampleCollection nec;
	private KnowledgeBase kb;
	private String testDocumentsFile;
	private CorpusInformationSpecification cis;
	private String evalOutputName;
	private boolean train = false;
	private boolean useFiger = false;

	private Integer featureThreshold = 2;
	private boolean strictNegativeGeneration = false;
	private RuleBasedDriver rbased;
	private Map<String, String> countryFreebaseIdMap;

	public NtronExperiment() {
	}

	public NtronExperiment(String propertiesFile) throws Exception {

		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		String jsonProperties = IOUtils.toString(new FileInputStream(new File(
				propertiesFile)));
		Map<String, Object> properties = JsonReader.jsonToMaps(jsonProperties);

		rbased = new RuleBasedDriver(true);
		corpusPath = getStringProperty(properties, "corpusPath");
		evalOutputName = getStringProperty(properties, "evalOutputName");
		testDocumentsFile = getStringProperty(properties, "testDocumentsFile");
		String train = getStringProperty(properties, "train");

		/**
		 * Create the entity name to id map
		 */
		String countriesFile = "/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/countries_list_ids";

		try {

			BufferedReader br = new BufferedReader(
					new FileReader(countriesFile));
			String countryRecord = null;
			countryFreebaseIdMap = new HashMap<String, String>();
			while ((countryRecord = br.readLine()) != null) {
				String vars[] = countryRecord.split("\t");
				String countryName = vars[1].toLowerCase();
				String countryId = vars[0];
				// System.out.println(countryName);
				countryFreebaseIdMap.put(countryName, countryId);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/**
		 * end creating the map
		 */

		double necRatio = 4.0;
		if (train != null) {
			if (train.equals("false")) {
				this.train = false;
			} else if (train.equals("true")) {
				this.train = true;
			}
		}

		String strictNegativeGenerationString = getStringProperty(properties,
				"strictNegativeGeneration");
		if (strictNegativeGenerationString != null) {
			if (strictNegativeGenerationString.equals("true")) {
				strictNegativeGeneration = true;
			}
		}

		String featThresholdString = getStringProperty(properties,
				"featureThreshold");
		if (featThresholdString != null) {
			this.featureThreshold = Integer.parseInt(featThresholdString);
		}

		String useFiger = getStringProperty(properties, "useFiger");
		if (useFiger != null) {
			if (useFiger.equals("true")) {
				this.useFiger = true;
			}
		}
		String featureGeneratorClass = getStringProperty(properties, "fg");
		if (featureGeneratorClass != null) {
			fg = (FeatureGenerator) ClassLoader.getSystemClassLoader()
					.loadClass(featureGeneratorClass).newInstance();
		}

		String aiClass = getStringProperty(properties, "ai");
		if (aiClass != null) {
			ai = (ArgumentIdentification) ClassLoader.getSystemClassLoader()
					.loadClass(aiClass).getMethod("getInstance").invoke(null);
		}

		String rmClass = getStringProperty(properties, "rm");
		if (rmClass != null) {
			rm = (RelationMatching) ClassLoader.getSystemClassLoader()
					.loadClass(rmClass).getMethod("getInstance").invoke(null);
		}

		String necRatioString = getStringProperty(properties, "necRatio");
		if (necRatioString != null) {
			necRatio = Double.parseDouble(necRatioString);
		}

		String necClass = getStringProperty(properties, "nec");
		if (necClass != null) {
			nec = (NegativeExampleCollection) ClassLoader
					.getSystemClassLoader().loadClass(necClass)
					.getMethod("getInstance", double.class)
					.invoke(null, necRatio);
		}

		String kbRelFile = getStringProperty(properties, "kbRelFile");
		String kbEntityFile = getStringProperty(properties, "kbEntityFile");
		String targetRelFile = getStringProperty(properties, "targetRelFile");
		if (kbRelFile != null && kbEntityFile != null && targetRelFile != null) {
			kb = new KnowledgeBase(kbRelFile, kbEntityFile, targetRelFile);
		}

		List<String> sigClasses = getListProperty(properties, "sigs");
		sigs = new ArrayList<>();
		for (String sigClass : sigClasses) {
			sigs.add((SententialInstanceGeneration) ClassLoader
					.getSystemClassLoader().loadClass(sigClass)
					.getMethod("getInstance").invoke(null));
		}

		List<String> dsFileNames = getListProperty(properties, "dsFiles");
		DSFiles = new ArrayList<>();
		for (String dsFileName : dsFileNames) {
			DSFiles.add(dsFileName);
		}

		List<String> oldFeatureFileNames = getListProperty(properties,
				"oldFeatureFiles");
		oldFeatureFiles = new ArrayList<>();
		for (String oldFeatureFileName : oldFeatureFileNames) {
			oldFeatureFiles.add(oldFeatureFileName);
		}

		List<String> featureFileNames = getListProperty(properties,
				"featureFiles");
		featureFiles = new ArrayList<>();
		for (String featureFileName : featureFileNames) {
			featureFiles.add(featureFileName);
		}

		List<String> oldMultirDirNames = getListProperty(properties,
				"oldModels");
		oldMultirDirs = new ArrayList<>();
		for (String oldMultirDirName : oldMultirDirNames) {
			oldMultirDirs.add(oldMultirDirName);
		}

		multirDirs = new ArrayList<>();
		List<String> multirDirNames = getListProperty(properties, "models");
		for (String multirDirName : multirDirNames) {
			multirDirs.add(multirDirName);
		}

		cis = new CustomCorpusInformationSpecification();

		String altCisString = getStringProperty(properties, "cis");
		if (altCisString != null) {
			cis = (CustomCorpusInformationSpecification) ClassLoader
					.getSystemClassLoader().loadClass(altCisString)
					.newInstance();
		}

		// CorpusInformationSpecification
		List<String> tokenInformationClassNames = getListProperty(properties,
				"ti");
		List<TokenInformationI> tokenInfoList = new ArrayList<>();
		for (String tokenInformationClassName : tokenInformationClassNames) {
			tokenInfoList.add((TokenInformationI) ClassLoader
					.getSystemClassLoader()
					.loadClass(tokenInformationClassName).newInstance());
		}

		List<String> sentInformationClassNames = getListProperty(properties,
				"si");
		List<SentInformationI> sentInfoList = new ArrayList<>();
		for (String sentInformationClassName : sentInformationClassNames) {
			sentInfoList.add((SentInformationI) ClassLoader
					.getSystemClassLoader().loadClass(sentInformationClassName)
					.newInstance());
		}

		List<String> docInformationClassNames = getListProperty(properties,
				"di");
		List<DocumentInformationI> docInfoList = new ArrayList<>();
		for (String docInformationClassName : docInformationClassNames) {
			docInfoList.add((DocumentInformationI) ClassLoader
					.getSystemClassLoader().loadClass(docInformationClassName)
					.newInstance());
		}

		CustomCorpusInformationSpecification ccis = (CustomCorpusInformationSpecification) cis;
		ccis.addDocumentInformation(docInfoList);
		ccis.addTokenInformation(tokenInfoList);
		ccis.addSentenceInformation(sentInfoList);

		typeRelMapPath = getStringProperty(properties, "typeRelMap");

	}

	private List<String> getListProperty(Map<String, Object> properties,
			String string) {
		if (properties.containsKey(string)) {
			JsonObject obj = (JsonObject) properties.get(string);
			List<String> returnValues = new ArrayList<>();
			for (Object o : obj.getArray()) {
				returnValues.add(o.toString());
			}
			return returnValues;
		}
		return new ArrayList<>();
	}

	private String getStringProperty(Map<String, Object> properties, String str) {
		if (properties.containsKey(str)) {
			if (properties.get(str) == null) {
				return null;
			} else {
				return properties.get(str).toString();
			}
		}
		return null;
	}

	public void run() throws SQLException, IOException, InterruptedException, ExecutionException {
		Corpus c = new Corpus(corpusPath, cis, true);
		/*Step 1: create a file of all the possible spots*/
		boolean runDS = !filesExist(DSFiles);
		if(runDS) {
			Spotting spotting = new Spotting(corpusPath, cis, rbased);
			spotting.iterateAndSpot(DSFiles.get(0), c);
		}
		
		/*Step 2: Generate features*/
		boolean runFG = !filesExist(featureFiles);
		if(runFG){ 
			FeatureGeneration fGeneration = new FeatureGeneration(fg);
			fGeneration.run(DSFiles, featureFiles, c, cis);
		}
		
		/*Step 3: Training and weight learning*/
		
	}
	public static void main(String args[]) throws Exception {
		System.out.println("sg");
		NtronExperiment irb = new NtronExperiment(
				args[0]);
		irb.run();
	}

	private boolean filesExist(List<String> dsFiles) {
		for(String s : dsFiles){
			File f = new File(s);
			if(!f.exists()){
				System.err.println(s + " File does not exist!Need To Generate it");
				return false;
			}
		}
		return true;
	}

}
