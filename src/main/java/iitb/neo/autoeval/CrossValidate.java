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
				endItr = 10, 
				mulItr = 2, 
				startTopk = 3,
				endTopk = 3,
				deltopK = 2;
		
		public static final double 
				startRegul = 0.85, 
				endRegul = 0.85, 
				delRegul = 0.15,
				startMargin = 0.3,
				endMargin = 0.3,
				delMargin = 0.1,
				w_start = 0.0,
				w_end = 1.0,
				w_margin = 0.1;
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
							
							for(double w_m = CVParams.w_end; w_m >= CVParams.w_start; w_m -= CVParams.w_margin ){
								for(double w_k = CVParams.w_start; w_k < CVParams.w_end; w_k += CVParams.w_margin ){
									for(double w_n = CVParams.w_start; w_n < CVParams.w_end; w_n += CVParams.w_margin ){
							
										// Do not update the feature cutoff thresholds at
										// all
										// update hyperparams
										System.out.println("Hyperparameters: "+w_m+"\t"+w_k+"\t"+w_n);
										pw.write(configString(iterations, regul, topk, margin, finalAvgConfig, w_m, w_k, w_n) + "\n");
										pw.write("====================================");
										ntronExp.updateHyperparams(iterations, regul, topk, margin,
												NtronExperiment.MINTZ_FEATURE_THRESHOLD, NtronExperiment.KEYWORD_FEATURE_THRESHOLD,
												finalAvgConfig);
										//ntronExp.run();
										eval.evaluate(efc, false, pw, w_m, w_k, w_n);
										pw.flush();
									}
								}
							}
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
	
	private static String configString(int iterations, double regul, int topk, double margin, boolean finalAvgConfig, double w_m, double w_k, double w_n) {
		StringBuffer configStr = new StringBuffer();
		String SEP = "\t";
		configStr.append("iterations = " + iterations + SEP);
		configStr.append("Regularizer = " + regul + SEP);
		configStr.append("Top k = " + topk + SEP);
		configStr.append("Margin = " + margin + SEP);
		configStr.append("Averaging = " + finalAvgConfig + SEP);
		configStr.append("W_mintz = " + w_m + SEP);
		configStr.append("W_keywords = " + w_k + SEP);
		configStr.append("W_numbers = " + w_n + SEP);
		return configStr.toString();
	}

}
