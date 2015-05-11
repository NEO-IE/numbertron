package main.java.iitb.neo.util;

public class RandomUtils {
	public static boolean coinToss(double bias) {
		double toss = Math.random();
		return toss <= bias;
	}
}
