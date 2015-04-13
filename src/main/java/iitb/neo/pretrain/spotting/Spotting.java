//sg
/**
 * An attempt at integrating rule based system with multir
 */
package main.java.iitb.neo.pretrain.spotting;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.data.KBArgument;

public abstract class Spotting {
	
	abstract  void iterateAndSpot(String outputFile, Corpus c) throws SQLException, IOException;
	public static void writeDistantSupervisionAnnotations(
			List<Pair<Triple<KBArgument, KBArgument, String>, Integer>> distantSupervisionAnnotations,
			PrintWriter dsWriter) {
		for (Pair<Triple<KBArgument, KBArgument, String>, Integer> dsAnno : distantSupervisionAnnotations) {
			Triple<KBArgument, KBArgument, String> trip = dsAnno.first;
			Integer sentGlobalID = dsAnno.second;
			KBArgument arg1 = trip.first;
			KBArgument arg2 = trip.second;
			if (null == arg1) {
				System.out.println("here");
				continue;
			}
			String rel = trip.third;
			try {
				dsWriter.write(arg1.getKbId()); // for missing countries
			} catch (Exception e) {
				System.err.println("Country missing: " + arg1.getArgName());
			}
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg1.getStartOffset()));
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg1.getEndOffset()));
			dsWriter.write("\t");
			dsWriter.write(arg1.getArgName());
			dsWriter.write("\t");
			dsWriter.write(arg2.getKbId());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg2.getStartOffset()));
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg2.getEndOffset()));
			dsWriter.write("\t");
			dsWriter.write(arg2.getArgName());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(sentGlobalID));
			dsWriter.write("\t");
			dsWriter.write(rel);
			dsWriter.write("\n");
		}
	}
}
