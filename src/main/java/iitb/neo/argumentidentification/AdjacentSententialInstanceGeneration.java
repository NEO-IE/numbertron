package main.java.iitb.neo.argumentidentification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.data.Argument;

/**
 * Returns the sentential instances that are adjacent to each other. Arg1 - Arg2
 * - Arg3 will return (Arg1, Arg2), (Arg2, Arg3)
 * 
 * @author aman
 * 
 */
public class AdjacentSententialInstanceGeneration implements
		SententialInstanceGeneration {
	private static AdjacentSententialInstanceGeneration instance = null;

	public static AdjacentSententialInstanceGeneration getInstance() {
		if (instance == null) {
			instance = new AdjacentSententialInstanceGeneration();
		}
		return instance;
	}

	@Override

	public List<Pair<Argument, Argument>> generateSententialInstances(
			List<Argument> arguments, CoreMap sentence) {
		HashSet<Pair<Argument, Argument>> sententialInstances = new HashSet<>();
		Collections.sort(arguments, new Comparator<Argument>() {

			@Override
			public int compare(Argument arg0, Argument arg1) {
				if (arg0.getEndOffset() < arg1.getEndOffset()) {
					return -1;
				} else if (arg1.getEndOffset() < arg0.getEndOffset()) {
					return 1;
				}
				return 0;
			}

		});
		int numArgs = arguments.size();
		// the first arg
		for (int i = 0; i < numArgs; i++) {
			Pair<Argument, Argument> p = null;
			if (i > 0) {
				p = new Pair<>(arguments.get(i - 1), arguments.get(i));
				sententialInstances.add(p);
				p = new Pair<>(arguments.get(i), arguments.get(i - 1));
				sententialInstances.add(p);
			}

			if (i < numArgs - 1) {
				p = new Pair<>(arguments.get(i + 1), arguments.get(i));
				sententialInstances.add(p);
				p = new Pair<>(arguments.get(i), arguments.get(i + 1));
				sententialInstances.add(p);
			}

		}
		
		ArrayList<Pair<Argument, Argument>> res = new ArrayList<Pair<Argument, Argument>>(sententialInstances);
		return res;
	}

}
