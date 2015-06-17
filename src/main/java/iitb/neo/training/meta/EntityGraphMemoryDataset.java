package main.java.iitb.neo.training.meta;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import main.java.iitb.neo.training.ds.EntityGraph;
import edu.washington.multirframework.multiralgorithm.Dataset;

public class EntityGraphMemoryDataset implements Dataset<EntityGraph> {

	private EntityGraph[] docs;
	private int cursor = 0;
	
	public EntityGraphMemoryDataset() {  }
	
	public EntityGraphMemoryDataset(String file) 
		throws IOException {
		
		List<EntityGraph> egraphList = new ArrayList<EntityGraph>();
		DataInputStream dis = new DataInputStream(new BufferedInputStream
				(new FileInputStream(file)));
		EntityGraph egraph = new EntityGraph();
		while (egraph.read(dis)) {
			egraphList.add(egraph);
			egraph = new EntityGraph();
		}
		dis.close();
		docs = egraphList.toArray(new EntityGraph[0]);
	}
	
	public int numDocs() { return docs.length; }

	public void shuffle(Random random) {
		for (int i=0; i < docs.length; i++) {
			// pick element that we want to swap with
			int e = i + random.nextInt(docs.length - i);
			EntityGraph tmp = docs[e];
			docs[e] = docs[i];
			docs[i] = tmp;
		}
	}

	public EntityGraph next() { 
		if (cursor < docs.length) 
			return docs[cursor++]; 
		else return null;
	}

	public boolean next(EntityGraph doc) {
		if (cursor < docs.length) {
			EntityGraph d = docs[cursor++];
			doc.entity = d.entity;

			doc.numMentions = d.numMentions;
			doc.s = d.s;
		
			doc.numNodesCount = d.numNodesCount;
			doc.n = d.n;
			
			return true;
		}
		return false;
	}

	public void reset() {
		cursor = 0;
	}

	
}
