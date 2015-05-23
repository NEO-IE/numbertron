package main.java.iitb.neo.util;

import edu.stanford.nlp.util.Interval;

public class IntervalUtils {


	/**
	 * Finds the absolute value of the first interval to the second interval
	 * 
	 * @param intr1
	 * @param intr2
	 */
	public static int findIntervalDistance(Interval<Integer> intr1,
			Interval<Integer> intr2) {
		int order = intr1.compareIntervalOrder(intr2);
		if (order == -1) { // before
			return intr2.first() - intr1.second();
		} else if (order == 1) {
			return intr1.first() - intr2.second();
		} else {
			return -1;
		}
	}
}
