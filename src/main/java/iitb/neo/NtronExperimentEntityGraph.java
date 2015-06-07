//sg
/**
 * An attempt at integrating rule based system with multir
 */
package main.java.iitb.neo;

import iitb.rbased.main.RuleBasedDriver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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

import main.java.iitb.neo.goldDB.GoldDB;
import main.java.iitb.neo.pretrain.featuregeneration.NumbertronFeatureGenerationDriver;
import main.java.iitb.neo.pretrain.process.MakeEntityGraph;
import main.java.iitb.neo.pretrain.spotting.UnitLocationSpotting;
import main.java.iitb.neo.training.algorithm.lpercp.LperceptTrain;
import main.java.iitb.neo.training.algorithm.lpercp.LperceptTrainEntityGraph;
import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.meta.EntityGraphMemoryDataset;
import main.java.iitb.neo.util.JsonUtils;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.corpus.DocumentInformationI;
import edu.washington.multirframework.corpus.SentInformationI;
import edu.washington.multirframework.corpus.TokenInformationI;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
import edu.washington.multirframework.multiralgorithm.Dataset;
import edu.washington.multirframework.multiralgorithm.DenseVector;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;

public class NtronExperimentEntityGraph {
	private String corpusPath;
	private FeatureGenerator mintzKeywordsFg, numberFg, keywordsFg;

	private List<SententialInstanceGeneration> sigs;
	private List<String> DSFiles;

	private List<String> featureFiles;
	private List<String> ntronModelDirs;
	private CorpusInformationSpecification cis;

	private String countriesFile;
	private boolean useKeywordFeatures = false;
	private RuleBasedDriver rbased;
	private Map<String, String> countryFreebaseIdMap;

	// Properties for the averaged perceptron
	double regularizer; // regularizer to dampen the weights
	int numIterations; // the number of iterations of the perceptron
	boolean finalAvg; // should the parameters be finally divided?

	// Gold database matching params
	int topKGoldDb; // match the recent k params from the gold database
	double MARGIN;
	// Feature thresholds
	public static int MINTZ_FEATURE_THRESHOLD;
	public static int KEYWORD_FEATURE_THRESHOLD;

	// confused relations ignoring
	boolean ignoreConfusion;

	public NtronExperimentEntityGraph() {
	}

	public NtronExperimentEntityGraph(String propertiesFile) throws Exception {

		Map<String, Object> properties = JsonUtils.getJsonMap(propertiesFile);

		rbased = new RuleBasedDriver(true);
		corpusPath = JsonUtils.getStringProperty(properties, "corpusPath");

		/**
		 * Create the entity name to id map
		 */

		countriesFile = JsonUtils
				.getStringProperty(properties, "countriesList");

		try {

			BufferedReader br = new BufferedReader(
					new FileReader(countriesFile));
			String countryRecord = null;
			countryFreebaseIdMap = new HashMap<String, String>();
			while ((countryRecord = br.readLine()) != null) {
				String vars[] = countryRecord.split("\t");
				if (vars.length == 2) {
					String countryName = vars[1].toLowerCase();
					String countryId = vars[0];
					countryFreebaseIdMap.put(countryName, countryId);
				}
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/**
		 * end creating the map
		 */

		String useKeywordFeature = JsonUtils.getStringProperty(properties,
				"useKeywordFeatures");
		if (useKeywordFeature != null) {
			if (useKeywordFeature.equals("true")) {
				this.useKeywordFeatures = true;
			}
		}

		String keywordFeatureGeneratorClass = JsonUtils.getStringProperty(
				properties, "keywordsFg");
		String mintzFeatureGeneratorClass = JsonUtils.getStringProperty(
				properties, "mintzKeywordsFg");
		String numbersFeatureGeneratorClass = JsonUtils.getStringProperty(
				properties, "numbersFg");

		if (keywordFeatureGeneratorClass != null
				&& !keywordFeatureGeneratorClass.isEmpty()) {
			this.keywordsFg = (FeatureGenerator) ClassLoader
					.getSystemClassLoader()
					.loadClass(keywordFeatureGeneratorClass).newInstance();
		}
		if (mintzFeatureGeneratorClass != null
				&& !mintzFeatureGeneratorClass.isEmpty()) {
			this.mintzKeywordsFg = (FeatureGenerator) ClassLoader
					.getSystemClassLoader()
					.loadClass(mintzFeatureGeneratorClass).newInstance();

		}
		if (numbersFeatureGeneratorClass != null

		&& !numbersFeatureGeneratorClass.isEmpty()) {
			this.numberFg = (FeatureGenerator) ClassLoader
					.getSystemClassLoader()
					.loadClass(numbersFeatureGeneratorClass).newInstance();
		}

		List<String> sigClasses = JsonUtils.getListProperty(properties, "sigs");
		sigs = new ArrayList<>();
		for (String sigClass : sigClasses) {
			sigs.add((SententialInstanceGeneration) ClassLoader
					.getSystemClassLoader().loadClass(sigClass)
					.getMethod("getInstance").invoke(null));
		}

		List<String> dsFileNames = JsonUtils.getListProperty(properties,
				"dsFiles");
		DSFiles = new ArrayList<>();
		for (String dsFileName : dsFileNames) {
			DSFiles.add(dsFileName);
		}

		List<String> featureFileNames = JsonUtils.getListProperty(properties,
				"featureFiles");
		featureFiles = new ArrayList<>();
		for (String featureFileName : featureFileNames) {
			featureFiles.add(featureFileName);
		}

		ntronModelDirs = new ArrayList<>();
		List<String> multirDirNames = JsonUtils.getListProperty(properties,
				"models");
		for (String multirDirName : multirDirNames) {
			ntronModelDirs.add(multirDirName);
		}

		cis = new CustomCorpusInformationSpecification();

		String altCisString = JsonUtils.getStringProperty(properties, "cis");
		if (altCisString != null) {
			cis = (CustomCorpusInformationSpecification) ClassLoader
					.getSystemClassLoader().loadClass(altCisString)
					.newInstance();
		}

		// CorpusInformationSpecification
		List<String> tokenInformationClassNames = JsonUtils.getListProperty(
				properties, "ti");
		List<TokenInformationI> tokenInfoList = new ArrayList<>();
		for (String tokenInformationClassName : tokenInformationClassNames) {
			tokenInfoList.add((TokenInformationI) ClassLoader
					.getSystemClassLoader()
					.loadClass(tokenInformationClassName).newInstance());
		}

		List<String>

		sentInformationClassNames = JsonUtils.getListProperty(properties, "si");
		List<SentInformationI> sentInfoList = new ArrayList<>();
		for (String sentInformationClassName : sentInformationClassNames) {
			sentInfoList.add((SentInformationI) ClassLoader
					.getSystemClassLoader().loadClass(sentInformationClassName)
					.newInstance());
		}

		List<String> docInformationClassNames = JsonUtils.getListProperty(
				properties, "di");
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

		// perceptron params setup
		this.regularizer = JsonUtils.getDoubleProperty(properties,
				"regularizer");
		this.numIterations = JsonUtils.getIntegerProperty(properties,
				"iterations");
		this.finalAvg = JsonUtils.getBooleanProperty(properties, "finalAvg");

		// gold db params setup

		String goldDBFileLoc = JsonUtils.getStringProperty(properties,
				"kbRelFile");

		this.topKGoldDb = JsonUtils
				.getIntegerProperty(properties, "topKGoldDb");
		this.MARGIN = JsonUtils.getDoubleProperty(properties, "margin");
		GoldDB.initializeGoldDB(goldDBFileLoc, topKGoldDb, MARGIN);

		// Feature thresholds
		KEYWORD_FEATURE_THRESHOLD = JsonUtils.getIntegerProperty(properties,
				"keywordFeatureThreshold");
		MINTZ_FEATURE_THRESHOLD = JsonUtils.getIntegerProperty(properties,
				"mintzFeatureThreshold");

		// Confused relations ignore
		this.ignoreConfusion = JsonUtils.getBooleanProperty(properties,
				"ignoreConfusion");
	}

	/**
	 * The orchestrator. Runs spotting, preprocessing, feature generation and
	 * training in this order. Only starts steps that are needed
	 * 
	 * @throws SQLException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void run() throws SQLException, IOException, InterruptedException,
			ExecutionException {
		Corpus c = new Corpus(corpusPath, cis, true);
		/* Step 1: create a file of all the possible spots */
		boolean runDS = !filesExist(DSFiles);
		if (runDS) {
			System.err.println("Running DS");
			UnitLocationSpotting spotting = new UnitLocationSpotting(
					corpusPath, cis, rbased, countriesFile);
			spotting.iterateAndSpot(DSFiles.get(0), c);
		}

		/* Step 2: Generate features */
		boolean runFG = !filesExist(featureFiles);
		if (runFG) {
			System.err.println("Running Feature Generation");
			NumbertronFeatureGenerationDriver fGeneration = new NumbertronFeatureGenerationDriver(
					mintzKeywordsFg, numberFg, keywordsFg);
			fGeneration.run(DSFiles, featureFiles, c, cis);
		}

		System.err.println("Training " + ntronModelDirs.get(0));
		/* Step 3: Training and weight learning */
		// Step 3.1: From the feature file, generate graphs
		// for each input feature training file
		for (int i = 0; i < featureFiles.size(); i++) {
			File modelFile = new File(ntronModelDirs.get(i));
			if (!modelFile.exists())
				modelFile.mkdir();
			MakeEntityGraph.run(featureFiles.get(i), ntronModelDirs.get(0)
					+ File.separatorChar + "mapping", ntronModelDirs.get(0)
					+ File.separatorChar + "train", ntronModelDirs.get(0));
		}
		File modelFile = new File(ntronModelDirs.get(0));

		// /**Print Graph*/
		// String dir = modelFile.getAbsoluteFile().toString();
		Dataset<EntityGraph> train = new EntityGraphMemoryDataset(ntronModelDirs.get(0)
				+ File.separatorChar + "train");
		EntityGraph egraph = new EntityGraph();
		BufferedWriter bw = new BufferedWriter(new FileWriter("graph"));
		while (train.next(egraph)) {
			bw.write(egraph.toString() + "\n");
			bw.write("\n\n");
		}
		bw.close();
		/**/
		LperceptTrainEntityGraph.train(modelFile.getAbsoluteFile().toString(), new Random(
				1), this.numIterations, this.regularizer, this.finalAvg,
				this.ignoreConfusion, ntronModelDirs.get(0)
						+ File.separatorChar + "mapping");

	}

	public static void main(String args[]) throws Exception {

		NtronExperimentEntityGraph irb = new NtronExperimentEntityGraph(args[0]);
		irb.run();
		// PrintWriter pw = new PrintWriter("hitstats");
		//
		// GoldDbInference.printMatchStats(pw);
		// pw.close();
		 /*writeFeatureWeights(irb.ntronModelDirs.get(0) + File.separatorChar
		 + "mapping", irb.ntronModelDirs.get(0) + File.separatorChar
		 + "params", irb.ntronModelDirs.get(0) + File.separatorChar
		 + "model", irb.ntronModelDirs.get(0) + File.separatorChar
		 + "weights");*/
	}

	private boolean filesExist(List<String> dsFiles) {
		for (String s : dsFiles) {
			File f = new File(s);
			if (!f.exists()) {
				System.err.println(s
						+ " File does not exist!Need To Generate it");
				return false;
			}
		}
		return true;
	}

	/**
	 * Just a meta function to facilitate debugging. Creates a fairly large
	 * feature weight file for each for the relations.
	 * 
	 * @param mapping
	 * @param parametersFile
	 * @param modelFile
	 * @param outFile
	 * @throws IOException
	 */
	public static void writeFeatureWeights(String mapping,
			String parametersFile, String modelFile, String outFile)
			throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
		BufferedReader featureReader = new BufferedReader(new FileReader(
				mapping));
		Integer numRel = Integer.parseInt(featureReader.readLine());
		HashMap<Integer, String> relNumNameMapping = new HashMap<Integer, String>();

		for (int i = 0; i < numRel; i++) {
			// skip relation names
			relNumNameMapping.put(i, featureReader.readLine());
		}
		int numFeatures = Integer.parseInt(featureReader.readLine());
		String ftr = null;
		HashMap<Integer, String> featureList = new HashMap<Integer, String>();
		int fno = 0;
		while (fno < numFeatures) {
			ftr = featureReader.readLine();
			featureList.put(fno, ftr);
			fno++;
		}
		Parameters p = new Parameters();
		p.model = new Model();
		p.model.read(modelFile);
		p.deserialize(parametersFile);
		for (int r = 0; r < p.model.numRelations; r++) {
			String relName = relNumNameMapping.get(r);
			DenseVector dv = p.relParameters[r];
			// System.out.println(dv.vals.length);
			for (int i = 0; i < numFeatures; i++) {
				bw.write(relName + "\t" + featureList.get(i) + "\t"
						+ dv.vals[i] + "\n");
			}
		}
		bw.close();
		featureReader.close();
	}

	/**
	 * 
	 * @param iterations
	 *            Iterations of the perceptron
	 * @param regularizer
	 *            Damper
	 * @param topk
	 *            Match the top k values in the gold db
	 * @param margin
	 *            match margin
	 * @param mintzFeatureThreshold
	 * @param kwFeatureThreshold
	 * @param finalAvg
	 *            Should the parameters be averaged?
	 * @param extractionCutoff
	 */
	public void updateHyperparams(int iterations, double regularizer, int topk,
			double margin, int mintzFeatureThreshold, int kwFeatureThreshold,
			boolean finalAvg) {
		this.numIterations = iterations;
		this.regularizer = regularizer;
		this.topKGoldDb = topk;
		MINTZ_FEATURE_THRESHOLD = mintzFeatureThreshold;
		KEYWORD_FEATURE_THRESHOLD = kwFeatureThreshold;
		this.finalAvg = finalAvg;

	}

}
