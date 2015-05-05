package main.java.iitb.neo.training.ds;
/**
 * Represents the Location Relation Graph
 * @author aman
 *
 */

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;



/*
 * Adapted from MultiR. Each location relation forms an LRGraph.
 * Copied from original: 
 * 
 * "The purpose of this data structure is to keep all relevant information for
 * learning while using as little memory as possible. Using less memory helps
 * keeping more records in memory at the same time, thus improving speed."
 * 
 * TODO: Move the list of Z nodes connected to a number node to the 
 * SparseBinaryRepresentation
 */
public class LRGraph {
	public static final int MNT_CAPACITY = 1;
	public static final int NUM_RELS = 11;
	public String location, relation;
	public int relNumber;
	public int random = 0;
	

	
	// mentions of this entity pair
	public int numMentions = 0;
	public int[] mentionIDs;
	public int[] sentenceIDs;
	public int[] Z;
	public Number[] n;
	public SparseBinaryVector[] features;

	//mentions of the number
	public int[] N;
	public int numNodesCount = 0;
	public int[] numMentionIDs;
	public SparseBinaryVector[] numFeatures;
	
	public LRGraph() {
		mentionIDs = new int[MNT_CAPACITY];
		Z = new int[MNT_CAPACITY];
		sentenceIDs = new int[MNT_CAPACITY];
		features = new SparseBinaryVector[MNT_CAPACITY];
		
		numFeatures = new SparseBinaryVector[MNT_CAPACITY];
		numMentionIDs = new int[MNT_CAPACITY];
		N = new int[MNT_CAPACITY];
	}
	
	public void clear() {
		numMentions = 0;
		numNodesCount = 0;
	}
	
	public void setCapacity(int targetSize) {
		int[] newMentionIDs = new int[targetSize];
		int[] newSentenceIDs = new int[targetSize];
		int[] newZ = new int[targetSize];
		SparseBinaryVector[] newFeatures = new SparseBinaryVector[targetSize];
		if (numMentions > 0) {
			System.arraycopy(sentenceIDs, 0, newSentenceIDs, 0, numMentions);
			System.arraycopy(mentionIDs, 0, newMentionIDs, 0, numMentions);
			System.arraycopy(Z, 0, newZ, 0, numMentions);
			System.arraycopy(features, 0, newFeatures, 0, numMentions);
		}
		mentionIDs = newMentionIDs;
		Z = newZ;
		features = newFeatures;
		sentenceIDs = newSentenceIDs;
	}
	
	
	public void setNumCapacity(int targetSize){
		int[] newNumMentionIDs = new int[targetSize];
		int[] newN = new int[targetSize];
		SparseBinaryVector[] newNumFeatures = new SparseBinaryVector[targetSize];
		if(numNodesCount > 0){
			System.arraycopy(numMentionIDs, 0, newNumMentionIDs, 0, numNodesCount);
			System.arraycopy(N, 0, newN, 0, numNodesCount);
			System.arraycopy(numFeatures, 0, newNumFeatures, 0, numNodesCount);
		}
		numMentionIDs = newNumMentionIDs;
		N = newN;
		numFeatures = newNumFeatures;
	}
	
	public boolean read(DataInputStream dis) throws IOException {
		try {
			random = dis.readInt();
			location = dis.readUTF();
			relation = dis.readUTF();
			relNumber = dis.readInt();
		
			int lenN = dis.readInt();
			
			n = new Number[lenN];
			for (int i=0; i < lenN; i++) {
				n[i] = new Number();
				n[i].deserialize(dis);
			};
			
			if(lenN > numMentionIDs.length) setNumCapacity(lenN);
			this.numNodesCount = lenN;
			
			for(int i = 0; i < numNodesCount; i++){
				numMentionIDs[i] = dis.readInt();
				N[i] = dis.readInt();
				if(numFeatures[i] == null){
					numFeatures[i] = new SparseBinaryVector();
				}
				numFeatures[i].deserialize(dis);
			}
			
			int numMentions = dis.readInt();
			if (numMentions > mentionIDs.length) setCapacity(numMentions);
			this.numMentions = numMentions;
			for (int i=0; i < numMentions; i++) {
				mentionIDs[i] = dis.readInt();
				sentenceIDs[i] = dis.readInt();
				Z[i] = dis.readInt();
				if (features[i] == null) features[i] = new SparseBinaryVector();
				features[i].deserialize(dis);
			}
			
			//location = relation = null;
			//mentionIDs = null;
			
			return true;
		} catch (EOFException e) { return false; }
	}
	
	public void write(DataOutputStream dos) throws IOException {
		dos.writeInt(random);
		dos.writeUTF(location);
		dos.writeUTF(relation);
		dos.writeInt(relNumber);
		dos.writeInt(n.length);
		
		for (int i=0; i < n.length; i++)
			n[i].serialize(dos);
		
		for(int i = 0; i < numNodesCount; i++){
			dos.writeInt(numMentionIDs[i]);
			dos.writeInt(N[i]);
			numFeatures[i].serialize(dos);
		}
		
		dos.writeInt(numMentions);
		for (int i=0; i < numMentions; i++) {
			dos.writeInt(mentionIDs[i]);
			dos.writeInt(sentenceIDs[i]);
			dos.writeInt(Z[i]);
			features[i].serialize(dos);
		}
	}
	
	@Override
	public String toString(){

		StringBuilder sb = new StringBuilder();
		sb.append(location);
		sb.append("\t");
		sb.append(relation);
		sb.append("\t");
		for(Number n_node: n){
			sb.append(n_node);
			sb.append("|");
		}
		sb.setLength(sb.length()-1);
		sb.append("\n");
		for(int i = 0; i < numNodesCount; i++){
			sb.append("Number  " + i + " ");
			for(int feat: numFeatures[i].ids){
				sb.append(feat);
				sb.append(" ");
			}
			sb.append("\n");
		}
		for(int i = 0; i < numMentions; i++){
			sb.append("Mention " + i+ " ");
			sb.append("\""+sentenceIDs[i] +"\"\t" );
			for(int feat: features[i].ids){
				sb.append(feat);
				sb.append(" ");
			}
			sb.append("\n");
		}
		sb.setLength(sb.length()-1);

		
		
		return sb.toString().trim();
	}
	
	public static void main(String[] args) throws IOException{
		
		LRGraph d = new LRGraph();
		DataInputStream dis = new DataInputStream(new FileInputStream(new File(args[0])));
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(args[1])));
		while(d.read(dis)){
			bw.write(d.toString()+"\n");
		}
		bw.close();
		dis.close();
	}
}