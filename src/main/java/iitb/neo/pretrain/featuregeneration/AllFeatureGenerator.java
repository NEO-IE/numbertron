package main.java.iitb.neo.pretrain.featuregeneration;

import java.util.List;

import edu.stanford.nlp.ie.pascal.AcronymModel.Feature;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;

public class AllFeatureGenerator implements FeatureGenerator {

	@Override
	public List<String> generateFeatures(Integer arg1StartOffset,
			Integer arg1EndOffset, Integer arg2StartOffset,
			Integer arg2EndOffset, String arg1Id, String arg2Id,
			CoreMap sentence, Annotation document) {
		// TODO Auto-generated method stub
		FeatureGenerator mintzFg = new MintzKeywordFeatureGenerator();
		FeatureGenerator keywordFg = new KeywordFeatureGenerator();
		FeatureGenerator numFg = new NumberFeatureGenerator();
		List<String> mintzFeatures = mintzFg.generateFeatures(arg1StartOffset, arg1EndOffset, arg2StartOffset, arg2EndOffset, arg1Id, arg2Id, sentence, document);
		mintzFeatures.addAll(keywordFg.generateFeatures(arg1StartOffset, arg1EndOffset, arg2StartOffset, arg2EndOffset, arg1Id, arg2Id, sentence, document));
		mintzFeatures.addAll(numFg.generateFeatures(arg1StartOffset, arg1EndOffset, arg2StartOffset, arg2EndOffset, arg1Id, arg2Id, sentence, document));
		return mintzFeatures;
		
	}

}
