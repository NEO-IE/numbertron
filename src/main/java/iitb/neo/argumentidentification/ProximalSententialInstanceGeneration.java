package main.java.iitb.neo.argumentidentification;

import java.util.ArrayList;
import java.util.List;

import main.java.iitb.neo.util.IntervalUtils;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Interval;
import edu.stanford.nlp.util.Pair;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.data.Argument;

/**
 * Implements <code>SententialInstanceGeneration</code> method
 * <code>generateSententialInstances</code> by returning all pairs of arguments
 * where links are not the same.
 * 
 * @author jgilme1
 * 
 */
public class ProximalSententialInstanceGeneration implements
		SententialInstanceGeneration {

	private static ProximalSententialInstanceGeneration instance = null;

	public static ProximalSententialInstanceGeneration getInstance() {
		if (instance == null) {
			instance = new ProximalSententialInstanceGeneration();
		}
		return instance;
	}

	@Override
	public List<Pair<Argument, Argument>> generateSententialInstances(
			List<Argument> arguments, CoreMap sentence) {
		List<Pair<Argument, Argument>> sententialInstances = new ArrayList<>();

		for (int i = 0; i < arguments.size(); i++) {
			Argument arg1 = arguments.get(i);
			Interval<Integer> arg1Interval = Interval.toInterval(
					arg1.getStartOffset(), arg1.getEndOffset());
			Argument closestArg = null;
			int closestDist = 1000000;
			for (int j = i + 1; j < arguments.size(); j++) {
				if(i == j) {
					continue;
				}
				Argument arg2 = arguments.get(j);
				Interval<Integer> arg2Interval = Interval.toInterval(
						arg2.getStartOffset(), arg2.getEndOffset());
				int dist = IntervalUtils.findIntervalDistance(arg1Interval, arg2Interval);
				if (closestDist > dist) {
					closestDist = dist;
					closestArg = arg2;
				}

			}
			if (closestArg != null) {
				Pair<Argument, Argument> p = new Pair<>(arg1, closestArg);
				sententialInstances.add(p);
			}

		}
		return sententialInstances;
	}


}
