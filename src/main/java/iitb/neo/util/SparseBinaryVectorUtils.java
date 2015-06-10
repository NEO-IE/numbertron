package main.java.iitb.neo.util;

import java.util.Arrays;
import java.util.List;

import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;

public class SparseBinaryVectorUtils {
	public static SparseBinaryVector getSBVfromList(List<Integer> features) {
		SparseBinaryVector sv = new SparseBinaryVector();

		int[] fts = new int[features.size()];

		for (int i = 0; i < features.size(); i++)
			fts[i] = features.get(i);
		Arrays.sort(fts);
		int countUnique = 0;
		for (int i = 0; i < fts.length; i++)
			if (fts[i] != -1 && (i == 0 || fts[i - 1] != fts[i]))
				countUnique++;
		sv.num = countUnique;
		sv.ids = new int[countUnique];
		int pos = 0;
		for (int i = 0; i < fts.length; i++)
			if (fts[i] != -1 && (i == 0 || fts[i - 1] != fts[i]))
				sv.ids[pos++] = fts[i];

		return sv;
	}

}
