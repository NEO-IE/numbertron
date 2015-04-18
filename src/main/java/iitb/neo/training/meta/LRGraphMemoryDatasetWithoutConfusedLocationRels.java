package main.java.iitb.neo.training.meta;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import edu.washington.multirframework.multiralgorithm.Dataset;
import main.java.iitb.neo.training.ds.LRGraph;

public class LRGraphMemoryDatasetWithoutConfusedLocationRels implements Dataset<LRGraph> {

	private LRGraph[] docs;
	private int cursor = 0;
	private HashSet<String> confusedPercentCountries, confusedGOODSGDPCountries;

	public LRGraphMemoryDatasetWithoutConfusedLocationRels() {
	}

	public LRGraphMemoryDatasetWithoutConfusedLocationRels(String file) throws IOException {
		confusedPercentCountries = new HashSet<String>();
		confusedGOODSGDPCountries = new HashSet<String>();
		
		String confusedPercentCountriesNames[] = { "/m/06s9y", "/m/04xn_", "/m/088vb", "/m/07tp2", "/m/07dvs",
				"/m/07f5x", "/m/06v36", "/m/06tw8", "/m/01n8qg", "/m/06dfg", "/m/05qkp", "/m/016zwt", "/m/05cc1",
				"/m/04tr1", "/m/04vjh", "/m/04w8f", "/m/04v09", "/m/04gqr", "/m/04hvw", "/m/01xbgx", "/m/019rg5",
				"/m/0d05q4", "/m/03gyl", "/m/036b_", "/m/02kcz", "/m/035dk", "/m/01nyl", "/m/0163v", "/m/07bxhl",
				"/m/0164v", "/m/01699", "/m/0162b", "/m/0j4b", "/m/0jdd", "/m/0166v", "/m/05sb1", "/m/03rk0" };

		for (String confusedPercentCountry : confusedPercentCountriesNames) {
			confusedPercentCountries.add(confusedPercentCountry);
		}

		String confusedGOODSGDPNames[] = { "/m/01crd5", "/m/06vbd", "/m/06s_2", "/m/06sw9", "/m/0697s", "/m/05l8y",
				"/m/04tr1", "/m/04gqr", "/m/047yc", "/m/0d05q4", "/m/0163v", "/m/0167v", "/m/0j11", "/m/06npd" };
		for(String confusedGOODSGDP: confusedGOODSGDPNames) {
			confusedGOODSGDPCountries.add(confusedGOODSGDP);
		}
		
		LRGraph d = new LRGraph();
		List<LRGraph> l = new ArrayList<LRGraph>();
		DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		while (d.read(dis)) {
			boolean ignore = confusedPercentCountries.contains(d.location) && (d.relation.equals("INTERNET") || d.relation.equals("INF"));
			ignore = ignore || (confusedGOODSGDPCountries.contains(d.location) && (d.relation.equals("GOODS") || d.relation.equals("GDP"))); 
			if (!ignore) {
				l.add(d);
			} else {
				System.err.println("Ignoring with " + d.location + ", " + d.relation);
			}

			d = new LRGraph();
		}
		dis.close();
		docs = l.toArray(new LRGraph[0]);
	}

	public int numDocs() {
		return docs.length;
	}

	public void shuffle(Random random) {
		for (int i = 0; i < docs.length; i++) {
			// pick element that we want to swap with
			int e = i + random.nextInt(docs.length - i);
			LRGraph tmp = docs[e];
			docs[e] = docs[i];
			docs[i] = tmp;
		}
	}

	public LRGraph next() {
		if (cursor < docs.length)
			return docs[cursor++];
		else
			return null;
	}

	public boolean next(LRGraph doc) {
		if (cursor < docs.length) {
			LRGraph d = docs[cursor++];
			doc.location = d.location;
			doc.relation = d.relation;
			doc.features = d.features;
			doc.mentionIDs = d.mentionIDs;
			doc.relNumber = d.relNumber;
			doc.numMentions = d.numMentions;
			doc.n = d.n;
			doc.Z = d.Z;
			return true;
		}
		return false;
	}

	public void reset() {
		cursor = 0;
	}

}
