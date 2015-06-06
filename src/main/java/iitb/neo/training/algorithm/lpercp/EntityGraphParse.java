package main.java.iitb.neo.training.algorithm.lpercp;

import main.java.iitb.neo.training.ds.EntityGraph;

/**
 * Stores the states of the variables, to be used during training
 * when the underlying data structure is the entity relation graph
 * @author aman
 *
 */
public class EntityGraphParse {
	public EntityGraph graph; //the underlying graph 
	public boolean [][] n_states; //state of the ns
	public int z_states[]; //state of the Zs
}
