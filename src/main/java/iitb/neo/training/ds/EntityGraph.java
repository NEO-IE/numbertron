package main.java.iitb.neo.training.ds;

/**
 * A light weight rewrite of the original location relation graph.
 * The Z nodes are now multi-ary, and are taken care of in the parse
 * The graph stores the features of the mentions and the number nodes
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

import meta.RelationMetaData;
import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;


public class EntityGraph extends Graph {
	int NUM_RELATIONS = RelationMetaData.NUM_RELATIONS;
	//for each number-unit, there are NUM_RELATIONS binary number nodes
	
	public Number[][] n; //The set Q_e
	
	public int numNodesCount = 0;
	
	
	
	public EntityGraph() {
		mentionIDs = new int[MNT_CAPACITY];
		Z = new int[MNT_CAPACITY];
		features = new SparseBinaryVector[MNT_CAPACITY];
		n = new Number[MNT_CAPACITY][NUM_RELATIONS + 1];
	}

	public void clear() {
		numMentions = 0;
		numNodesCount = 0;
	}

	/**
	 * makes space for numMentions mentions and numNodesCount 
	 * number nodes
	 * @param numMentions
	 * @param numNodesCount
	 */
	public void setCapacity(int numMentions, int numNodesCount) {
		n = new Number[numNodesCount][RelationMetaData.NUM_RELATIONS + 1];
		SparseBinaryVector[] newFeatures = new SparseBinaryVector[numMentions];
		features = newFeatures;
	}


	/**
	 * Reads the entity, number nodes followed by the features
	 * @param dis
	 * @return
	 * @throws IOException
	 */
	public boolean read(DataInputStream dis) throws IOException {
		try {
			random = dis.readInt();
			entity = dis.readUTF();

			int totalN = dis.readInt();
			//there are (#relations * #unique number-units) number nodes
			int countNumNodes = totalN / RelationMetaData.NUM_RELATIONS;
			n = new Number[countNumNodes][RelationMetaData.NUM_RELATIONS + 1];
			for (int i = 0; i < countNumNodes; i++) {
				for(int r = 1; r <= RelationMetaData.NUM_RELATIONS; r++) {
					n[i][r] = new Number();
					n[i][r].deserialize(dis);
				}
			}

			this.numNodesCount = countNumNodes;
			
		

			
			this.numMentions = dis.readInt();
			
			for (int i = 0; i < numMentions; i++) {
				if (features[i] == null)
					features[i] = new SparseBinaryVector();
				features[i].deserialize(dis);
			}
			
			return true;
		} catch (EOFException e) {
			return false;
		}
	}
	
	/**
	 * Writes the number nodes followed by the sparse features
	 * @param dos
	 * @throws IOException
	 */
	public void write(DataOutputStream dos) throws IOException {
		dos.writeInt(random);
		dos.writeUTF(entity);
		
		//write the number nodes
		dos.writeInt(n.length * (NUM_RELATIONS));
		for (int i = 0; i < n.length; i++) {
			for(int r = 1; r <= NUM_RELATIONS; r++) {
				n[i][r].serialize(dos);
				//the null number node is just for convenience, not really needed
				//Anything associated with relation number 0 is NA
			}
		}
			
		//now write the mentions
		

		dos.writeInt(numMentions);
		for (int i = 0; i < numMentions; i++) {
			features[i].serialize(dos);
		}
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append(entity);
		sb.append("\n");
		
		//first dump the number nodes
		for(int i = 0; i < numNodesCount; i++) {
			for(int r = 1; r <= RelationMetaData.NUM_RELATIONS; r++) {
				for (Number n_node : n[i]) {
					sb.append(n_node);
					sb.append("\n");
				}
			}	
		}
		
		sb.setLength(sb.length() - 1);
		sb.append("\n");

		for(int i = 0; i < numMentions; i++) {
			sb.append("Mention " + i + " ");
			for (int feat : features[i].ids) {
				sb.append(feat);
				sb.append(" ");
			}
			sb.append("\n");
		}
		sb.setLength(sb.length() - 1);

		return sb.toString().trim();
	}

	public static void main(String[] args) throws IOException {

		EntityGraph d = new EntityGraph();
		DataInputStream dis = new DataInputStream(new FileInputStream(new File(
				args[0])));
		BufferedWriter bw = new BufferedWriter(
				new FileWriter(new File(args[1])));
		while (d.read(dis)) {
			bw.write(d.toString() + "\n");
		}
		bw.close();
		dis.close();
	}
}