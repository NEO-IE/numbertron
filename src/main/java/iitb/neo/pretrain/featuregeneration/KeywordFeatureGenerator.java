package main.java.iitb.neo.pretrain.featuregeneration;

import iitb.rbased.meta.KeywordData;
import iitb.rbased.meta.RelationMetadata;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.corpus.SentDependencyInformation.DependencyAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetEndAnnotation;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;

/**
 * @author ashishm
 * 
 */

/**
 */
public class KeywordFeatureGenerator implements FeatureGenerator {
	
	
	@Override
	public List<String> generateFeatures(Integer arg1StartOffset,
			Integer arg1EndOffset, Integer arg2StartOffset,
			Integer arg2EndOffset, String arg1ID, String arg2ID, CoreMap sentence, Annotation document) {
		
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		//System.out.println(tokens);
	//	System.out.println("arg1 id : " + arg1ID + " arg2 id : " + arg2ID);
		//initialize arguments
		String[] tokenStrings = new String[tokens.size()];
		String[] posTags = new String[tokens.size()];
		
		//initialize dependency parents to -1
		int[] depParents = new int[tokens.size()];
		for(int i = 0; i < depParents.length; i ++){
			depParents[i] = -1;
		}
		
		String[] depTypes = new String[tokens.size()];
		String arg1ner = "";
		String arg2ner = "";
		int[] arg1Pos = new int[2];
		int[] arg2Pos = new int[2];

		//iterate over tokens
		for(int i =0; i < tokens.size(); i++){
			
			CoreLabel token = tokens.get(i);
		
			//set the tokenString value
			tokenStrings[i] =token.value();
			
			//set the pos value
			String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
			if(pos == null){
				posTags[i] = "";
			}
			else{
				posTags[i] = pos;
			}
			
			int begOffset = token.get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
			int endOffset = token.get(SentenceRelativeCharacterOffsetEndAnnotation.class);
			//System.out.println(token + " " + begOffset + ":" + endOffset);
			// if the token matches the argument set the ner and argPos values
			if(begOffset == arg1StartOffset){
				String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
				if(ner != null){
					arg1ner = ner;
				}
				arg1Pos[0] = i;
			}
			
			if(endOffset == arg1EndOffset){
				arg1Pos[1] = i;
			}	
			if(begOffset == arg2StartOffset){
				String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
				if(ner != null){
					arg2ner = ner;
				}
				arg2Pos[0] = i;
			}
			
			if(endOffset == arg2EndOffset){
				arg2Pos[1] = i;
			}
		}
		//dependency conversions..
		List<Triple<Integer,String,Integer>> dependencyData = sentence.get(DependencyAnnotation.class);
		if(dependencyData != null){
			for(Triple<Integer,String,Integer> dep : dependencyData){
				int parent = dep.first -1;
				String type = dep.second;
				int child = dep.third -1;
	
				//child and parent should not be equivalent
				if(parent == child){
					parent = -1;
				}
				
				if(child < tokens.size()){
					depParents[child] = parent;
					depTypes[child] = type;
				}
				else{
					System.err.println("ERROR BETWEEN DEPENDENCY PARSE AND TOKEN SIZE");
					return new ArrayList<String>();
				}
			}
		}
		else{
			return new ArrayList<String>();
		}
		//add 1 to end Pos values
		arg1Pos[1] += 1;
		arg2Pos[1] += 1;	
		
		List<String> features = new ArrayList<String>();
		for (int i = 0; i < tokenStrings.length; i++) {
			if (fixedKeywordContains(tokenStrings[i])) {
				features.add("key: " + tokenStrings[i]);
			}
		}
		return features;
	}
	
	boolean fixedKeywordContains(String token){
		String[] fixedKeywords = {"population", "people", "inhabitants", "natives", "residents", "people",
				"area", "land",
				"foreign", "fdi", "direct", "investments", "investment",
				"goods", "exports", "export", "exporter", "exported", "ships", "shipped",
				"electricity", "kilowatthors", "terawatt", "generation", "production", "sector",
				"carbon", "emission", "CO2", "co2", "emissions", "kilotons",
				"inflation", "Price", "Rise", "rate",
				"Internet", "users", "usage", "penetration", "use", "user",
				"Gross",  "domestic", "GDP", "gdp", "product",
				"life", "expectancy",
				"diesel", "price", "priced", "fuel", "prices"} ;
		for(String key: fixedKeywords){
			if(key.equals(token)){
				return true;
			}
		}
		return false;
	}
}
