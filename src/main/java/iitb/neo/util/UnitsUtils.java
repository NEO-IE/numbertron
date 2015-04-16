package main.java.iitb.neo.util;

import iitb.rbased.meta.RelationMetadata;
import iitb.rbased.util.Number;
import iitb.shared.EntryWithScore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catalog.QuantityCatalog;
import catalog.Unit;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multirframework.corpus.SentOffsetInformation.SentStartOffset;
import edu.washington.multirframework.data.Argument;
import eval.UnitExtractor;

/**
 * Collection of unit specific utilities Please add comments in verbose
 * 
 * @author aman
 * 
 */
public class UnitsUtils {
	public static Set<String> relations;
	private static UnitExtractor ue;
	static {
		relations = RelationMetadata.getRelations();
		try {
			ue = new UnitExtractor();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Find out all the relations that can possibly go with a particular
	 * relation
	 * 
	 * @param numArg
	 *            The argument (a number)
	 * @param sentence
	 * @param map
	 *            A map from relation name to relation number
	 * @return A list of relation numbers that can be possibly associated with
	 *         this number. For example, if the Argument has percent as a unit,
	 *         the returned list will have Internet and inflation.
	 */
	public static ArrayList<Integer> unitsCompatible(Argument numArg, CoreMap sentence, Map<String, Integer> map) {
		ArrayList<String> validRelationsStrs = unitsCompatible(numArg, sentence);
		ArrayList<Integer> validRelations = new ArrayList<Integer>();
		for (String rel : validRelationsStrs) {
			validRelations.add(map.get(rel));
		}
		return validRelations;
	}

	/**
	 * Find out all the relations that can possibly go with a particular
	 * relation
	 * 
	 * @param numArg
	 *            The argument (a number)
	 * @param sentence
	 * @return A list of relation names that can be possibly associated with
	 *         this number. For example, if the Argument has percent as a unit,
	 *         the returned list will have INTERNET and INF.
	 */
	public static ArrayList<String> unitsCompatible(Argument numArg, CoreMap sentence) {

		String sentString = sentence.toString();
		String tokenStr = numArg.getArgName();
		String parts[] = tokenStr.split("\\s+");
		int beginIdx = sentString.indexOf(parts[0]);
		int endIdx = beginIdx + parts[0].length();

		String front = sentString.substring(0, beginIdx);
		if (front.length() > 20) {
			front = front.substring(front.length() - 20);
		}
		String back = sentString.substring(endIdx);
		if (back.length() > 20) {
			back = back.substring(0, 20);
		}
		String utString = front + "<b>" + parts[0] + "</b>" + back;
		float values[][] = new float[1][1];
		List<? extends EntryWithScore<Unit>> unitsS = ue.parser.getTopKUnitsValues(utString, "b", 1, 0, values);

		// check for unit here....

		String unit = "";
		if (unitsS != null && unitsS.size() > 0) {
			unit = unitsS.get(0).getKey().getBaseName();
		}

		ArrayList<String> validRelations = new ArrayList<String>();
		for (String rel : relations) {
			if (unitRelationMatch(rel, unit)) {
				validRelations.add(rel);
			}
		}
		return validRelations;
	}

	public static boolean unitRelationMatch(String rel, String unitStr) {
		Unit unit = UnitsUtils.ue.quantDict.getUnitFromBaseName(unitStr);
		if (unit != null && !unit.getBaseName().equals("")) {
			Unit SIUnit = unit.getParentQuantity().getCanonicalUnit();
			if (SIUnit != null && !RelationMetadata.getUnit(rel).equals(SIUnit.getBaseName()) || SIUnit == null
					&& !RelationMetadata.getUnit(rel).equals(unit.getBaseName())) {
				return false; // Incorrect unit, this cannot be the relation.

			}
		} else if (unit == null && !unitStr.equals("") && RelationMetadata.getUnit(rel).equals(unitStr)) { // for
																											// the
																											// cases
																											// where
																											// units
																											// are
																											// compound
																											// units.
			return true;
		} else {
			if (!RelationMetadata.getUnit(rel).equals("")) {
				return false; // this cannot be the correct relation.
			}
		}
		return true;
	}

	/**
	 * Returns the flattened value of a number given as an argument.
	 * Flattened: "1 million" is returned as 1000000
	 * @param sentence
	 * @param number
	 * @return
	 */
	public static String getFlatValString(CoreMap sentence, Argument number) {
		String sentString = sentence.toString();
		int docOffset = sentence.get(SentStartOffset.class);
		int beginIdx = number.getStartOffset() - docOffset;
		int endIdx = number.getEndOffset() - docOffset;
		String tokenStr = number.getArgName();
		float values[][] = new float[1][1];
		Float flatValue = Number.getDoubleValue(Unit.parseDecimalExpressionL(tokenStr)).floatValue();
		String front = sentString.substring(0, beginIdx);
		if (front.length() > 20) {
			front = front.substring(front.length() - 20);
		}
		String back = sentString.substring(endIdx);
		if (back.length() > 20) {
			back = back.substring(0, 20);
		}
		String utString = front + "<b>" + tokenStr + "</b>" + back;

		List<? extends EntryWithScore<Unit>> unitsS = ue.parser.getTopKUnitsValues(utString, "b", 1, 0, values);

		// check for unit here....
		if (unitsS != null && unitsS.size() > 0) {

			// setting the unit
			String curUnit = unitsS.get(0).getKey().getBaseName();

			// STORE FLATTENED VALUE;

			QuantityCatalog qu = ue.quantDict;

			String unit_parts[] = curUnit.split("\\["); // Looking
														// for
														// multiplier,
														// e.g, sq
														// km
														// [million],
														// [billion],
														// etc.
			Unit b_unit;
			Unit multiplier = null;
			if (unit_parts.length == 1) { // no multiplier
				b_unit = qu.getUnitFromBaseName(unit_parts[0]);
			} else {
				b_unit = qu.getUnitFromBaseName(unit_parts[0].trim());
				String mult = unit_parts[1].split("\\]")[0];
				multiplier = qu.getUnitFromBaseName(mult);
			}

			// flat the value and store it in num_val
			if (b_unit != null) {
				Unit SIUnit = b_unit.getParentQuantity().getCanonicalUnit();
				if (SIUnit != null) {
					boolean success[] = new boolean[1];
					flatValue = qu.convert(flatValue, b_unit, SIUnit, success);
				}
			}

			if (multiplier != null && multiplier.getParentQuantity() != null) {
				boolean success[] = new boolean[1];
				flatValue = qu.convert(flatValue.floatValue(), multiplier, multiplier.getParentQuantity()
						.getCanonicalUnit(), success);
			}

		}
		return new Double(flatValue).toString();

	}

}
