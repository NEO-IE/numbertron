package main.java.iitb.neo.training.algorithm.lpercp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import main.java.iitb.neo.pretrain.process.MakeEntityGraph;
import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.ds.Number;
import meta.RelationMetaData;
import edu.washington.multirframework.multiralgorithm.Mappings;


/**
 * This is not inference at all actually.
 * The results are cached in a map to be served as it is later.
 * @author aman
 *
 */
public class ConditionalInferenceEntityGraph {
	
	private static Map<String, EntityGraphParse> cache = new HashMap<>();
	public static EntityGraphParse infer(EntityGraph egraph) {
			EntityGraphParse p = null;
//			if((p = cache.get(egraph.entity)) != null) {
//				return p;
//			}
			p = new EntityGraphParse();
			p.graph = egraph;
			
			
			//EntityGraphParse trueParse = GoldDbInferenceEntityGraph.infer(egraph); //get the states of n nodes from gold DB for this graph.
			EntityGraphParse trueParse = KeywordInferenceEntityGraph.infer(egraph);
			//Parse trueParse = GoldDBKeywordInference.infer(lrg);
			
			p.z_states = new int[egraph.numMentions];
			p.n_states = trueParse.n_states;
			
			
			for(int i = 0 ; i < egraph.numNodesCount; i++){
				Number n = egraph.n[i];
				ArrayList<Integer> validIdx = RelationMetaData.unitRelationMap.get(n.unit);
				for(Integer r: validIdx) {
					ArrayList<Integer> z_s = n.zs_linked;
					if(trueParse.n_states[i][r]) {
						for(Integer z: z_s){
							p.z_states[z] = r;  //z_s copy the state of n_s.
						}
					}
					
				}
			}
			cache.put(egraph.entity, p);
			return p;
	}
}
