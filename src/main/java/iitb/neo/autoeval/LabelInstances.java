package main.java.iitb.neo.autoeval;

import iitb.rbased.meta.KeywordData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import main.java.iitb.neo.goldDB.GoldDB;
import main.java.iitb.neo.training.algorithm.lpercp.GoldDbInference;
import main.java.iitb.neo.util.StemUtils;

/**
 * This class takes as input an instance file and for each instance, assigns a
 * label based on one of the labelings
 * 
 * @author aman
 * 
 */
public class LabelInstances {

	static boolean keywordLabel(String sent, String rel) {
		List<String> keywords = KeywordData.REL_KEYWORD_MAP.get(rel);
		String tokens[] = sent.split(" ");
		HashSet<String> keywordSet = new HashSet<String>();

		for (String keyword : keywords) {
			keywordSet.add(StemUtils.getStemWord(keyword.toLowerCase()));
		}
		for (String token : tokens) {
			if (keywordSet.contains(StemUtils.getStemWord(token.toLowerCase()))) {
				return true;
			}
		}
		return false;
	}

	static boolean distanceLabel(Double value, String rel, String entity,
			double margin) {
		return GoldDbInference.closeEnough(value, rel, entity, margin);
	}

	public static void main(String args[]) throws IOException {
		String instancesFile = "/mnt/a99/d0/aman/combined_all_instances.tsv";
		BufferedReader br = new BufferedReader(new FileReader(instancesFile));
		String instance = null;
		int k = 5;
		HashSet<String> confusedPercentCountries = new HashSet<>();
		String confusedPercentCountriesNames[] = { "/m/06s9y", "/m/04xn_",
				"/m/088vb", "/m/07tp2", "/m/07dvs", "/m/07f5x", "/m/06v36",
				"/m/06tw8", "/m/01n8qg", "/m/06dfg", "/m/05qkp", "/m/016zwt",
				"/m/05cc1", "/m/04tr1", "/m/04vjh", "/m/04w8f", "/m/04v09",
				"/m/04gqr", "/m/04hvw", "/m/01xbgx", "/m/019rg5", "/m/0d05q4",
				"/m/03gyl", "/m/036b_", "/m/02kcz", "/m/035dk", "/m/01nyl",
				"/m/0163v", "/m/07bxhl", "/m/0164v", "/m/01699", "/m/0162b",
				"/m/0j4b", "/m/0jdd", "/m/0166v", "/m/05sb1", "/m/03rk0" };

		for (String confusedPercentCountry : confusedPercentCountriesNames) {
			confusedPercentCountries.add(confusedPercentCountry);
		}
		HashSet<String> confusedGOODSGDPCountries = new HashSet<>();

		String confusedGOODSGDPNames[] = { "/m/01crd5", "/m/06vbd", "/m/06s_2",
				"/m/06sw9", "/m/0697s", "/m/05l8y", "/m/04tr1", "/m/04gqr",
				"/m/047yc", "/m/0d05q4", "/m/0163v", "/m/0167v", "/m/0j11",
				"/m/06npd" };
		for (String confusedGOODSGDP : confusedGOODSGDPNames) {
			confusedGOODSGDPCountries.add(confusedGOODSGDP);
		}

		HashSet<String> confusedFDIGOODSCountries = new HashSet<>();
		String confusedFDIGOODSNames[] = { "/m/02lx0", "/m/07dvs", "/m/06s_2",
				"/m/01n8qg", "/m/04wlh", "/m/04vs9", "/m/04v3q", "/m/04w8f",
				"/m/04hzj", "/m/047t_", "/m/0165b", "/m/0j11", "/m/04g61",
				"/m/03rj0" };
		for (String confusedFDIGOODSName : confusedFDIGOODSNames) {
			confusedFDIGOODSCountries.add(confusedFDIGOODSName);
		}
		/**
		 * Initialize the gold database
		 */
		double margin = 0.20;
		int goldTrue = 0, keywordTrue = 0;
		GoldDB.initializeGoldDB(
				"/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/kb-worldbank-SI.tsv",
				k, margin);
		
		int total = 0, agree = 0, goldNotKey = 0, keyNotGold = 0, totalFalse = 0;
		while ((instance = br.readLine()) != null) {
			String instanceParts[] = instance.split("\t");
			String sent = instanceParts[10];
			String rel = instanceParts[9];
			String entity = instanceParts[0];

			boolean ignore = confusedPercentCountries.contains(entity)
					&& (rel.equals("INTERNET") || rel.equals("INF"));
			ignore = ignore
					|| (confusedGOODSGDPCountries.contains(entity) && (rel
							.equals("GOODS") || rel.equals("GDP")));
			ignore = ignore
					|| (confusedFDIGOODSCountries.contains(entity) && (rel
							.equals("GOODS") || rel.equals("FDI")));
			if (ignore) {
				continue;
			}
			Double value = Double.parseDouble(instanceParts[7]);
			boolean golddbLabel = LabelInstances.distanceLabel(value, rel,
					entity, margin);
			boolean keywordLabel = LabelInstances.keywordLabel(sent, rel);
			if (golddbLabel) {
				System.out
						.println(rel + "\t" + entity + "\t" + value + "\t"
								+ sent + "\tG: " + golddbLabel + "\tK: "
								+ keywordLabel);
				goldTrue++;
			}
			if (keywordLabel) {
				keywordTrue++;
			}
			if (golddbLabel && !keywordLabel) {
				goldNotKey++;
			} else if (!golddbLabel && keywordLabel) {
				keyNotGold++;

			} else {
				agree++;
				if (!golddbLabel) {
					totalFalse++;
				}
			}
			total++;
		}
		System.out.println("Total = " + total + " Agreement on " + agree
				+ ",  " + ((agree * 100.0) / total) + "%" + "\nGold db True = "
				+ goldTrue + "\nKeyword True = " + keywordTrue
				+ "\nGold but not key  = " + goldNotKey
				+ "\nKey but not gold  = " + keyNotGold + "\nTotal False = "
				+ totalFalse);
		br.close();
	}
}