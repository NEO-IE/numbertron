package main.java.iitb.neo.pretrain.featuregeneration;

import edu.washington.multirframework.featuregeneration.FeatureGenerator;

public interface NumberFeatureGenerator extends FeatureGenerator {
	public void useKeywordAsFeature(boolean useKeywordFeatures);
}
