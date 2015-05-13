package main.java.iitb.neo.pretrain.featuregeneration;

import java.util.ArrayList;
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
//
//		if ((value == Math.floor(value)) && !Double.isInfinite(value)) {
//			features.add("Num: Integer");
//		}

		/*
		 * todo: add scientific notation feature
		 */

		return features;
	}

}
