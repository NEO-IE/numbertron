package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.ArrayList;

import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.ds.Number;
import edu.washington.multirframework.multiralgorithm.Parameters;



/*
 * @author ashish
 */
public class ConditionalInference {
	public static Parse infer(LRGraph lrg, Scorer scorer, Parameters params) {
				
			Parse p = new Parse();
			p.graph = lrg;
			scorer.setParameters(params);
			
			//Parse trueParse = GoldDbInference.infer(lrg); //get the states of n nodes from gold DB for this graph.
			//Parse trueParse = KeywordInference.infer(lrg);
			Parse trueParse = GoldDBKeywordInference.infer(lrg);
			
			p.z_states = new boolean[lrg.Z.length];
			p.n_states = trueParse.n_states;
			
			for(int i = 0 ; i < lrg.n.length; i++){
				Number n = lrg.n[i];
				ArrayList<Integer> z_s = n.zs_linked;
				for(Integer z: z_s){
					p.z_states[z] = trueParse.n_states[i];  //z_s copy the state of n_s.
				}
			}
			return p;
	}
}
