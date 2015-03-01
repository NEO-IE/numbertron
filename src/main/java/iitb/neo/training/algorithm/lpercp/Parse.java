package main.java.iitb.neo.training.algorithm.lpercp;

import main.java.iitb.neo.training.ds.LRGraph;

/**
 * Stores the states of the variables, to be used during training
 * @author aman
 *
 */
public class Parse {
	public LRGraph graph; //the underlying graph 
	public boolean n_states[]; //state of the ns
	public boolean z_states[]; //state of the Zs
}
