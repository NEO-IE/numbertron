package meta;
/**
 * Temporary fix to move the implementation to entity based rather than entity-location based
 * @author aman
 *
 */
public class RelationMetaData {
	public static String relationNames[] = {"AGL", "FDI", "GOODS", "ELEC", "CO2", "INF", "INTERNET", "LIFE", "POP", "GDP"};
	public static int NUM_RELATIONS = -1;
	static {
		NUM_RELATIONS = relationNames.length;
		
	}
		
}
