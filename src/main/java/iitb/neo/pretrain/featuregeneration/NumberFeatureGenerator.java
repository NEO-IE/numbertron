package main.java.iitb.neo.pretrain.featuregeneration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import main.java.iitb.neo.training.ds.Number;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;

/**
 * A class that generates the number features
 * 
 * @author ashish
 * 
 */
public class NumberFeatureGenerator implements FeatureGenerator {

	
	public static int NumCountThreshold = 5;
	
	public static String instanceFile = "data/train/instances_number.tsv";
	
	
	static HashSet<Double> commonNumber;
	
	static{
		commonNumber = new HashSet<Double>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(instanceFile));
			
			String line;
			boolean flag = true;
			Double prev = 0.0;
			Double current = 0.0;
			int count = 0;
			while ((line = br.readLine()) != null) {
				
				String parts[] = line.split("\t");
				current = Double.parseDouble(parts[4]);
				if(flag){ //first time
					prev = current;
					count ++;
					flag = false;
					continue;
				} 
				
				if(prev == current){
					count ++;
				}else{
					if(count >= NumCountThreshold){
						commonNumber.add(current);
					}
					prev = current;
					count = 1;
				}
			}
			if(count >= NumCountThreshold){
				commonNumber.add(current);
			}
			
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	/**
	 * 
	 */
	@Override
	public List<String> generateFeatures(Integer arg1StartOffset,
			Integer arg1EndOffset, Integer arg2StartOffset,
			Integer arg2EndOffset, String arg1ID, String numberStringVal, CoreMap sentence, Annotation document) {

		// List<CoreLabel> tokens =
		// sentence.get(CoreAnnotations.TokensAnnotation.class);
		List<String> features = new ArrayList<String>();
		Double value = Number.getDoubleValue(numberStringVal);
		if (value == null) {
			return features; // no valid number could be formed of this string.
		}

		if (value < 0) {
			features.add("Num: Neg");
		}

		if (value > 0 && value < 1) {
			features.add("Num: Bet0And1");
		} else if (value == 1) {
			features.add("Num: One");
		} else if (value > 1 && value < 10) {
			features.add("Num: Units");
		} else if (value >= 10 && value < 100) {
			features.add("Num: Tens");
		} else if (value >= 100 && value < 1000) {
			features.add("Num: Hundreds");
		} else if (value >= 1000 && value < 10000) {
			features.add("Num: Thousands");
		} else if (value >= 10000 && value < 100000) {
			features.add("Num: Ten Thousands");
		} else if (value >= 100000 && value < 1000000) {
			features.add("Num: Lac");
		} else if (value >= 1000000 && value < 1000000000) {
			features.add("Num: Million");
		} else if (value >= 1000000000 && value < 1000000000000.0) {
			features.add("Num: Billion");
		} else if (value >= 1000000000000.0) {
			features.add("Num: Tera");
		}

		if ((value == Math.floor(value)) && !Double.isInfinite(value)) {
			features.add("Num: Integer");
		}

		/*
		 * TF based feature
		 */
		if(commonNumber.contains(value)){
			features.add("Num: isCommon");
		}
			
		
		return features;
	}

}
