package main.java.iitb.neo.spotting;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.RelationMatching;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.data.NegativeAnnotation;
import edu.washington.multirframework.distantsupervision.NegativeExampleCollection;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;
import edu.washington.multirframework.util.BufferedIOUtils;

/**
 * Responsible for iterating over the sentences and creating a match file.
 * This file will then be used to create the instances of the graph.
 * @author aman
 *
 */

public class Spotting {
	protected ArgumentIdentification ai;
	protected SententialInstanceGeneration sig;
	protected RelationMatching rm;
	protected NegativeExampleCollection nec;
	
	public static void main(String args[]) {
	String jsonProperties = IOUtils.toString(new FileInputStream(new File(propertiesFile)));
	Map<String,Object> properties = JsonReader.jsonToMaps(jsonProperties);
	String corpusPath = getStringProperty(properties,"corpusPath");
	cis = new CustomCorpusInformationSpecification();
	
	String altCisString = getStringProperty(properties,"cis");
	if(altCisString != null){
		cis = (CustomCorpusInformationSpecification)ClassLoader.getSystemClassLoader().loadClass(altCisString).newInstance();
	}
	Corpus corpus = new Corpus(corpusPath, cis, true);
	}

	public void run(String outputFileName,KnowledgeBase kb, Corpus c) throws SQLException, IOException, NullPointerException{
    	long start = System.currentTimeMillis();
    	PrintWriter dsWriter = new PrintWriter(BufferedIOUtils.getBufferedWriter(new File(outputFileName)));
		Iterator<Annotation> di = c.getDocumentIterator();
		if(null == di) {
			System.out.println("NULL");
		}
		int count =0;
		long startms = System.currentTimeMillis();
		long timeSpentInQueries = 0;
		while(di.hasNext()){
			Annotation d = di.next();
			if(null == d) {
				System.out.println(d);
			}
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			
			List<NegativeAnnotation> documentNegativeExamples = new ArrayList<>();
			List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> documentPositiveExamples = new ArrayList<>();
			for(CoreMap sentence : sentences){
				int sentGlobalID = sentence.get(SentGlobalID.class);
				
				//argument identification
				List<Argument> arguments =  ai.identifyArguments(d,sentence);
				
				//sentential instance generation
				List<Pair<Argument,Argument>> sententialInstances = sig.generateSententialInstances(arguments, sentence);
				
				//relation matching
				List<Triple<KBArgument,KBArgument,String>> distantSupervisionAnnotations = 
						rm.matchRelations(sententialInstances,kb,sentence,d);
				//adding sentence IDs
				List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> dsAnnotationWithSentIDs = new ArrayList<>();
				for(Triple<KBArgument,KBArgument,String> trip : distantSupervisionAnnotations){
					Integer i = new Integer(sentGlobalID);
					Pair<Triple<KBArgument,KBArgument,String>,Integer> p = new Pair<>(trip,i);
					dsAnnotationWithSentIDs.add(p);
				}		
				//negative example annotations
				List<NegativeAnnotation> negativeExampleAnnotations =
						findNegativeExampleAnnotations(sententialInstances,distantSupervisionAnnotations,
								kb,sentGlobalID);
				
				documentNegativeExamples.addAll(negativeExampleAnnotations);
				documentPositiveExamples.addAll(dsAnnotationWithSentIDs);				
			}
			writeDistantSupervisionAnnotations(documentPositiveExamples,dsWriter);
			writeNegativeExampleAnnotations(nec.filter(documentNegativeExamples,documentPositiveExamples,kb,sentences),dsWriter);
			count++;
			if( count % 1000 == 0){
				long endms = System.currentTimeMillis();
				System.out.println(count + " documents processed");
				System.out.println("Time took = " + (endms-startms));
				startms = endms;
				System.out.println("Time spent in querying db = " + timeSpentInQueries);
				timeSpentInQueries = 0;
			}
		}
		dsWriter.close();
    	long end = System.currentTimeMillis();
    	System.out.println("Distant Supervision took " + (end-start) + " millisseconds");
	}
	
	public static void writeNegativeExampleAnnotations(
			List<NegativeAnnotation> filter, PrintWriter dsWriter) {
		
		List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> dsFormatList = new ArrayList<>();
		for(NegativeAnnotation negAnno: filter){
			List<String> rels = negAnno.getNegativeRelations();
			for(String negRel: rels){
				Triple<KBArgument,KBArgument,String> trip = new Triple<>(negAnno.getArg1(),negAnno.getArg2(),negRel);
				Pair<Triple<KBArgument,KBArgument,String>,Integer> p = new Pair<>(trip,negAnno.getSentNum());
				dsFormatList.add(p);
			}
		}
		
		writeDistantSupervisionAnnotations(dsFormatList, dsWriter);

		
	}

	protected  List<NegativeAnnotation> findNegativeExampleAnnotations(
			List<Pair<Argument, Argument>> sententialInstances,
			List<Triple<KBArgument, KBArgument, String>> distantSupervisionAnnotations,
			KnowledgeBase KB, Integer sentGlobalID) {
		
		Map<String,List<String>> entityMap = KB.getEntityMap();
		List<NegativeAnnotation> negativeExampleAnnotations = new ArrayList<>();
		for(Pair<Argument,Argument> p : sententialInstances){
			//check that at least one argument is not in distantSupervisionAnnotations
			Argument arg1 = p.first;
			Argument arg2 = p.second;
			boolean canBeNegativeExample = true;
			for(Triple<KBArgument,KBArgument,String> t : distantSupervisionAnnotations){
				Argument annotatedArg1 = t.first;
				Argument annotatedArg2 = t.second;
				
				//if sententialInstance is a distance supervision annotation
				//then it is not a negative example candidate
				if( (arg1.getStartOffset() == annotatedArg1.getStartOffset()) &&
					(arg1.getEndOffset() == annotatedArg1.getEndOffset()) &&
					(arg2.getStartOffset() == annotatedArg2.getStartOffset()) &&
					(arg2.getEndOffset() == annotatedArg2.getEndOffset())){
					canBeNegativeExample = false;
					break;
				}
			}
			if(canBeNegativeExample){
				//look for KBIDs, select a random pair
				List<String> arg1Ids = new ArrayList<>();
				if(arg1 instanceof KBArgument){
					   arg1Ids.add(((KBArgument) arg1).getKbId());
				}
				else{
					if(entityMap.containsKey(arg1.getArgName())){
						arg1Ids = entityMap.get(arg1.getArgName());
					}						
				}

				List<String> arg2Ids = new ArrayList<>();
				if(arg2 instanceof KBArgument){
					arg2Ids.add(((KBArgument) arg2).getKbId());
				}
				else{
					if(entityMap.containsKey(arg2.getArgName())){
						arg2Ids = entityMap.get(arg2.getArgName());
					}
				}
				if( (!arg1Ids.isEmpty()) && (!arg2Ids.isEmpty())){
					//check that no pair of entities represented by these
					//argument share a relation:
					if(KB.noRelationsHold(arg1Ids,arg2Ids)){
						Collections.shuffle(arg1Ids);
						Collections.shuffle(arg2Ids);
						String arg1Id = arg1Ids.get(0);
						String arg2Id = arg2Ids.get(0);
						if((!arg1Id.equals("null")) && (!arg2Id.equals("null"))){
							KBArgument kbarg1 = new KBArgument(arg1,arg1Id);
							KBArgument kbarg2 = new KBArgument(arg2,arg2Id);
							Triple<KBArgument,KBArgument,String> t = new Triple<>(kbarg1,kbarg2,"NA");
							List<String> annoRels = new ArrayList<String>();
							annoRels.add(t.third);
							NegativeAnnotation negAnno = new NegativeAnnotation(t.first,t.second,sentGlobalID,annoRels);
							if(!containsNegativeAnnotation(negativeExampleAnnotations,t)) negativeExampleAnnotations.add(negAnno);
						}
					}
				}
			}
		}
		return negativeExampleAnnotations;
	}
	
	
	protected static boolean containsNegativeAnnotation(
			List<NegativeAnnotation> negativeExampleAnnotations,
			Triple<KBArgument, KBArgument, String> t) {
		for(NegativeAnnotation anno : negativeExampleAnnotations){
			KBArgument annoArg1 = anno.getArg1();
			KBArgument annoArg2 = anno.getArg2();

			if( (annoArg1.getStartOffset() == t.first.getStartOffset()) &&
				(annoArg1.getEndOffset() == t.first.getEndOffset()) &&
				(annoArg2.getStartOffset() == t.second.getStartOffset()) &&
				(annoArg2.getEndOffset() == t.second.getEndOffset()) ){
				return true;
			}
		}	
		return false;
	}

	/**
	 * Write out distant supervision annotation information
	 * @param distantSupervisionAnnotations
	 * @param dsWriter
	 * @param sentGlobalID
	 */
	public static void writeDistantSupervisionAnnotations(
			List<Pair<Triple<KBArgument, KBArgument, String>,Integer>> distantSupervisionAnnotations, PrintWriter dsWriter) {
		for(Pair<Triple<KBArgument,KBArgument,String>,Integer> dsAnno : distantSupervisionAnnotations){
			Triple<KBArgument,KBArgument,String> trip = dsAnno.first;
			Integer sentGlobalID = dsAnno.second;
			KBArgument arg1 = trip.first;
			KBArgument arg2 = trip.second;
			String rel = trip.third;
			dsWriter.write(arg1.getKbId());
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
	
	public static class DistantSupervisionAnnotation{
		KBArgument arg1;
		KBArgument arg2;
		String rel;
		Integer sentID;
	}
	
	
	private List<String> getListProperty(Map<String, Object> properties,
			String string) {
		if(properties.containsKey(string)){
			JsonObject obj = (JsonObject) properties.get(string);
			List<String> returnValues = new ArrayList<>();
			for(Object o : obj.getArray()){
				returnValues.add(o.toString());
			}
			return returnValues;
		}
		return new ArrayList<>();
	}
	private String getStringProperty(Map<String,Object> properties, String str) {
		if(properties.containsKey(str)){
			if(properties.get(str)== null){
				return null;
			}
			else{
				return properties.get(str).toString();
			}
		}
		return null;
	}


}
