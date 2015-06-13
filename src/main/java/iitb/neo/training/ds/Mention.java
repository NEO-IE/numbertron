package main.java.iitb.neo.training.ds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;

/**
 * The abstraction for the instances
 * Just stores the features and the units, the actual values are stored in the parse
 * @author aman
 *
 */
public class Mention {
	public SparseBinaryVector features;
	public String unit;

	
	public Mention(SparseBinaryVector features, String unit) {
		this.features = features;
		this.unit = unit;
	}
	public Mention() {
		
	}

	public void serialize(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(unit);
		features.serialize(dos);
	}

	public void deserialize(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		this.unit = dis.readUTF();
		if(features == null) {
			features = new SparseBinaryVector();
		}
		features.deserialize(dis);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int feat : features.ids) {
			sb.append(feat);
			sb.append(" ");
		}
		sb.append("\n");
		sb.append(unit);
		return sb.toString();
	}
}
