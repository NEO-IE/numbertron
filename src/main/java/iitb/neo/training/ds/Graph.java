package main.java.iitb.neo.training.ds;

import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;

public class Graph {
	public static final int MNT_CAPACITY = 1;

	public String entity;
	
	public int random = 0;
	

	
	// mentions of this entity pair
	public int numMentions = 0; //the number of z nodes in the graph
	public int[] mentionIDs;
	public int[] Z; //can take values from different relations as well as NA, this is the set S_e
	
	public SparseBinaryVector[] features;

	//mentions of the number

	
}
