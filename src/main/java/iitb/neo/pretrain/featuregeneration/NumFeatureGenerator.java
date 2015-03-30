package main.java.iitb.neo.pretrain.featuregeneration;

import edu.washington.multirframework.featuregeneration.FeatureGenerator;

public interface NumFeatureGenerator extends FeatureGenerator {
	public void useKeywordAsFeature(boolean useKeywordFeatures);
}
