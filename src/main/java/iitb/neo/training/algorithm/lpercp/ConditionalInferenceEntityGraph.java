package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.ArrayList;

import main.java.iitb.neo.pretrain.process.MakeEntityGraph;
import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.ds.Number;
import meta.RelationMetaData;
import edu.washington.multirframework.multiralgorithm.Mappings;
import edu.washington.multirframework.multiralgorithm.Parameters;



/*
 * @author ashish
 */
public class ConditionalInferenceEntityGraph {
	public static EntityGraphParse infer(EntityGraph egraph, Scorer scorer, Parameters params) {
				
			EntityGraphParse p = new EntityGraphParse();
			p.graph = egraph;
			scorer.setParameters(params);
			
			EntityGraphParse trueParse = GoldDbInferenceEntityGraph.infer(egraph); //get the states of n nodes from gold DB for this graph.
			//Parse trueParse = KeywordInference.infer(lrg);
			//Parse trueParse = GoldDBKeywordInference.infer(lrg);
			
			p.z_states = new int[egraph.Z.length];
			p.n_states = trueParse.n_states;
			
			Mappings m = MakeEntityGraph.mapping;
			for(int i = 0 ; i < egraph.numNodesCount; i++){
				for(int r = 0; r <= RelationMetaData.NUM_RELATIONS; r++) {
					Number n = egraph.n[i][r];
					ArrayList<Integer> z_s = n.zs_linked;
					if(trueParse.n_states[i][r]) {
						for(Integer z: z_s){
							
							p.z_states[z] = r;  //z_s copy the state of n_s.
						}
					}
					
				}
				
				
				
			}
			return p;
	}
}