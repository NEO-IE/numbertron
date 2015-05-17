package main.java.iitb.neo.util;

import java.util.regex.Pattern;

import edu.stanford.nlp.util.Pair;
import edu.washington.multirframework.data.Argument;

/**
 * A collection of common regexp util functions
 * @author aman
 *
 */
public class RegExpUtils {
	public static final Pattern yearPat = Pattern.compile("^19[56789]\\d|20[012345]\\d$");
	
	public static final String numPattern = ".*\\d.*";
	public static boolean exactlyOneNumber(Pair<Argument, Argument> p) {
		boolean firstHasNumber = p.first.getArgName().matches(numPattern);
		boolean secondHasNumber = p.second.getArgName().matches(numPattern);
		return firstHasNumber ^ secondHasNumber;
	}

	public static boolean secondNumber(Pair<Argument, Argument> p) {
		boolean secondHasNumber = p.second.getArgName().matches(numPattern);
		return secondHasNumber;
	}
	
	public static boolean isYear(String token) {
		return yearPat.matcher(token).matches();
	}
	
	public static boolean isNumber(Argument arg) {
		return arg.getArgName().matches(numPattern);
	}
	
}
