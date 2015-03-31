package main.java.iitb.neo.goldDB;

import it.unimi.dsi.fastutil.Hash;

import java.util.ArrayList;
import java.util.HashSet;

import scala.actors.IScheduler;

/**
 * This class iterates over a knowledge base and determines which of the
 * location relations are confused. Confused: For a given location, relations r1
 * and r2 are said to be confused if r1 and r2 have the same units and the range
 * of true values of r1 and r2 are similar
 * 
 * @author aman
 * 
 */
public class ConfusedLocationUnits {

	public static final GoldDB gdb = new GoldDB();

	/**
	 * checks if a = b +- k*b
	 * 
	 * @param a
	 * @param b
	 * @param k
	 *            , a fraction
	 * @return
	 */
	boolean withinKPercent(double a, double b, double k) {
		assert (k <= 1 && k >= 0);
		return (a >= b - b * k && a <= b + b * k);
	}

	double sum(ArrayList<Double> list) {
		double res = 0;
		for (Double d : list) {
			res += d;
		}
		return res;
	}

	double mean(ArrayList<Double> list) {
		assert (list.size() > 0);
		return sum(list) / list.size();
	}

	/**
	 * Determines whether rel1 and rel2 for the given country can be confused
	 * 
	 * @param country
	 * @param rel1
	 * @param rel2
	 * @return
	 */
	boolean isConfused(String country, String rel1, String rel2) {
		ArrayList<Double> rel1Vals = gdb.getGoldDBValue(country, rel1);
		ArrayList<Double> rel2Vals = gdb.getGoldDBValue(country, rel2);
		
		if(null == rel1Vals || null == rel2Vals) {
			return false;
		}
		double closeness_criteria = 0.20;
		return withinKPercent(mean(rel1Vals), mean(rel2Vals), closeness_criteria);
	}

	public static void main(String args[]) {
		String rel1 = "INTERNET";
		String rel2 = "INF";
		ConfusedLocationUnits crel = new ConfusedLocationUnits();
		HashSet<String> countries = crel.gdb.getCountries();
		for (String country : countries) {
			if (crel.isConfused(country, rel1, rel2)) {
				System.out.println(country);
			}
		}
	}

}
