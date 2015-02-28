package main.java.iitb.neo.pretrain.process;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import edu.washington.multir.development.Preprocess;
import edu.washington.multirframework.multiralgorithm.Mappings;
import edu.washington.multirframework.multiralgorithm.Model;

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
			convertFeatureFileToLRDocument(trainFile, output1, mapping);
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
	
	private static void convertFeatureFileToLRGraph(String input, String output, 
			Mappings m) throws IOException {
		
		//external sorting mechanism so we don't have to keep entity pairs in memory
		Comparator<String> entityPairComparator = new Comparator<String>(){
			@Override
			public int compare(String line1, String line2) {
				String[] line1Values = line1.split("\t");
				String[] line2Values = line2.split("\t");
				
				Integer entity1Compare = line1Values[1].compareTo(line2Values[1]);
				return entity1Compare==0 ? line1Values[2].compareTo(line2Values[2]) : entity1Compare;
			}
			
		};
		
		
		File inputFile = new File(input);
		File tempSortedFeatureFile = new File(inputFile.getParentFile().getAbsolutePath()+"/"+inputFile.getName()+"-sortedFeaturesFile-"+new Random(System.nanoTime()).nextInt());
		long start = System.currentTimeMillis();
		System.out.println("Sorting feature file");
	
		Preprocess.externalSort(new File(input),tempSortedFeatureFile,entityPairComparator);
		long end = System.currentTimeMillis();
		System.out.println("Feature file sorted in "  + (end-start) + " milliseconds");
		
		
		//open input and output streams
		DataOutputStream os = new DataOutputStream
			(new BufferedOutputStream(new FileOutputStream(output)));
	
		BufferedReader br = new BufferedReader(new FileReader(tempSortedFeatureFile));
		System.out.println("Set up buffered reader");
	    
	    //create MILDocument data map
	    //load feature generation data into map from argument pair keys to
	    //a Pair of List<Integer> relations and a List<List<Integer>> for features
	    //for each instance
	    //Map<Integer,Pair<List<Integer>,List<List<Integer>>>> relationMentionMap = new HashMap<>();
	    
	    String line;
	    Integer count =0;
	    String prevKey = "";
	    List<List<Integer>> featureLists= new ArrayList<>();
	    List<Integer> relInts = new ArrayList<>();
	    while((line = br.readLine()) != null){
	    	String[] values = line.split("\t");
	    	String arg1Id = values[1];
	    	String arg2Id = values[2];
	    	String relString = values[3];	    	
	    	String[] rels = relString.split("&&");
	    	List<Integer> intRels = new ArrayList<>();
	    	for(String rel : rels){
	    		intRels.add(m.getRelationID(rel, true));
	    	}	    	
	    	String key = arg1Id+"%"+arg2Id;
	    	List<String> features = new ArrayList<>();
	    	//add all features
	    	for(int i = 4; i < values.length; i++){
	    		features.add(values[i]);
	    	}
	    	//convert to integer keys from the mappings m object
	    	List<Integer> featureIntegers = convertFeaturesToIntegers(features,m);
	    	
	    	if(key.equals(prevKey)){
	    		featureLists.add(featureIntegers);
	    		for(Integer relInt: intRels){
	    			if(!relInts.contains(relInt)) relInts.add(relInt);
	    		}
	    	}
	    	else{
	    		//construct MILDoc from currentFeatureLists
	    		if(!prevKey.equals("")){
	    			String[] v = prevKey.split("%");
	    			constructMILDOC(relInts,featureLists,v[0],v[1]).write(os);
	    		
	    		}
	    		//reste featureLists and prevKey
	    		relInts = new ArrayList<>();
	    		for(Integer relInt: intRels){
	    			if(!relInts.contains(relInt)) relInts.add(relInt);
	    		}
	    		featureLists = new ArrayList<>();
	    		featureLists.add(featureIntegers);
	    		prevKey = key;
	    	}
	    	
	    	
	    	count++;
	    	if(count % 100000 == 0){
	    		System.out.println("Number of training instances read in =" + count);
	    		Preprocess.printMemoryStatistics();
	    	}
	    }
	    
	    //construct last MILDOC from featureLists
		if(!prevKey.equals("")){
			String[] v = prevKey.split("%");
			constructMILDOC(relInts,featureLists,v[0],v[1]).write(os);
		
		}
	    
	    br.close();
		os.close();
		tempSortedFeatureFile.delete();
	}

}
