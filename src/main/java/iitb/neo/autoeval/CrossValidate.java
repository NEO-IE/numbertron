/**
 * 
 */
package main.java.iitb.neo.autoeval;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;

import main.java.iitb.neo.NtronExperiment;
import main.java.iitb.neo.extract.ExtractFromCorpus;
import main.java.iitb.neo.util.JsonUtils;

/**
 * Searches over a grid of hyperparameters to find the ones that perform the
 * best over the cross validation set
 * 
 * @author aman
 * 
 */
public class CrossValidate {
	static class CVParams {
		public static final int 
				startItr = 10, 
				endItr = 1000, 
				mulItr = 2, 
				startTopk = 3,
				endTopk = 8,
				deltopK = 2;
		public static final double 
				startRegul = 0.1, 
				endRegul = 0.9, 
				delRegul = 0.15,
				startMargin = 0.2,
				endMargin = 0.5,
				delMargin = 0.1;
	}

	public static void main(String[] args) throws Exception {
		NtronExperiment ntronExp = new NtronExperiment(args[0]);
		ExtractFromCorpus efc = new ExtractFromCorpus(args[0]);
		EvaluateModel eval = new EvaluateModel(args[0]);

		Map<String, Object> properties = JsonUtils.getJsonMap(args[0]);

		String outFile = JsonUtils.getStringProperty(properties, "cvFile");

		PrintWriter pw = new PrintWriter(new FileWriter(outFile));
		boolean finalAvgConfigs[] = { true, false };

		for (int iterations = CVParams.startItr; iterations <= CVParams.endItr; iterations *= CVParams.mulItr) {
			for (double regul = CVParams.startRegul; regul <= CVParams.endRegul; regul += CVParams.delRegul) {
				for (int topk = CVParams.startTopk; topk <= CVParams.endTopk; topk += CVParams.deltopK) {
					for (double margin = CVParams.startMargin; margin <= CVParams.endMargin; margin += CVParams.delMargin) {
						for (boolean finalAvgConfig : finalAvgConfigs) {
							// Do not update the feature cutoff thresholds at
							// all
							// update hyperparams
							pw.write(configString(iterations, regul, topk, margin, finalAvgConfig) + "\n");
							pw.write("====================================");
							ntronExp.updateHyperparams(iterations, regul, topk, margin,
									NtronExperiment.MINTZ_FEATURE_THRESHOLD, NtronExperiment.KEYWORD_FEATURE_THRESHOLD,
									finalAvgConfig);
							ntronExp.run();
							eval.evaluate(efc, false, pw);
							pw.flush();
						}
					}
				}
			}
		}
		pw.close();
	}

	private static String configString(int iterations, double regul, int topk, double margin, boolean finalAvgConfig) {
		StringBuffer configStr = new StringBuffer();
		String SEP = "\t";
		configStr.append("iterations = " + iterations + SEP);
		configStr.append("Regularizer = " + regul + SEP);
		configStr.append("Top k = " + topk + SEP);
		configStr.append("Margin = " + margin + SEP);
		configStr.append("Averaging = " + finalAvgConfig + SEP);
		return configStr.toString();
	}

}
