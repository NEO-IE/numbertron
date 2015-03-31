package main.java.iitb.neo.training.meta;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.washington.multirframework.multiralgorithm.Dataset;
import edu.washington.multirframework.multiralgorithm.MILDocument;
import main.java.iitb.neo.training.ds.LRGraph;

public class LRGraphMemoryDataset implements Dataset<LRGraph> {

	private LRGraph[] docs;
	private int cursor = 0;
	
	public LRGraphMemoryDataset() {  }
	
	public LRGraphMemoryDataset(String file) 
		throws IOException {
		LRGraph d = new LRGraph();
		List<LRGraph> l = new ArrayList<LRGraph>();
		DataInputStream dis = new DataInputStream(new BufferedInputStream
				(new FileInputStream(file)));
		while (d.read(dis)) {
			l.add(d);
		
			d = new LRGraph();
		}
		dis.close();
		docs = l.toArray(new LRGraph[0]);
	}
	
	public int numDocs() { return docs.length; }

	public void shuffle(Random random) {
		for (int i=0; i < docs.length; i++) {
			// pick element that we want to swap with
			int e = i + random.nextInt(docs.length - i);
			LRGraph tmp = docs[e];
			docs[e] = docs[i];
			docs[i] = tmp;
		}
	}

	public LRGraph next() { 
		if (cursor < docs.length) 
			return docs[cursor++]; 
		else return null;
	}

	public boolean next(LRGraph doc) {
		if (cursor < docs.length) {
			LRGraph d = docs[cursor++];
			doc.location = d.location;
			doc.relation = d.relation;
			doc.features = d.features;
			doc.mentionIDs = d.mentionIDs;
			doc.relNumber = d.relNumber;
			doc.numMentions = d.numMentions;
			doc.n = d.n;
			doc.Z = d.Z;
			
			doc.N = d.N;
			doc.numMentionIDs = d.numMentionIDs;
			doc.numFeatures = d.numFeatures;
			doc.numNodesCount = d.numNodesCount;
			
			return true;
		}
		return false;
	}
	
//	public void orderByMentionCount(){
//		
//		List<MILDocument> positiveInstances = new ArrayList<MILDocument>();
//		List<MILDocument> negativeInstances = new ArrayList<MILDocument>();
//		
//		for(LRGraph md: docs){
//			if(md.Y.length == 0){
//				negativeInstances.add(md);
//			}
//			else{
//				positiveInstances.add(md);
//			}
//		}
//		System.out.println("positive isntances = " + positiveInstances.size());
//		System.out.println("negative instances = " + negativeInstances.size());
//
//		Collections.sort(positiveInstances, new Comparator<MILDocument>(){
//			@Override
//			public int compare(MILDocument o1, MILDocument o2) {
//				
//				if(o1.numMentions < 5 && o2.numMentions >= 5){
//					return 1;
//				}
//				
//				else if(o1.numMentions >= 5 && o2.numMentions <5 ){
//					return -1;
//				}
//				
//				else if(o1.numMentions<5 && o2.numMentions <5){
//					
//					int diff = o2.numMentions - o1.numMentions;
//					if(diff != 0){
//						return diff;
//					}
//					else{
//						return (o1.arg1+o1.arg2).compareTo(o2.arg1+o2.arg2);
//					}
//				}
//				
//				//bot numMentions are >= 5
//				else{
//					
//					int diff = o1.numMentions - o2.numMentions;
//					if(diff != 0){
//						return diff;
//					}
//					else{
//						return (o1.arg1+o1.arg2).compareTo(o2.arg1+o2.arg2);
//					}
//				}
//				
//			}
//		});
//		
//		Collections.sort(negativeInstances,new Comparator<MILDocument>(){
//			@Override
//			public int compare(MILDocument o1, MILDocument o2) {
//				
//				int diff = o1.numMentions - o2.numMentions;
//				if(diff != 0){
//					return diff;
//				}
//				else{
//					return (o1.arg1+o1.arg2).compareTo(o2.arg1+o2.arg2);
//				}
//				
//			}
//		});
//		
////		int ratio = (int)Math.floor((double)negativeInstances.size()/(double)positiveInstances.size());
////		int posIndex =0;
////		int negIndex =0;
////		for(int i =0; i < docs.length; i++){
////			
////			if( i % ratio == 0){
////				if(posIndex < positiveInstances.size()){
////					docs[i] = positiveInstances.get(posIndex);
////					posIndex++;
////				}
////				else{
////					docs[i] = negativeInstances.get(negIndex);
////					negIndex++;
////				}
////			}
////			else{
////				
////				if(negIndex < negativeInstances.size()){
////					docs[i] = negativeInstances.get(negIndex);
////					negIndex++;
////				}
////				else{
////					docs[i] = positiveInstances.get(posIndex);
////					posIndex++;
////				}
////			}
////		}
//		
//		int posIndex =0;
//		int negIndex =0;
//		for(int i =0; i < docs.length; i++){
//			if(i % 2 == 0){
//				if(posIndex < positiveInstances.size()){
//					docs[i] = positiveInstances.get(posIndex);
//					posIndex++;
//				}
//				else{
//					docs[i] = negativeInstances.get(negIndex);
//					negIndex++;
//				}
//			}
//			else{
//				if(negIndex < negativeInstances.size()){
//					docs[i] = negativeInstances.get(negIndex);
//					negIndex++;
//				}
//				else{
//					docs[i] = positiveInstances.get(posIndex);
//					posIndex++;
//				}
//			}
//		}
//		
//		
//	}

	public void reset() {
		cursor = 0;
	}

	
}
