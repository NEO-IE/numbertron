package main.java.iitb.neo.pretrain.featuregeneration;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.code.externalsorting.ExternalSort;

import edu.washington.multirframework.multiralgorithm.MILDocument;
import edu.washington.multirframework.multiralgorithm.Mappings;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;

/**
 * This main method takes the featuresTest
 * and featuresTrain file and creates all the necessary
 * Multir files like mapping, model, train, test.
 * @author jgilme1
 *
 */
public class Preprocess {
	public static Map<String,Integer> keyToIntegerMap = new HashMap<String,Integer>();
	public static Map<Integer,String> intToKeyMap = new HashMap<Integer,String>();
    public static int FEATURE_THRESHOLD = 2;
    
    private static final double GIGABYTE_DIVISOR = 1073741824;
	/**
	 * args[0] is path to featuresTrain
	 * args[1] is path directory for new multir files like
	 * 			mapping, model, train, test..
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) 
	throws IOException {
            run(args[0],args[1]);
	}

	public static void run (String featureFile, String trainDir) throws IOException{
		run(featureFile,trainDir,FEATURE_THRESHOLD);
	}
	public static void run(String featureFile, String trainDir, Integer threshold) throws IOException{
		FEATURE_THRESHOLD = threshold;
    	long start = System.currentTimeMillis();
    	
    	printMemoryStatistics();

		String trainFile = featureFile;
		String outDir = trainDir;
		String mappingFile = outDir + File.separatorChar + "mapping";
		String modelFile = outDir + File.separatorChar + "model";
		
		System.out.println("GETTING Mapping form training data");
		Mappings mapping = getMappingFromTrainingData(trainFile,mappingFile);
		

		
			System.out.println("PREPROCESSING TRAIN FEATURES");
		{
			String output1 = outDir + File.separatorChar + "train";
			convertFeatureFileToMILDocument(trainFile, output1, mapping);
		}
		
			System.out.println("FINISHED PREPROCESSING TRAIN FEATURES");
			printMemoryStatistics();
			keyToIntegerMap.clear();

			intToKeyMap.clear();

	
		System.out.println("Writing model and mapping file");
		printMemoryStatistics();


		
		{
			Model m = new Model();
			m.numRelations = mapping.numRelations();
			m.numFeaturesPerRelation = new int[m.numRelations];
			for (int i=0; i < m.numRelations; i++)
				m.numFeaturesPerRelation[i] = mapping.numFeatures();
			m.write(modelFile);
			mapping.write(mappingFile);
		}
		
    	long end = System.currentTimeMillis();
    	System.out.println("Preprocessing took " + (end-start) + " millisseconds");
		
	}
	


	/**
	 * Obtain mappings object from training features file onle
	 * @param trainFile
	 * @param mappingFile
	 * @return
	 * @throws IOException
	 */
	public static Mappings getMappingFromTrainingData(String trainFile,
			String mappingFile) throws IOException {

		Mappings m = new Mappings();
		//ensure that "NA" gets ID o
		m.getRelationID("NA", true);
		
		System.out.println("Converting input feature file to temporary feature list file...");
		long start = System.currentTimeMillis();
		//grab feature from feature file and put in new file
		BufferedReader br = new BufferedReader(new FileReader(new File(trainFile)));
		File trainFileF = new File(trainFile);
		File tmpFeatureFile = new File(trainFileF.getParentFile().getAbsolutePath()+"/"+trainFileF.getName()+"-tmpFeatureFile-"+new Random(System.nanoTime()).nextInt());
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFeatureFile));
		String nextLine;
		int count =0;
		while((nextLine = br.readLine())!=null){
			
			/*
			 * @ashishm: made changes to handle number features.
			 */
			String[] parts = nextLine.split("@@");
			
			String[]values = parts[0].split("\t");
			for(int i = 4; i < values.length; i++){
				bw.write(values[i]+"\n");
			}
			if(parts.length == 2){
				values = parts[1].split("\t");
				for(int i = 0; i < values.length; i++){
					bw.write(values[i]+"\n");
				}
			}
			count++;
			if(count % 100000 == 0){
				System.out.println(count + " lines processed");
			}
		}
		br.close();
		bw.close();
		long end = System.currentTimeMillis();
		System.out.println("Converted feature file to temporary feature list file in " + (end-start) + " milliseconds");

		System.out.println("Sorting temporary feature list file....");
		start = end;
		//sort new file by feature string
		File sortedTmpFeatureFile = new File(tmpFeatureFile.getParentFile().getAbsolutePath()+"/"+trainFileF.getName()+"-sortedTmpFeatureFile-"+new Random(System.nanoTime()).nextInt());
		externalSort(tmpFeatureFile,sortedTmpFeatureFile,ExternalSort.defaultcomparator);
//		ExternalSort.sortAndSave(tmplist, cmp, cs, tmpdirectory)
//		ExternalSort.sort(tmpFeatureFile, sortedTmpFeatureFile);
        end = System.currentTimeMillis();
		System.out.println("Sorted temporary feature list file in " + (end-start) + " milliseconds");

		
		br = new BufferedReader(new FileReader(sortedTmpFeatureFile));
		
			
		String line;
		count = 0 ;
		String prevFeature = "";
		int prevCount = 0;
		System.out.println("Converting feature list to feature mapping object");
		while((line = br.readLine()) != null){
			String feature = line.trim();
			if(feature.equals(prevFeature)){
				prevCount++;
			}
			else{
				prevFeature = feature;
				prevCount = 1;
			}
			
			if(prevCount == FEATURE_THRESHOLD){
				m.getFeatureID(feature, true);
			}
	    	count++;
	    	if(count % 100000 == 0){
	    		System.out.println(count + " features processed");
	    	}
		}
		

		
		br.close();
		tmpFeatureFile.delete();
		sortedTmpFeatureFile.delete();
		return m;
	}



	/**
	 * Converts featuresTrain or featuresTest to 
	 * train or test by aggregating the entity pairs
	 * into relations and their mentions.
	 * @param input - the test/train file in non-multir
	 * 				  format
	 * @param output - the test/train file in multir,
	 * 					MILDoc format
	 * @param m - the mappings object that keeps track of
	 * 			  relevant relations and features
	 * @param mentionThreshold 
	 * @param collapseSentences 
	 * @throws IOException
	 */
	private static void convertFeatureFileToMILDocument(String input, String output, 
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
		externalSort(new File(input),tempSortedFeatureFile,entityPairComparator);
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
	    		printMemoryStatistics();
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
	
	public static void externalSort(File srcFile, File destFile, Comparator<String> comparator) throws IOException {
		
		File tmpDir = destFile.getParentFile();
		List<File> tmpFiles = ExternalSort.sortInBatch(srcFile, comparator, 4096, 
				Charset.defaultCharset(), tmpDir, false);
		ExternalSort.mergeSortedFiles(tmpFiles, destFile,comparator);
		
	}

	private static MILDocument constructMILDOC(List<Integer> relInts, List<List<Integer>> featureInts,
			String arg1, String arg2){
		MILDocument doc = new MILDocument();
    	doc.arg1 = arg1;
    	doc.arg2 = arg2;
		
    	// set relations
    	{
	    	int[] irels = new int[relInts.size()];
	    	for (int i=0; i < relInts.size(); i++)
	    		irels[i] = relInts.get(i);
	    	Arrays.sort(irels);
	    	// ignore NA and non-mapped relations
	    	int countUnique = 0;
	    	for (int i=0; i < irels.length; i++)
	    		if (irels[i] > 0 && (i == 0 || irels[i-1] != irels[i]))
	    			countUnique++;
	    	doc.Y = new int[countUnique];
	    	int pos = 0;
	    	for (int i=0; i < irels.length; i++)
	    		if (irels[i] > 0 && (i == 0 || irels[i-1] != irels[i]))
	    			doc.Y[pos++] = irels[i];
    	}
    	
    	// set mentions
    	doc.setCapacity(featureInts.size());
    	doc.numMentions = featureInts.size();
    	
    	for (int j=0; j < featureInts.size(); j++) {
	    	doc.Z[j] = -1;
    		doc.mentionIDs[j] = j;
    		SparseBinaryVector sv = doc.features[j] = new SparseBinaryVector();
    		
    		List<Integer> instanceFeatures = featureInts.get(j);
    		int[] fts = new int[instanceFeatures.size()];
    		
    		for (int i=0; i < instanceFeatures.size(); i++)
    			fts[i] = instanceFeatures.get(i);
    		Arrays.sort(fts);
	    	int countUnique = 0;
	    	for (int i=0; i < fts.length; i++)
	    		if (fts[i] != -1 && (i == 0 || fts[i-1] != fts[i]))
	    			countUnique++;
	    	sv.num = countUnique;
	    	sv.ids = new int[countUnique];
	    	int pos = 0;
	    	for (int i=0; i < fts.length; i++)
	    		if (fts[i] != -1 && (i == 0 || fts[i-1] != fts[i]))
	    			sv.ids[pos++] = fts[i];
	    	
    	}
    	
    	return doc;
	}

	public static List<Integer> convertFeaturesToIntegers(
			List<String> features, Mappings m) {
		
		List<Integer> intFeatures = new ArrayList<Integer>();
		
		for(String feature: features){
			Integer intFeature = m.getFeatureID(feature, false);
			if(intFeature != -1){
				intFeatures.add(intFeature);
			}
		}
		
		return intFeatures;
	}

	
	public static void printMemoryStatistics() {
		double freeMemory = Runtime.getRuntime().freeMemory()/GIGABYTE_DIVISOR;
		double allocatedMemory = Runtime.getRuntime().totalMemory()/GIGABYTE_DIVISOR;
		double maxMemory = Runtime.getRuntime().maxMemory()/GIGABYTE_DIVISOR;
		System.out.println("MAX MEMORY: " + maxMemory);
		System.out.println("ALLOCATED MEMORY: " + allocatedMemory);
		System.out.println("FREE MEMORY: " + freeMemory);
	}
}