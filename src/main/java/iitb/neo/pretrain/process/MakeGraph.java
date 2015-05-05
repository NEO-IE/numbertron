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
import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.ds.Number;
import edu.stanford.nlp.ie.pascal.AcronymModel.Feature;
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

	public static int FEATURE_THRESHOLD = 1;
	//private static final double GIGABYTE_DIVISOR = 1073741824;

	static HashMap<String, Integer> relToRelnumberMap;
	static int numRelations = 11; 
	
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
	

	}

	public static void main(String args[]) {

	}

	public static void run(String featureFile, String mappingFile, String graphFile, String trainDir)
			throws IOException {
		run(featureFile, mappingFile, graphFile, trainDir, FEATURE_THRESHOLD);
	}

	public static void run(String featureFile, String mappingFile, String graphFile, String trainDir, Integer threshold)
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
		Mappings mapping = null;
		if (mappingFileHandle.exists()) { // directly read
			mapping = new Mappings();
			mapping.read(mappingFile);
		} else {
			mapping = Preprocess.getMappingFromTrainingData(featureFile, mappingFile);
			generateMapping = true;
		}

		System.out.println("PREPROCESSING TRAIN FEATURES");
		{
			File graphFileHandle = new File(graphFile);
			if (!graphFileHandle.exists()) {
				String output1 = outDir + File.separatorChar + "train";
				convertFeatureFileToLRGraph(featureFile, output1, mapping);
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
		System.out.println("Preprocessing took " + (end - start) + " millisseconds");

	}

	private static void convertFeatureFileToLRGraph(String input, String output, Mappings m) throws IOException {

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
		List<String> ZsentenceID = new ArrayList<>();

		int mentionNumber = 0; // keeps track of the mention that is being
								// processed for the current location relation.
		HashMap<String, List<Integer>> numberMentionMap = null; // stores the
																	// sentences
																	// in which
																	// the
																	// current
																	// number
																	// appears
		HashMap<String, List<Integer>> numberFeatureMap = null; //stores the features for the numbers
		String sentenceID = null;
		
		while ((line = br.readLine()) != null) {

			String[] parts = line.split(NumbertronFeatureGenerationDriver.FEATURE_TYPE_SEPARATOR);
			
			//parts[1] is number features. TODO from here.
			
			String[] values = parts[0].split("\t");
			
			sentenceID = values[0];
			
			String location = values[1];
			String number = values[2];
			String relString = values[3];

			String key = location + "%" + relString;
			m.getRelationID(relString, true); // add the relation to the list of
												// relations
			List<String> features = new ArrayList<>();
			// add all features
			for (int i = 4; i < values.length; i++) {
				features.add(values[i]);
			}
			
			List<String> numFeatures = new ArrayList<>();
			if(parts.length == 2) { //if number features
				String[] vals = parts[1].split("\t");
				for(int i = 0; i < vals.length; i++){
					numFeatures.add(vals[i]);
				}
			}
			
			// convert to integer keys from the mappings m object
			List<Integer> featureIntegers = Preprocess.convertFeaturesToIntegers(features, m);
			List<Integer> numFeatureIntegers = Preprocess.convertFeaturesToIntegers(numFeatures, m);

			if (key.equals(prevKey)) { // same location relation, add
				featureLists.add(featureIntegers);
				
				ZsentenceID.add(sentenceID);
				
				if (numberMentionMap.keySet().contains(number)) { // number
					numberMentionMap.get(number).add(mentionNumber);
				} else { // need to add
					ArrayList<Integer> mentionIds = new ArrayList<Integer>();
					mentionIds.add(mentionNumber);
					numberMentionMap.put(number, mentionIds);
					numberFeatureMap.put(number, numFeatureIntegers);
				}
			} else {
				// construct MILDoc from currentFeatureLists
				if (!prevKey.equals("")) { // not first time round?
					String[] v = prevKey.split("%");
					ArrayList<Number> numbers = new ArrayList<Number>();
					for (String num : numberMentionMap.keySet()) {
						numbers.add(new Number(num, numberMentionMap.get(num)));
					}
					// m/0154j AGL

					LRGraph lr = constructLRGraph(numbers, featureLists, numberFeatureMap, v[0], v[1], m.getRelationID(v[1], false), ZsentenceID);
					lr.write(os);
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
				ZsentenceID = new ArrayList<>();
				ZsentenceID.add(sentenceID);
				prevKey = key;
			}

			count++;
			if (count % 100000 == 0) {
				System.out.println("Number of training instances read in =" + count);
				Preprocess.printMemoryStatistics();
			}
			mentionNumber++;
		}

		// construct last MILDOC from featureLists
		if (!prevKey.equals("")) {
			String[] v = prevKey.split("%");
			ArrayList<Number> numbers = new ArrayList<Number>();
			for (String num : numberMentionMap.keySet()) {
				numbers.add(new Number(num, numberMentionMap.get(num)));
			}

			LRGraph newGraph = constructLRGraph(numbers, featureLists, numberFeatureMap, v[0], v[1], m.getRelationID(v[1], false), ZsentenceID);
			newGraph.write(os);	
		}

		br.close();
		os.close();
		tempSortedFeatureFile.delete();
	}

	private static LRGraph constructLRGraph(List<Number> numbers, 
			List<List<Integer>> featureInts, HashMap<String, List<Integer>> numberFeatureMap, 
			String location, String relation, int relNumber, List<String> sentenceID) {
		LRGraph lrg = new LRGraph();
		lrg.location = location;
		lrg.relation = relation;
		lrg.relNumber = relNumber;
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
			lrg.features[j] = getSBVfromList(featureInts.get(j));
			lrg.sentenceIDs[j] = Integer.parseInt(sentenceID.get(j));
		}
		
		//set num Mentions
		lrg.setNumCapacity(numNodesCount);
		lrg.numNodesCount = numNodesCount;
		
		for(int j = 0; j < numNodesCount; j++){
			lrg.N[j] = -1;
			lrg.numMentionIDs[j] = j;
			lrg.numFeatures[j] = getSBVfromList(numberFeatureMap.get(lrg.n[j].svalue));
		}

		return lrg;
	}
	
	public static SparseBinaryVector getSBVfromList(List<Integer> features){
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
