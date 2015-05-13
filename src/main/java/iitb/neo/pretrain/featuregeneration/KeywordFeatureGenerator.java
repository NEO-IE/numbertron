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
	
//	@Override
//	public List<String> generateFeatures(Integer arg1StartOffset,
//			Integer arg1EndOffset, Integer arg2StartOffset,
//			Integer arg2EndOffset, String arg1ID, String arg2ID, CoreMap sentence, Annotation document) {
//		
//		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
//		//System.out.println(tokens);
//	//	System.out.println("arg1 id : " + arg1ID + " arg2 id : " + arg2ID);
//		//initialize arguments
//		String[] tokenStrings = new String[tokens.size()];
//		String[] posTags = new String[tokens.size()];
//		
//		//initialize dependency parents to -1
//		int[] depParents = new int[tokens.size()];
//		for(int i = 0; i < depParents.length; i ++){
//			depParents[i] = -1;
//		}
//		
//		String[] depTypes = new String[tokens.size()];
//		String arg1ner = "";
//		String arg2ner = "";
//		int[] arg1Pos = new int[2];
//		int[] arg2Pos = new int[2];
//
//		//iterate over tokens
//		for(int i =0; i < tokens.size(); i++){
//			
//			CoreLabel token = tokens.get(i);
//		
//			//set the tokenString value
//			tokenStrings[i] =token.value();
//			
//			//set the pos value
//			String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
//			if(pos == null){
//				posTags[i] = "";
//			}
//			else{
//				posTags[i] = pos;
//			}
//			
//			int begOffset = token.get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
//			int endOffset = token.get(SentenceRelativeCharacterOffsetEndAnnotation.class);
//			//System.out.println(token + " " + begOffset + ":" + endOffset);
//			// if the token matches the argument set the ner and argPos values
//			if(begOffset == arg1StartOffset){
//				String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
//				if(ner != null){
//					arg1ner = ner;
//				}
//				arg1Pos[0] = i;
//			}
//			
//			if(endOffset == arg1EndOffset){
//				arg1Pos[1] = i;
//			}	
//			if(begOffset == arg2StartOffset){
//				String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
//				if(ner != null){
//					arg2ner = ner;
//				}
//				arg2Pos[0] = i;
//			}
//			
//			if(endOffset == arg2EndOffset){
//				arg2Pos[1] = i;
//			}
//		}
//		//dependency conversions..
//		List<Triple<Integer,String,Integer>> dependencyData = sentence.get(DependencyAnnotation.class);
//		if(dependencyData != null){
//			for(Triple<Integer,String,Integer> dep : dependencyData){
//				int parent = dep.first -1;
//				String type = dep.second;
//				int child = dep.third -1;
//	
//				//child and parent should not be equivalent
//				if(parent == child){
//					parent = -1;
//				}
//				
//				if(child < tokens.size()){
//					depParents[child] = parent;
//					depTypes[child] = type;
//				}
//				else{
//					System.err.println("ERROR BETWEEN DEPENDENCY PARSE AND TOKEN SIZE");
//					return new ArrayList<String>();
//				}
//			}
//		}
//		else{
//			return new ArrayList<String>();
//		}
//		//add 1 to end Pos values
//		arg1Pos[1] += 1;
//		arg2Pos[1] += 1;	
//		
//		return getDepKeywordFeatures(tokenStrings, posTags, depParents, depTypes, arg1Pos, arg2Pos, arg1ner, arg2ner);
//	}
//	
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

	public List<String> getSentKeywordFeatures(String[] tokens, 
			String[] postags,
			int[] depParents, String[] depTypes,
			int[] arg1Pos, int[] arg2Pos, String arg1ner, String arg2ner){
		
			/*
			 * creates keyword features for words in sentence.
			 */
		
			List<String> features = new ArrayList<String>();
			
			for (String token: tokens) {
				if (keywords.contains(token)) {
					features.add("key: " + token);
				}
			}
			return features;
	}
	
	public  List<String> getDepKeywordFeatures( String[] tokens, 
			String[] postags,
			int[] depParents, String[] depTypes,
			int[] arg1Pos, int[] arg2Pos, String arg1ner, String arg2ner) {

		/*
		 * create keyword features for words in dependency path. 
		 */
		
		List<String> features = new ArrayList<String>();

		
		// dependency features
		if (depParents == null || depParents.length < tokens.length) return features;
		
		// identify head words of arg1 and arg2
		// (start at end, while inside entity, jump)
		int head1 = arg1Pos[1]-1;
		int loopIterationCount =0;
		while (depParents[head1] >= arg1Pos[0] && depParents[head1] < arg1Pos[1]) {
			  head1 = depParents[head1];
			  //avoid infinite loop
			  if(loopIterationCount == 100){
				  break;
			  }
			  loopIterationCount++;
		}
		int head2 = arg2Pos[1]-1;
		//System.out.println(head1 + " " + head2);
		loopIterationCount =0;
		while (depParents[head2] >= arg2Pos[0] && depParents[head2] < arg2Pos[1]) {
			head2 = depParents[head2];
			//avoid infinite loop
			  if(loopIterationCount == 100){
				  break;
			  }
			  loopIterationCount++;
		}

		
		
		// find path of dependencies from first to second
		int[] path1 = new int[tokens.length];
		for (int i=0; i < path1.length; i++) path1[i] = -1;
		path1[0] = head1; // last token of first argument
		for (int i=1; i < path1.length; i++) {
			path1[i] = depParents[path1[i-1]];
			if (path1[i] == -1) break;
		}	
		int[] path2 = new int[tokens.length];
		for (int i=0; i < path2.length; i++) path2[i] = -1;
		path2[0] = head2; // last token of first argument
		for (int i=1; i < path2.length; i++) {
			path2[i] = depParents[path2[i-1]];
			if (path2[i] == -1) break;
		}
		int lca = -1;
		int lcaUp = 0, lcaDown = 0;
		outer:
		for (int i=0; i < path1.length; i++)
			for (int j=0; j < path2.length; j++) {
				if (path1[i] == -1 || path2[j] == -1) {
					break; // no path
				}
				if (path1[i] == path2[j]) {
					lca = path1[i];
					lcaUp = i;
					lcaDown = j;
					break outer;
				}
			}
		
		if (lca < 0) return features; // no dependency path (shouldn't happen)

		//ArrayList<String> keywords = new ArrayList<String>();
		
		if (lcaUp + lcaDown < 12) {
			
			for (int i=0; i < lcaUp; i++) {
				/*
				 * adding keywords as features.
				 * Intuition: NN phrases in dependency path form good keywords.
				 */
				if(i > 0 && keywords.contains(tokens[path1[i]])){
					features.add("key: "+tokens[path1[i]]);
				}	
			}
			for (int j=0; j < lcaDown; j++) {
				/*
				 * adding keywords as features
				 */
				if(lcaUp + j > 0  && keywords.contains(tokens[path2[lcaDown-j]])){
					features.add("key: "+tokens[path2[lcaDown-j]]);
				}
			}
		}	
		return features;
	}
}