package main.java.iitb.neo.training.ds;

/**
 * A light weight rewrite of the original location relation graph.
 * The Z nodes are now multi-ary, and are taken care of in the parse
 * The graph stores the features of the mentions and the number nodes
 * Extends from the class Graph only for compatibility, none of the 
 * variables from the original class is used
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

public class EntityGraph{
	int NUM_RELATIONS = RelationMetaData.NUM_RELATIONS;
	// for each number-unit, there are NUM_RELATIONS binary number nodes
	public String entity;
	int random;
	public int numMentions = 0;
	public int numNodesCount = 0;
	public Number[] n; // The set Q_e
	// We just need one number node, the parse can take care of the replication
	// The number object anyways doesn't store any information about the label

	// we also need to store the set of relations that are allowed for each
	public Mention[] s;

	public EntityGraph() {

	}

	public void clear() {
		numMentions = 0;
		numNodesCount = 0;
	}

	/**
	 * makes space for numMentions mentions and numNodesCount number nodes
	 * 
	 * @param numMentions
	 * @param numNodesCount
	 */
	public void setCapacity(int numMentions, int numNodesCount) {
		n = new Number[numNodesCount];
		s = new Mention[numMentions];
	}

	/**
	 * Reads the entity, number nodes followed by the features
	 * 
	 * @param dis
	 * @return
	 * @throws IOException
	 */
	public boolean read(DataInputStream dis) throws IOException {
		try {
			random = dis.readInt();
			entity = dis.readUTF();

			// there are (#relations * #unique number-units) number nodes

			this.numNodesCount = dis.readInt();
			this.numMentions = dis.readInt();

			setCapacity(numMentions, numNodesCount);
			for (int i = 0; i < numNodesCount; i++) {
				n[i] = new Number();
				n[i].deserialize(dis);
			}

			for (int i = 0; i < numMentions; i++) {
				s[i] = new Mention();
				s[i].deserialize(dis);
			}

			return true;
		} catch (EOFException e) {
			return false;
		}
	}

	/**
	 * Writes the number nodes followed by the sparse features
	 * 
	 * @param dos
	 * @throws IOException
	 */
	public void write(DataOutputStream dos) throws IOException {
		dos.writeInt(random);
		dos.writeUTF(entity);

		// write the number nodes
		dos.writeInt(numNodesCount);
		dos.writeInt(numMentions);

		for (int i = 0; i < numNodesCount; i++) {
			n[i].serialize(dos);
		}

		// now write the mentions
		for (int i = 0; i < numMentions; i++) {
			s[i].serialize(dos);
		}
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append(entity);
		sb.append("\n");

		// first dump the number nodes
		for (Number n_node : n) {
			sb.append(n_node);
			sb.append("\n");
		}
		sb.append("\n");

		sb.setLength(sb.length() - 1);

		for (int i = 0; i < numMentions; i++) {
			sb.append("Mention " + i + " ");
			sb.append(s[i]);
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