//sg
/**
 * An attempt at integrating rule based system with multir
 */
package main.java.iitb.neo.pretrain.spotting;

import iitb.rbased.main.RuleBasedDriver;
import iitb.rbased.util.Number;
import iitb.rbased.util.Relation;
import iitb.rbased.util.Word;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.corpus.SentDependencyInformation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetEndAnnotation;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;

public class Spotting {
	String corpusPath;
	CorpusInformationSpecification cis;
	RuleBasedDriver rbased;
	private Map<String, String> countryFreebaseIdMap;
	
	public Spotting(String corpusPath, CorpusInformationSpecification cis,
			RuleBasedDriver rbased) {
		super();
		this.corpusPath = corpusPath;
		this.cis = cis;
		this.rbased = rbased;
		String countriesFile = "/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/countries_list_ids";

		try {

			BufferedReader br = new BufferedReader(
					new FileReader(countriesFile));
			String countryRecord = null;
			countryFreebaseIdMap = new HashMap<String, String>();
			while ((countryRecord = br.readLine()) != null) {
				String vars[] = countryRecord.split("\t");
				String countryName = vars[1].toLowerCase();
				String countryId = vars[0];
				// System.out.println(countryName);
				countryFreebaseIdMap.put(countryName, countryId);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * The corpus is specified in the constructor. This function iterates over the sentences in the corpus,
	 * for each sentence, attaches required annotations, and calls the rule based system.
	 * Basically, performs the following: For each sentence that has a country-number, we check the following for spotting:
	 * a) There should not be a modifier in the dependency path from the country to the number.
	 * b) The unit should be consistent. 
	 * This will lead to a large number of matches for a given country, number pair, since we don't check for the keywords at
	 * all.
	 * @throws SQLException
	 * @throws IOException
	 */
	public void iterateAndSpot(String outputFile, Corpus c) throws SQLException, IOException {
		PrintWriter pw = new PrintWriter(outputFile);
		
		System.out.println("Reading the docs");
		Iterator<Annotation> di = c.getDocumentIterator();
		if (null == di) {
			System.out.println("NULL");
		}
		// this is where we store results
		List<Pair<Triple<KBArgument, KBArgument, String>, Integer>> kbArgRelnList = new ArrayList<Pair<Triple<KBArgument, KBArgument, String>, Integer>>();
		ArrayList<Pair<Integer, Relation>> relationList = new ArrayList<Pair<Integer, Relation>>();
		int sentID = 0;
		System.out.println("Starting rule based distant supervision");
		int numProcessed = 0, ignored = 0;
		while (di.hasNext()) {
			Annotation d = di.next();
			if (null == d) {
				System.out.println(d);
			}
			//System.out.println("Doc Shift");
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			//System.out.println("Got " + sentences.size() + " sentences");
			
			for (CoreMap sentence : sentences) {
				numProcessed++;
				sentID = sentence.get(SentGlobalID.class);
				if(numProcessed % 1000 == 0) {
					System.out.println("Processed: " + numProcessed + " , ignored: " + ignored);
				}
				
				if(sentence.toString().length() > 350) {
					ignored++;
					continue;
				}
				try {
					List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
					for(int i =0; i < tokens.size(); i++){
						CoreLabel token = tokens.get(i);
						int begOffset = token.get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
						int endOffset = token.get(SentenceRelativeCharacterOffsetEndAnnotation.class);
						token.set(CoreAnnotations.TokenBeginAnnotation.class, begOffset);
						token.set(CoreAnnotations.TokenEndAnnotation.class, endOffset);
						
					}
					List<Triple<Integer, String, Integer>> deps = sentence.get(SentDependencyInformation.DependencyAnnotation.class);
					List<Relation> currRels = rbased.spotPossibleRelations(deps, sentence);
					for (Relation rel : currRels) {
						relationList.add(new Pair<Integer, Relation>(sentID, rel));
					}
					
				} catch (Exception e) {
					//e.printStackTrace();
					continue; //sshhh
				}
			
			}
			
		}
		for (Pair<Integer, Relation> irPair : relationList) {
			Relation r = irPair.second;
			sentID = irPair.first;
			Word location = r.getCountry();
			Number number = r.getNumber();
			String relName = r.getRelName();
			// System.out.println(location);
			String countryId = countryFreebaseIdMap.get(location.getVal());
			KBArgument countryArg = new KBArgument(new Argument(
					location.getVal(), location.getStartOff(),
					location.getEndOff()), countryId);
			KBArgument numberArg = new KBArgument(new Argument(Double.toString(number.getFlatVal()),
					number.getStartOff(), number.getEndOff()), Double.toString(number.getFlatVal()));
			Triple<KBArgument, KBArgument, String> relTrip = new Triple<KBArgument, KBArgument, String>(
					countryArg, numberArg, relName);
			Pair<Triple<KBArgument, KBArgument, String>, Integer> relTripSentIdPair = new Pair<>(
					relTrip, sentID);
			kbArgRelnList.add(relTripSentIdPair);
		}
		writeDistantSupervisionAnnotations(kbArgRelnList, pw);
		pw.close();
	}

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
