package main.java.iitb.neo.pretrain.spotting;

import java.io.IOException;
import java.sql.SQLException;

import edu.washington.multirframework.corpus.Corpus;

public class KeywordSpotting extends Spotting {

	@Override
	void iterateAndSpot(String outputFile, Corpus c) throws SQLException, IOException {
//	PrintWriter pw = new PrintWriter(outputFile);
//		
//		System.out.println("Reading the docs");
//		Iterator<Annotation> di = c.getDocumentIterator();
//		if (null == di) {
//			System.out.println("NULL");
//		}
//		// this is where we store results
//		List<Pair<Triple<KBArgument, KBArgument, String>, Integer>> kbArgRelnList = new ArrayList<Pair<Triple<KBArgument, KBArgument, String>, Integer>>();
//		ArrayList<Pair<Integer, Relation>> relationList = new ArrayList<Pair<Integer, Relation>>();
//		int sentID = 0;
//		System.out.println("Starting rule based distant supervision");
//		int numProcessed = 0, ignored = 0;
//		while (di.hasNext()) {
//			Annotation d = di.next();
//			if (null == d) {
//				System.out.println(d);
//			}
//			//System.out.println("Doc Shift");
//			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
//			//System.out.println("Got " + sentences.size() + " sentences");
//			
//			for (CoreMap sentence : sentences) {
//				numProcessed++;
//				sentID = sentence.get(SentGlobalID.class);
//				if(numProcessed % 1000 == 0) {
//					System.out.println("Processed: " + numProcessed + " , ignored: " + ignored);
//				}
//				
//				if(sentence.toString().length() > 350) {
//					ignored++;
//					continue;
//				}
//				try {
//					List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
//					for(int i =0; i < tokens.size(); i++){
//						CoreLabel token = tokens.get(i);
//						int begOffset = token.get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
//						int endOffset = token.get(SentenceRelativeCharacterOffsetEndAnnotation.class);
//						token.set(CoreAnnotations.TokenBeginAnnotation.class, begOffset);
//						token.set(CoreAnnotations.TokenEndAnnotation.class, endOffset);
//						
//					}
//					List<Triple<Integer, String, Integer>> deps = sentence.get(SentDependencyInformation.DependencyAnnotation.class);
//					List<Relation> currRels = rbased.spotPossibleRelations(deps, sentence);
//					for (Relation rel : currRels) {
//						relationList.add(new Pair<Integer, Relation>(sentID, rel));
//					}
//					
//				} catch (Exception e) {
//					//e.printStackTrace();
//					continue; //sshhh
//				}
//			
//			}
//			
//		}
//		for (Pair<Integer, Relation> irPair : relationList) {
//			Relation r = irPair.second;
//			sentID = irPair.first;
//			Word location = r.getCountry();
//			Number number = r.getNumber();
//			String relName = r.getRelName();
//			// System.out.println(location);
//			String countryId = countryFreebaseIdMap.get(location.getVal());
//			KBArgument countryArg = new KBArgument(new Argument(
//					location.getVal(), location.getStartOff(),
//					location.getEndOff()), countryId);
//			KBArgument numberArg = new KBArgument(new Argument(Double.toString(number.getFlatVal()),
//					number.getStartOff(), number.getEndOff()), Double.toString(number.getFlatVal()));
//			Triple<KBArgument, KBArgument, String> relTrip = new Triple<KBArgument, KBArgument, String>(
//					countryArg, numberArg, relName);
//			Pair<Triple<KBArgument, KBArgument, String>, Integer> relTripSentIdPair = new Pair<>(
//					relTrip, sentID);
//			kbArgRelnList.add(relTripSentIdPair);
//		}
//		writeDistantSupervisionAnnotations(kbArgRelnList, pw);
//		pw.close();
//
//		
	}
	

}
