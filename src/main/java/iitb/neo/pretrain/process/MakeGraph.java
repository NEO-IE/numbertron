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

import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.ds.Number;
import edu.washington.multir.development.Preprocess;
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
public class MakeGraph {

	public static int FEATURE_THRESHOLD = 2;
	private static final double GIGABYTE_DIVISOR = 1073741824;

	static HashMap<String, Integer> relToRelnumberMap;
	static {
		relToRelnumberMap = new HashMap<String, Integer>();

		relToRelnumberMap.put("AGL", 0);
		relToRelnumberMap.put("FDI", 1);
		relToRelnumberMap.put("GOODS", 2);
		relToRelnumberMap.put("ELEC", 3);
		relToRelnumberMap.put("CO2", 4);
		relToRelnumberMap.put("INF", 5);
		relToRelnumberMap.put("INTERNET", 6);
		relToRelnumberMap.put("GDP", 7);
		relToRelnumberMap.put("LIFE", 8);
		relToRelnumberMap.put("POP", 9);
		relToRelnumberMap.put("DIESEL", 10);

	}

	public static void main(String args[]) {

	}

	public static void run(String featureFile, String trainDir) throws IOException {
		run(featureFile, trainDir, FEATURE_THRESHOLD);
	}

	public static void run(String featureFile, String trainDir, Integer threshold) throws IOException {
		FEATURE_THRESHOLD = threshold;
		long start = System.currentTimeMillis();

		Preprocess.printMemoryStatistics();

		String trainFile = featureFile;
		String outDir = trainDir;
		String mappingFile = outDir + File.separatorChar + "mapping";
		String modelFile = outDir + File.separatorChar + "model";

		System.out.println("GETTING Mapping form training data");
		Mappings mapping = Preprocess.getMappingFromTrainingData(trainFile, mappingFile);

		System.out.println("PREPROCESSING TRAIN FEATURES");
		{
			String output1 = outDir + File.separatorChar + "train";
			convertFeatureFileToLRGraph(trainFile, output1, mapping);
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
			mapping.write(mappingFile);
		}

		long end = System.currentTimeMillis();
		System.out.println("Preprocessing took " + (end - start) + " millisseconds");

	}

	private static void convertFeatureFileToLRGraph(String input, String output, Mappings m) throws IOException {

		// external sorting mechanism so we don't have to keep entity pairs in
		// memory
		Comparator<String> locationRelationPairComparator = new Comparator<String>() {
			@Override
			public int compare(String line1, String line2) {
				String[] line1Values = line1.split("\t");
				String[] line2Values = line2.split("\t");
				int locationIdx = 1;
				int relationIdx = 3;
				Integer entity1Compare = line1Values[locationIdx].compareTo(line2Values[locationIdx]);
				return entity1Compare == 0 ? line1Values[relationIdx].compareTo(line2Values[relationIdx])
						: entity1Compare;
			}

		};

		File inputFile = new File(input);
		File tempSortedFeatureFile = new File(inputFile.getParentFile().getAbsolutePath() + "/" + inputFile.getName()
				+ "-sortedFeaturesFile-" + new Random(System.nanoTime()).nextInt());
		long start = System.currentTimeMillis();
		System.out.println("Sorting feature file");

		Preprocess.externalSort(new File(input), tempSortedFeatureFile, locationRelationPairComparator);
		long end = System.currentTimeMillis();
		System.out.println("Feature file sorted in " + (end - start) + " milliseconds");

		// open input and output streams
		DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)));

		BufferedReader br = new BufferedReader(new FileReader(tempSortedFeatureFile));
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
		HashMap<String, List<Integer>> numberSentenceMap = null; // stores the
																	// sentences
																	// in which
																	// the
																	// current
																	// number
																	// appears
		while ((line = br.readLine()) != null) {
			mentionNumber++;

			String[] values = line.split("\t");
			String location = values[1];
			String number = values[2];
			String relString = values[3];

			String key = location + "%" + relString;

			List<String> features = new ArrayList<>();
			// add all features
			for (int i = 4; i < values.length; i++) {
				features.add(values[i]);
			}
			// convert to integer keys from the mappings m object
			List<Integer> featureIntegers = Preprocess.convertFeaturesToIntegers(features, m);

			if (key.equals(prevKey)) { // same location relation, add
				featureLists.add(featureIntegers);
				if (numberSentenceMap.keySet().contains(number)) { // number
																	// already
																	// exists
					numberSentenceMap.get(number).add(mentionNumber);
				}
			} else {
				// construct MILDoc from currentFeatureLists
				if (!prevKey.equals("")) { // not first time round?
					String[] v = prevKey.split("%");
					ArrayList<Number> numbers = new ArrayList<Number>();
					for (String num : numberSentenceMap.keySet()) {
						numbers.add(new Number(num, numberSentenceMap.get(num)));
					}
					constructLRGraph(numbers, featureLists, v[0], v[1]).write(os);

				}
				// reste featureLists and prevKey
				numberSentenceMap = new HashMap<String, List<Integer>>();

				featureLists = new ArrayList<>();
				featureLists.add(featureIntegers);
				prevKey = key;
			}

			count++;
			if (count % 100000 == 0) {
				System.out.println("Number of training instances read in =" + count);
				Preprocess.printMemoryStatistics();
			}
		}

		// construct last MILDOC from featureLists
		if (!prevKey.equals("")) {
			String[] v = prevKey.split("%");
			ArrayList<Number> numbers = new ArrayList<Number>();
			for (String num : numberSentenceMap.keySet()) {
				numbers.add(new Number(num, numberSentenceMap.get(num)));
			}
			constructLRGraph(numbers, featureLists, v[0], v[1]).write(os);

		}

		br.close();
		os.close();
		tempSortedFeatureFile.delete();
	}

	private static LRGraph constructLRGraph(List<Number> numbers, List<List<Integer>> featureInts, String location,
			String relation) {
		LRGraph lrg = new LRGraph();
		lrg.location = location;
		lrg.relation = relation;
		lrg.relNumber = relToRelnumberMap.get(relation);
		// set number nodes

		int numNodesCount = numbers.size();
		lrg.n = new Number[numNodesCount];
		for (int i = 0; i < numNodesCount; i++) {
			lrg.n[i] = numbers.get(i); // just for performance reasons, too
										// early and perhaps evil. But worth a
										// try;
		}
		// set mentions
		lrg.setCapacity(featureInts.size());
		lrg.numMentions = featureInts.size();

		for (int j = 0; j < featureInts.size(); j++) {
			lrg.Z[j] = -1;
			lrg.mentionIDs[j] = j;
			SparseBinaryVector sv = lrg.features[j] = new SparseBinaryVector();

			List<Integer> instanceFeatures = featureInts.get(j);
			int[] fts = new int[instanceFeatures.size()];

			for (int i = 0; i < instanceFeatures.size(); i++)
				fts[i] = instanceFeatures.get(i);
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

		}

		return lrg;
	}

}
