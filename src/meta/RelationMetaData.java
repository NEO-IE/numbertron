package meta;

import java.util.ArrayList;
import java.util.HashMap;

import scala.actors.threadpool.Arrays;

/**
 * Temporary fix to move the implementation to entity based rather than entity-location based
 * @author aman
 *
 */
@SuppressWarnings("unchecked")
public class RelationMetaData {
	public static String relationNames[] = {"AGL", "FDI", "GOODS", "ELEC", "CO2", "INF", "INTERNET", "LIFE", "POP", "GDP"};
	public static int NUM_RELATIONS = -1;
	public static HashMap<String, ArrayList<Integer> > unitRelationMap;
	static {
		NUM_RELATIONS = relationNames.length;
		ArrayList<Integer> perc = new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 1, 2}));
		ArrayList<Integer> usd = new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 3, 4, 5}));
		ArrayList<Integer> agl = new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 6}));
		ArrayList<Integer> life = new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 7}));
		ArrayList<Integer> pop = new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 8}));
		ArrayList<Integer> elec = new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 9}));
		ArrayList<Integer> co2 = new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 10}));
		unitRelationMap = new HashMap<>();
		unitRelationMap.put("PERC", perc);
		unitRelationMap.put("USD", usd);
		unitRelationMap.put("LIFE", life);
		unitRelationMap.put("CO2", co2);
		unitRelationMap.put("AGL", agl);
		unitRelationMap.put("POP", pop);
		unitRelationMap.put("ELEC", elec);
		
	}
	 
	
}
