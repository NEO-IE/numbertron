package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.ArrayList;

import main.java.iitb.neo.training.ds.LRGraph;

public class ORUtils {
	/**
	 * This method takes a parse P and flips the N nodes based on the values of
	 * the Z nodes via a simple OR
	 * 
	 * @param p
	 */
	public static void SoftOR(Parse p) {
		// now set the N nodes based on soft ORing of the Z nodes
		int numN = p.graph.n.length;
		LRGraph lrg = p.graph;
		for (int n_i = 0; n_i < numN; n_i++) {
			ArrayList<Integer> attachedZ = lrg.n[n_i].zs_linked;
			p.n_states[n_i] = false;
			for (Integer z : attachedZ) { // iterate over all the attached Z
				if (p.z_states[z]) { // if any of them is one, set the number //
										// node to 1
					p.n_states[n_i] = true;// (((trueAttachedZCount * 1.0) /
											// (totalZ)) >=
											// LEAST_Z_FLIPPED_COUNT);
					break;
				}
			}
		}
	}
	
	/**
	 * Also sets the N nodes based on the flippings of the Z nodes, but takes a threshold
	 * @param p
	 */
	public static void CountOR(Parse p) {
		
	}
}
