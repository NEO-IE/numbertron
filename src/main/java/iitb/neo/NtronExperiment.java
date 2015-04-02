//sg
/**
 * An attempt at integrating rule based system with multir
 */
package main.java.iitb.neo;

import iitb.rbased.main.RuleBasedDriver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import main.java.iitb.neo.pretrain.featuregeneration.PerSpotFeatureGeneration;
import main.java.iitb.neo.pretrain.process.MakeGraph;
import main.java.iitb.neo.pretrain.spotting.Spotting;
import main.java.iitb.neo.training.algorithm.lpercp.LperceptTrain;
import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.meta.LRGraphMemoryDataset;

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
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;
import edu.washington.multirframework.multiralgorithm.Dataset;
import edu.washington.multirframework.multiralgorithm.DenseVector;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;

public class NtronExperiment {
	private String corpusPath;
	private FeatureGenerator fg;
	private List<SententialInstanceGeneration> sigs;
	private List<String> DSFiles;

	private List<String> featureFiles;
	private List<String> ntronModelDirs;
	private NegativeExampleCollection nec;
	private CorpusInformationSpecification cis;
	private RuleBasedDriver rbased;
	private Map<String, String> countryFreebaseIdMap;
	
	
	public NtronExperiment() {
	}

	public NtronExperiment(String propertiesFile) throws Exception {

		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		String jsonProperties = IOUtils.toString(new FileInputStream(new File(propertiesFile)));
		Map<String, Object> properties = JsonReader.jsonToMaps(jsonProperties);

		rbased = new RuleBasedDriver(true);
		corpusPath = getStringProperty(properties, "corpusPath");
		

		/**
		 * Create the entity name to id map
		 */
		String countriesFile = "/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/countries_list_ids";

		try {

			BufferedReader br = new BufferedReader(new FileReader(countriesFile));
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

			String featureGeneratorClass = getStringProperty(properties, "fg");
		if (featureGeneratorClass != null) {
			fg = (FeatureGenerator) ClassLoader.getSystemClassLoader().loadClass(featureGeneratorClass).newInstance();
		}

			
		List<String> sigClasses = getListProperty(properties, "sigs");
		sigs = new ArrayList<>();
		for (String sigClass : sigClasses) {
			sigs.add((SententialInstanceGeneration) ClassLoader.getSystemClassLoader().loadClass(sigClass)
					.getMethod("getInstance").invoke(null));
		}

		List<String> dsFileNames = getListProperty(properties, "dsFiles");
		DSFiles = new ArrayList<>();
		for (String dsFileName : dsFileNames) {
			DSFiles.add(dsFileName);
		}

		
		List<String> featureFileNames = getListProperty(properties, "featureFiles");
		featureFiles = new ArrayList<>();
		for (String featureFileName : featureFileNames) {
			featureFiles.add(featureFileName);
		}

		ntronModelDirs = new ArrayList<>();
		List<String> multirDirNames = getListProperty(properties, "models");
		for (String multirDirName : multirDirNames) {
			ntronModelDirs.add(multirDirName);
		}

		cis = new CustomCorpusInformationSpecification();

		String altCisString = getStringProperty(properties, "cis");
		if (altCisString != null) {
			cis = (CustomCorpusInformationSpecification) ClassLoader.getSystemClassLoader().loadClass(altCisString)
					.newInstance();
		}

		// CorpusInformationSpecification
		List<String> tokenInformationClassNames = getListProperty(properties, "ti");
		List<TokenInformationI> tokenInfoList = new ArrayList<>();
		for (String tokenInformationClassName : tokenInformationClassNames) {
			tokenInfoList.add((TokenInformationI) ClassLoader.getSystemClassLoader()
					.loadClass(tokenInformationClassName).newInstance());
		}

		List<String> sentInformationClassNames = getListProperty(properties, "si");
		List<SentInformationI> sentInfoList = new ArrayList<>();
		for (String sentInformationClassName : sentInformationClassNames) {
			sentInfoList.add((SentInformationI) ClassLoader.getSystemClassLoader().loadClass(sentInformationClassName)
					.newInstance());
		}

		List<String> docInformationClassNames = getListProperty(properties, "di");
		List<DocumentInformationI> docInfoList = new ArrayList<>();
		for (String docInformationClassName : docInformationClassNames) {
			docInfoList.add((DocumentInformationI) ClassLoader.getSystemClassLoader()
					.loadClass(docInformationClassName).newInstance());
		}

		CustomCorpusInformationSpecification ccis = (CustomCorpusInformationSpecification) cis;
		ccis.addDocumentInformation(docInfoList);
		ccis.addTokenInformation(tokenInfoList);
		ccis.addSentenceInformation(sentInfoList);



	}

	private List<String> getListProperty(Map<String, Object> properties, String string) {
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
		/* Step 1: create a file of all the possible spots */
		boolean runDS = !filesExist(DSFiles);
		if (runDS) {
			System.err.println("Running DS");
			Spotting spotting = new Spotting(corpusPath, cis, rbased);
			spotting.iterateAndSpot(DSFiles.get(0), c);
		}

		/* Step 2: Generate features */
		boolean runFG = !filesExist(featureFiles);
		if (runFG) {
			System.err.println("Running Feature Generation");
			PerSpotFeatureGeneration fGeneration = new PerSpotFeatureGeneration(fg);
			fGeneration.run(DSFiles, featureFiles, c, cis);
		}

		System.err.println("Training");
		/* Step 3: Training and weight learning */
		// Step 3.1: From the feature file, generate graphs
		// for each input feature training file
		for (int i = 0; i < featureFiles.size(); i++) {
			String featureFile = featureFiles.get(i);
			File modelFile = new File(ntronModelDirs.get(i));
			if (!modelFile.exists())
				modelFile.mkdir();
			MakeGraph.run(featureFiles.get(0), ntronModelDirs.get(0) + File.separatorChar + "mapping", ntronModelDirs.get(0) + File.separatorChar + "train", ntronModelDirs.get(0));
		}
		File modelFile = new File(ntronModelDirs.get(0));
//		//Step 3.2: Now run the super naive training algorithm
//			
		/**Print Graph*/
		String dir = modelFile.getAbsoluteFile().toString();
		Dataset train = new LRGraphMemoryDataset(dir + File.separatorChar + "train");
		LRGraph lrg = new LRGraph();
		BufferedWriter bw = new BufferedWriter(new FileWriter("graph"));
		while(train.next(lrg)) {
			bw.write(lrg.toString() + "\n");
			bw.write("\n\n");
		}
		bw.close();
		/**/
		LperceptTrain.train(modelFile.getAbsoluteFile().toString(),new Random(1));
		
	}

	public static void main(String args[]) throws Exception {
		System.out.println("sg");
		NtronExperiment irb = new NtronExperiment(args[0]);
		irb.run();
		writeFeatureWeights(irb.ntronModelDirs.get(0) + File.separatorChar + "mapping", irb.ntronModelDirs.get(0) + File.separatorChar + "params", irb.ntronModelDirs.get(0) + File.separatorChar + "model", irb.ntronModelDirs.get(0) + File.separatorChar + "weights");
	}

	private boolean filesExist(List<String> dsFiles) {
		for (String s : dsFiles) {
			File f = new File(s);
			if (!f.exists()) {
				System.err.println(s + " File does not exist!Need To Generate it");
				return false;
			}
		}
		return true;
	}
	
	public static void writeFeatureWeights(String mapping, String parametersFile, String modelFile, String outFile) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
		BufferedReader featureReader = new BufferedReader(new FileReader(mapping));
		Integer numRel = Integer.parseInt(featureReader.readLine());
		HashMap<Integer, String> relNumNameMapping = new HashMap<Integer, String>();
		
		for(int i = 0; i < numRel; i++) {
			//skip relation names
			relNumNameMapping.put(i, featureReader.readLine());
		}
		int numFeatures = Integer.parseInt(featureReader.readLine());
		String ftr = null;
		HashMap<Integer, String> featureList = new HashMap<Integer, String>();
		int fno = 0;
		while(fno < numFeatures) {
			ftr = featureReader.readLine();
			featureList.put(fno, ftr);
			fno++;
		}
		Parameters p = new Parameters();
		p.model  = new Model();
		p.model.read(modelFile);
		p.deserialize(parametersFile);
		for(int r = 0; r < p.model.numRelations; r++) {
			String relName = relNumNameMapping.get(r);
			DenseVector dv = p.relParameters[r];
			System.out.println(dv.vals.length);
			for(int i = 0; i < numFeatures; i++) {
				bw.write(relName + "\t" + featureList.get(i) + "\t" + dv.vals[i] + "\n");
			}
		}
		bw.close();
		featureReader.close();
	}

}
