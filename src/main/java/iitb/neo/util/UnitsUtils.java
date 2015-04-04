package main.java.iitb.neo.util;

import iitb.rbased.meta.RelationMetadata;
import iitb.shared.EntryWithScore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catalog.Unit;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multirframework.data.Argument;
import eval.UnitExtractor;

/**
 * Collection of unit specific utilities
 * Please add comments in verbose
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
	 * Find out all the relations that can possibly go with a particular relation
	 * 
	 * @param numArg The argument (a number)
	 * @param sentence
	 * @param map A map from relation name to relation number
	 * @return A list of relation numbers that can be possibly associated with this number. For example, if the Argument has
	 * percent as a unit, the returned list will have Internet and inflation. 
	 */
	public static ArrayList<Integer> unitsCompatible(Argument numArg, CoreMap sentence, Map<String, Integer> map) {
		
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

		ArrayList<Integer> validRelations = new ArrayList<Integer>();
		for (String rel : relations) {
			if (unitRelationMatch(rel, unit)) {
				validRelations.add(map.get(rel));
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

}
