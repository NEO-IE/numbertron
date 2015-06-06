package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.ArrayList;

import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.ds.Number;
import edu.washington.multirframework.multiralgorithm.Parameters;



/*
 * @author ashish
 */
public class ConditionalInferenceEntityGraph {
	public static EntityGraphParse infer(EntityGraph egraph, Scorer scorer, Parameters params) {
				
			EntityGraphParse p = new EntityGraphParse();
			p.graph = egraph;
			scorer.setParameters(params);
			
			Parse trueParse = GoldDbInference.infer(egraph); //get the states of n nodes from gold DB for this graph.
			//Parse trueParse = KeywordInference.infer(lrg);
			//Parse trueParse = GoldDBKeywordInference.infer(lrg);
			
			p.z_states = new int[lrg.Z.length];
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
