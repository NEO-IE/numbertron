package main.java.iitb.neo.pretrain.process;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import main.java.iitb.neo.pretrain.featuregeneration.NumbertronFeatureGenerationDriver;
import main.java.iitb.neo.pretrain.featuregeneration.Preprocess;
import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.ds.Number;
import meta.RelationMetaData;
import edu.washington.multirframework.multiralgorithm.Mappings;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;

/**
 * This class assumes that the features have been created and then uses the
 * feature file to create the graphs (MIML docs in the multir parlance, we can
 * call it something else).
 * 
 * Relies heavily on Preprocess.java from the multirframework.
 * 
 * @author aman
 * 
 */
public class MakeEntityGraph {

	public static int FEATURE_THRESHOLD = 1;
	public static Mappings mapping = null;
	// private static final double GIGABYTE_DIVISOR = 1073741824;

	public static void main(String args[]) {

	}

	public static void run(String featureFile, String mappingFile,
			String graphFile, String trainDir) throws IOException {
		run(featureFile, mappingFile, graphFile, trainDir, FEATURE_THRESHOLD);
	}

	public static void run(String featureFile, String mappingFile,
			String graphFile, String trainDir, Integer threshold)
			throws IOException {
		FEATURE_THRESHOLD = threshold;
		long start = System.currentTimeMillis();
		boolean generateMapping = false;
		Preprocess.printMemoryStatistics();

		String outDir = trainDir;

		String modelFile = outDir + File.separatorChar + "model";

		System.out.println("GETTING Mapping form training data");
		// Check if mapping file already exists
		File mappingFileHandle = new File(mappingFile);
	
		if (mappingFileHandle.exists()) { // directly read
			mapping = new Mappings();
			mapping.read(mappingFile);
		} else {
			System.out.println("Creating a Mapping File");
			mapping = Preprocess.getMappingFromTrainingData(featureFile,
					mappingFile, true);
			generateMapping = true;
		}

		System.out.println("PREPROCESSING TRAIN FEATURES");
		{
			File graphFileHandle = new File(graphFile);
			if (!graphFileHandle.exists()) {
				String output1 = outDir + File.separatorChar + "train";
				convertFeatureFileToEntityGraph(featureFile, output1, mapping);
			}

		}

		System.out.println("FINISHED PREPROCESSING TRAIN FEATURES");
		Preprocess.printMemoryStatistics();
		Preprocess.keyToIntegerMap.clear();
		Preprocess.intToKeyMap.clear();

		System.out.println("Writing model and mapping file");
		Preprocess.printMemoryStatistics();

		{
			Model m = new Model();
			m.numRelations = mapping.numRelations();
			m.numFeaturesPerRelation = new int[m.numRelations];
			for (int i = 0; i < m.numRelations; i++)
				m.numFeaturesPerRelation[i] = mapping.numFeatures();
			m.write(modelFile);
			if (generateMapping) {
				mapping.write(mappingFile);
			}
		}

		long end = System.currentTimeMillis();
		System.out.println("Preprocessing took " + (end - start)
				+ " millisseconds");

	}

	/**
	 * Given a set of features, creates an entity graph where the entuty graph
	 * is as defined at https://www.overleaf.com/2748088ynypty#/7347851/
	 * 
	 * @param input
	 * @param output
	 * @param m
	 * @throws IOException
	 */
	private static void convertFeatureFileToEntityGraph(String input,
			String output, Mappings m) throws IOException {

		Comparator<String> entityComparator = new Comparator<String>() {
			@Override
			public int compare(String line1, String line2) {
				return line1.compareTo(line2);
			}

		};

		File inputFile = new File(input);
		File tempSortedFeatureFile = new File(inputFile.getParentFile()
				.getAbsolutePath()
				+ "/"
				+ inputFile.getName()
				+ "-sortedFeaturesFile-"
				+ new Random(System.nanoTime()).nextInt());
		long start = System.currentTimeMillis();
		System.out.println("Sorting feature file");

		// sort the feature file based on the entity
		Preprocess.externalSort(new File(input), tempSortedFeatureFile,
				entityComparator);
		long end = System.currentTimeMillis();
		System.out.println("Feature file sorted in " + (end - start)
				+ " milliseconds");

		// open input and output streams
		DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(output)));

		BufferedReader br = new BufferedReader(new FileReader(
				tempSortedFeatureFile));
		System.out.println("Set up buffered reader");

		// create MILDocument data map
		// load feature generation data into map from argument pair keys to
		// a Pair of List<Integer> relations and a List<List<Integer>> for
		// features
		// for each instance
		// Map<Integer,Pair<List<Integer>,List<List<Integer>>>>
		// relationMentionMap = new HashMap<>();

		String line;
		Integer count = 0;
		String prevKey = "";
		List<List<Integer>> featureLists = new ArrayList<>();

		int mentionNumber = 0; // keeps track of the mention that is being
								// processed for the current location relation.
		HashMap<String, List<Integer>> numberMentionMap = null; // stores the
																// sentences
																// in which
																// the
																// current
																// number
																// appears
		HashMap<String, List<Integer>> numberFeatureMap = null; // stores the
																// features for
																// the numbers

		while ((line = br.readLine()) != null) {

			String[] parts = line
					.split(NumbertronFeatureGenerationDriver.FEATURE_TYPE_SEPARATOR);

			// parts[1] is number features. TODO from here.

			String[] values = parts[0].split("\t");
			String entity = values[1];
			String number = values[2];
			String relString = values[3];

			String key = entity;
			m.getRelationID(relString, true); // add the relation to the list of
												// relations
			List<String> features = new ArrayList<>();
			// add all features
			for (int i = 4; i < values.length; i++) {
				features.add(values[i]);
			}

			List<String> numFeatures = new ArrayList<>();
			if (parts.length == 2) { // if number features
				String[] vals = parts[1].split("\t");
				for (int i = 0; i < vals.length; i++) {
					numFeatures.add(vals[i]);
				}
			}

			// convert to integer keys from the mappings m object
			List<Integer> featureIntegers = Preprocess
					.convertFeaturesToIntegers(features, m);
			List<Integer> numFeatureIntegers = Preprocess
					.convertFeaturesToIntegers(numFeatures, m);

			if (key.equals(prevKey)) { // same entity, add
				featureLists.add(featureIntegers);
				if (numberMentionMap.keySet().contains(number)) { // number
					numberMentionMap.get(number).add(mentionNumber);
				} else { // need to add
					ArrayList<Integer> mentionIds = new ArrayList<Integer>();
					mentionIds.add(mentionNumber);
					numberMentionMap.put(number, mentionIds);
					numberFeatureMap.put(number, numFeatureIntegers);
				}
			} else {
				// a new entity has arrived, collect all the instances that
				// you had about the previous entity, and store them
				if (!prevKey.equals("")) { // not first time round?
					ArrayList<Number> numbers = new ArrayList<Number>();
					for (String num : numberMentionMap.keySet()) {
						numbers.add(new Number(num, numberMentionMap.get(num)));
					}

					EntityGraph egraph = constructEntityGraph(numbers,
							featureLists, prevKey);
					egraph.write(os);
					mentionNumber = 0; // reset the mention number

				}
				// reset featureLists and prevKey
				numberMentionMap = new HashMap<String, List<Integer>>();
				ArrayList<Integer> mentionIds = new ArrayList<Integer>();
				mentionIds.add(mentionNumber);
				numberMentionMap.put(number, mentionIds);

				numberFeatureMap = new HashMap<String, List<Integer>>();
				numberFeatureMap.put(number, numFeatureIntegers);
				featureLists = new ArrayList<>();
				featureLists.add(featureIntegers);
				prevKey = key;
			}

			count++;
			if (count % 100000 == 0) {
				System.out.println("Number of training instances read in ="
						+ count);
				Preprocess.printMemoryStatistics();
			}
			mentionNumber++;
		}

		// construct last MILDOC from featureLists
		if (!prevKey.equals("")) {
			ArrayList<Number> numbers = new ArrayList<Number>();
			for (String num : numberMentionMap.keySet()) {
				numbers.add(new Number(num, numberMentionMap.get(num)));
			}

			EntityGraph newGraph = constructEntityGraph(numbers, featureLists,
					prevKey);
			newGraph.write(os);
		}

		br.close();
		os.close();
		tempSortedFeatureFile.delete();
	}

	private static EntityGraph constructEntityGraph(List<Number> numbers,
			List<List<Integer>> featureInts, String loc) {
		EntityGraph egraph = new EntityGraph();
		egraph.entity = loc;
		// set number nodes
		int numNodesCount = numbers.size();
		egraph.setCapacity(featureInts.size(), numNodesCount);

		egraph.n = new Number[numNodesCount][RelationMetaData.NUM_RELATIONS + 1];

		for (int i = 0; i < numNodesCount; i++) {
			for (int r = 1; r <= RelationMetaData.NUM_RELATIONS; r++) {
				egraph.n[i][r] = numbers.get(i);
			}
		}
		// set mentions

		egraph.numMentions = featureInts.size();

		for (int j = 0; j < featureInts.size(); j++) {
			egraph.features[j] = getSBVfromList(featureInts.get(j));
		}

		// set num Mentions

		egraph.numNodesCount = numNodesCount;
		return egraph;
	}

	public static SparseBinaryVector getSBVfromList(List<Integer> features) {
		SparseBinaryVector sv = new SparseBinaryVector();

		int[] fts = new int[features.size()];

		for (int i = 0; i < features.size(); i++)
			fts[i] = features.get(i);
		Arrays.sort(fts);
		int countUnique = 0;
		for (int i = 0; i < fts.length; i++)
			if (fts[i] != -1 && (i == 0 || fts[i - 1] != fts[i]))
				countUnique++;
		sv.num = countUnique;
		sv.ids = new int[countUnique];
		int pos = 0;
		for (int i = 0; i < fts.length; i++)
			if (fts[i] != -1 && (i == 0 || fts[i - 1] != fts[i]))
				sv.ids[pos++] = fts[i];

		return sv;
	}

}
