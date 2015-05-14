	package main.java.iitb.neo.pretrain.featuregeneration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import main.java.iitb.neo.util.StemUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;

/**
 * @author ashishm
 * 
 */

/**
 */
public class KeywordFeatureGenerator implements FeatureGenerator {
	
	private static HashSet<String> keywords;
	static
	{
		keywords = new HashSet<>();
		String[] fixedKeywords = {"population", "people", "inhabitants", "natives", "residents", "people",
				"area", "land",
				"foreign", "fdi", "direct", "investments", "investment",
				"goods", "exports", "export",
				"electricity", "kilowatthors", "terawatt", "generation", "production", "sector",
				"carbon", "emission", "CO2", "co2", "emissions", "kilotons",
				"inflation",
				"internet", "penetration", 
				"gross",  "domestic", "gdp",
				"life", "expectancy",
				"diesel"} ;
		for(String key: fixedKeywords){
			keywords.add(StemUtils.getStemWord(key.toLowerCase()));
		}
		
	}
	
		
	@Override
	public List<String> generateFeatures(Integer arg1StartOffset,
			Integer arg1EndOffset, Integer arg2StartOffset,
			Integer arg2EndOffset, String arg1ID, String arg2ID, CoreMap sentence, Annotation document) {
		
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		List<String> features = new ArrayList<String>();
		
		for (CoreLabel token: tokens) {
			
			String tokenStr = StemUtils.getStemWord(token.toString().toLowerCase());
			if (keywords.contains(tokenStr)) {
				features.add("key: " + tokenStr);
			}
		}
		return features;
	}

}