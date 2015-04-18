/**
 * 
 */
package main.java.iitb.neo.autoeval;

import main.java.iitb.neo.NtronExperiment;

/**
 * Searches over a grid of hyperparameters to find the ones that perform the
 * best over the cross validation set
 * 
 * @author aman
 * 
 */
public class CrossValidate {

	public static void main(String[] args) throws Exception {
		NtronExperiment ntronExp = new NtronExperiment(args[0]);
		boolean finalAvgConfigs[] = { true, false };
		for (int iterations = 10; iterations <= 70; iterations++) {
			for (double regul = 0.1; regul < 1; regul += 0.1) {
				for (int topk = 1; topk <= 30; topk++) {
					for (double margin = 0.05; margin <= 0.6; margin += 0.5) {
						for (boolean finalAvgConfig : finalAvgConfigs) {
							//Do not update the feature cutoff thresholds at all
							ntronExp.updateHyperparams(iterations, regul, topk, margin, NtronExperiment.MINTZ_FEATURE_THRESHOLD,
									NtronExperiment.KEYWORD_FEATURE_THRESHOLD, finalAvgConfig);
						}
					}
				}
			}
		}
	}

}
