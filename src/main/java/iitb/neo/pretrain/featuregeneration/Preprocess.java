package main.java.iitb.neo.pretrain.featuregeneration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import main.java.iitb.neo.NtronExperiment;

import com.google.code.externalsorting.ExternalSort;

import edu.washington.multirframework.multiralgorithm.Mappings;

/**
 * This main method takes the featuresTest and featuresTrain file and creates
 * all the necessary Multir files like mapping, model, train, test.
 * 
 * @author jgilme1
 * 
 */
public class Preprocess {
	public static Map<String, Integer> keyToIntegerMap = new HashMap<String, Integer>();
	public static Map<Integer, String> intToKeyMap = new HashMap<Integer, String>();
	
	
	private static final double GIGABYTE_DIVISOR = 1073741824;

	/**
	 * args[0] is path to featuresTrain args[1] is path directory for new multir
	 * files like mapping, model, train, test..
	 * 
	 * @param args
	 * @throws IOException
	 */
	// public static void main(String[] args)
	// throws IOException {
	// run(args[0],args[1]);
	// }

	/**
	 * Obtain mappings object from training features file onle
	 * 
	 * @param trainFile
	 * @param mappingFile
	 * @return
	 * @throws IOException
	 */
	public static Mappings getMappingFromTrainingData(String trainFile, String mappingFile, boolean hasNA) throws IOException {

		Mappings m = new Mappings();
		// ensure that "NA" gets ID o
		if(hasNA) {
			m.getRelationID("NA", true);
		}
		System.out.println("Converting input feature file to temporary feature list file...");
		long start = System.currentTimeMillis();
		// grab feature from feature file and put in new file
		BufferedReader br = new BufferedReader(new FileReader(new File(trainFile)));
		File trainFileF = new File(trainFile);
		File tmpFeatureFile = new File(trainFileF.getParentFile().getAbsolutePath() + "/" + trainFileF.getName()
				+ "-tmpFeatureFile-" + new Random(System.nanoTime()).nextInt());
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFeatureFile));
		String nextLine;
		int count = 0;
		while ((nextLine = br.readLine()) != null) {

			String[] values = nextLine.split("\t");
			for (int i = 4; i < values.length; i++) {
				bw.write(values[i] + "\n");
			}
			count++;
			
			if (count % 100000 == 0) {
				System.out.println(count + " lines processed");
			}
		}
		br.close();
		bw.close();
		long end = System.currentTimeMillis();
		System.out.println("Converted feature file to temporary feature list file in " + (end - start)
				+ " milliseconds");

		System.out.println("Sorting temporary feature list file....");
		start = end;
		// sort new file by feature string
		File sortedTmpFeatureFile = new File(tmpFeatureFile.getParentFile().getAbsolutePath() + "/"
				+ trainFileF.getName() + "-sortedTmpFeatureFile-" + new Random(System.nanoTime()).nextInt());
		externalSort(tmpFeatureFile, sortedTmpFeatureFile, ExternalSort.defaultcomparator);
		// ExternalSort.sortAndSave(tmplist, cmp, cs, tmpdirectory)
		// ExternalSort.sort(tmpFeatureFile, sortedTmpFeatureFile);
		end = System.currentTimeMillis();
		System.out.println("Sorted temporary feature list file in " + (end - start) + " milliseconds");

		br = new BufferedReader(new FileReader(sortedTmpFeatureFile));

		String line;
		count = 0;
		String prevFeature = "";
		int prevCount = 0;
		System.out.println("Converting feature list to feature mapping object");
		boolean keyfeature = false;
		String feature = null;
		while ((line = br.readLine()) != null) {
			feature = line.trim();
			if (feature.equals(prevFeature)) {
				prevCount++;
			} else {
				if ((!keyfeature && prevCount >= NtronExperiment.MINTZ_FEATURE_THRESHOLD) || (keyfeature && prevCount >= NtronExperiment.KEYWORD_FEATURE_THRESHOLD)) {
					m.getFeatureID(prevFeature, true);
				}
				if (feature.split(":")[0].equals("key")) {
					keyfeature = true;
				} else {
					keyfeature = false;
				}

				prevFeature = feature;
				prevCount = 1;
			}

			count++;
			if (count % 100000 == 0) {
				System.out.println(count + " features processed");
			}
		}
		// For the last feature
		if ((!keyfeature && prevCount >=  NtronExperiment.MINTZ_FEATURE_THRESHOLD) || (keyfeature && prevCount >=  NtronExperiment.KEYWORD_FEATURE_THRESHOLD)) {
			m.getFeatureID(feature, true);
		}

		br.close();
		tmpFeatureFile.delete();
		sortedTmpFeatureFile.delete();
		return m;
	}

	public static void externalSort(File srcFile, File destFile, Comparator<String> comparator) throws IOException {

		File tmpDir = destFile.getParentFile();
		List<File> tmpFiles = ExternalSort.sortInBatch(srcFile, comparator, 4096, Charset.defaultCharset(), tmpDir,
				false);
		ExternalSort.mergeSortedFiles(tmpFiles, destFile, comparator);

	}

	public static List<Integer> convertFeaturesToIntegers(List<String> features, Mappings m) {

		List<Integer> intFeatures = new ArrayList<Integer>();

		for (String feature : features) {
			Integer intFeature = m.getFeatureID(feature, false);
			if (intFeature != -1) {
				intFeatures.add(intFeature);
			}
		}

		return intFeatures;
	}

	public static void printMemoryStatistics() {
		double freeMemory = Runtime.getRuntime().freeMemory() / GIGABYTE_DIVISOR;
		double allocatedMemory = Runtime.getRuntime().totalMemory() / GIGABYTE_DIVISOR;
		double maxMemory = Runtime.getRuntime().maxMemory() / GIGABYTE_DIVISOR;
		System.out.println("MAX MEMORY: " + maxMemory);
		System.out.println("ALLOCATED MEMORY: " + allocatedMemory);
		System.out.println("FREE MEMORY: " + freeMemory);
	}
}