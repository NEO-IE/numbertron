package main.java.iitb.neo.util;

public class MathUtils {
	/**
	 * 
	 * @param arg, the given value
	 * @param trueval
	 * @param margin, match margin
	 * @return whether arg is within margin % of the trueval
	 */
	public static boolean within(double arg, double trueval, double margin) {
		double upper = trueval + margin * trueval;
		double lower = trueval - margin * trueval;
		return (arg >= lower && arg <= upper);
	}
}
