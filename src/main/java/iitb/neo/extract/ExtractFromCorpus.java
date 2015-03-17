package main.java.iitb.neo.extract;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.sententialextraction.DocumentExtractor;
import edu.washington.multir.util.EvaluationUtils;
import edu.washington.multir.util.ModelUtils;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.corpus.SentOffsetInformation.SentStartOffset;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.Extraction;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;


/**
 * The main method of this class will print to an output file all the
 * extractions made over an input corpus.
 * 
 * @author jgilme1
 * 
 */
public class ExtractFromCorpus {

	private CorpusInformationSpecification cis;
	private ArgumentIdentification ai;
	private FeatureGenerator fg;
	private List<SententialInstanceGeneration> sigs;
	private List<String> multirDirs;
	private String corpusPath;
	
	

	public ExtractFromCorpus(String propertiesFile) throws FileNotFoundException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		String jsonProperties = IOUtils.toString(new FileInputStream(new File(
				propertiesFile)));
		Map<String, Object> properties = JsonReader.jsonToMaps(jsonProperties);
		corpusPath = getStringProperty(properties,"corpusPath");
		
		String featureGeneratorClass = getStringProperty(properties, "fg");
		if (featureGeneratorClass != null) {
			fg = (FeatureGenerator) ClassLoader.getSystemClassLoader()
					.loadClass(featureGeneratorClass).newInstance();
		}

		String aiClass = getStringProperty(properties, "ai");
		if (aiClass != null) {
			ai = (ArgumentIdentification) ClassLoader.getSystemClassLoader()
					.loadClass(aiClass).getMethod("getInstance").invoke(null);
		}
		List<String> sigClasses = getListProperty(properties, "sigs");
		sigs = new ArrayList<>();
		for (String sigClass : sigClasses) {
			sigs.add((SententialInstanceGeneration) ClassLoader
					.getSystemClassLoader().loadClass(sigClass)
					.getMethod("getInstance").invoke(null));
		}
		multirDirs = new ArrayList<>();
		List<String> multirDirNames = getListProperty(properties,"models");
		for(String multirDirName : multirDirNames){
			multirDirs.add(multirDirName);
		}
		cis = new CustomCorpusInformationSpecification();
		
		String altCisString = getStringProperty(properties,"cis");
		if(altCisString != null){
			cis = (CustomCorpusInformationSpecification)ClassLoader.getSystemClassLoader().loadClass(altCisString).newInstance();
		}
		
	}

	
	public static void main(String[] args) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException, ParseException,
			NoSuchMethodException, SecurityException, IllegalArgumentException,
			InvocationTargetException, SQLException, IOException {
	
		
		ExtractFromCorpus efc = new ExtractFromCorpus(args[0]);
		Corpus c = new Corpus(efc.corpusPath, efc.cis, true);
		c.setCorpusToDefault();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("rulebased_extractions")));
		List<Extraction> extrs = getMultiModelExtractions(c, efc.ai, efc.fg, efc.sigs, efc.multirDirs, bw);
		System.out.println("Total extractions : " + extrs.size());
		bw.close();

	}

	private String getStringProperty(Map<String, Object> properties, String str) {
		if (properties.containsKey(str)) {
			if (properties.get(str) == null) {
				return null;
			} else {
				return properties.get(str).toString();
			}
		}
		return null;
	}

	public static String formatExtractionString(Corpus c, Extraction e)
			throws SQLException {
		StringBuilder sb = new StringBuilder();
		String[] eValues = e.toString().split("\t");
		String arg1Name = eValues[0];
		String arg2Name = eValues[3];
		String docName = eValues[6].replaceAll("__", "_");
		String rel = eValues[7];
		String sentenceText = eValues[9];

		Integer sentNum = Integer.parseInt(eValues[8]);
		Integer arg1SentStartOffset = Integer.parseInt(eValues[1]);
		Integer arg1SentEndOffset = Integer.parseInt(eValues[2]);
		Integer arg2SentStartOffset = Integer.parseInt(eValues[4]);
		Integer arg2SentEndOffset = Integer.parseInt(eValues[5]);

		CoreMap s = c.getSentence(sentNum);
		Integer sentStartOffset = s.get(SentStartOffset.class);
		Integer arg1DocStartOffset = sentStartOffset + arg1SentStartOffset;
		Integer arg1DocEndOffset = sentStartOffset + arg1SentEndOffset;
		Integer arg2DocStartOffset = sentStartOffset + arg2SentStartOffset;
		Integer arg2DocEndOffset = sentStartOffset + arg2SentEndOffset;

		sb.append(arg1Name);
		sb.append("\t");
		sb.append(arg1DocStartOffset);
		sb.append("\t");
		sb.append(arg1DocEndOffset);
		sb.append("\t");
		sb.append(arg2Name);
		sb.append("\t");
		sb.append(arg2DocStartOffset);
		sb.append("\t");
		sb.append(arg2DocEndOffset);
		sb.append("\t");
		sb.append(docName);
		sb.append("\t");
		sb.append(rel);
		sb.append("\t");
		sb.append(e.getScore());
		sb.append("\t");
		sb.append(sentenceText);
		return sb.toString().trim();

	}
	
	

	public static List<Extraction> getMultiModelExtractions(Corpus c,
			ArgumentIdentification ai, FeatureGenerator fg,
			List<SententialInstanceGeneration> sigs, List<String> modelPaths,
			BufferedWriter bw) throws SQLException, IOException {
		BufferedWriter featureWriter = new BufferedWriter(new FileWriter("extract_features"));
		
		List<Extraction> extrs = new ArrayList<Extraction>();
		for (int i = 0; i < sigs.size(); i++) {
			Iterator<Annotation> docs = c.getDocumentIterator();
			SententialInstanceGeneration sig = sigs.get(i);
			String modelPath = modelPaths.get(i);
			SentLevelExtractor sle = new SentLevelExtractor(modelPath, fg, ai, sig);
			
			Map<String, Integer> rel2RelIdMap = sle.getMapping().getRel2RelID();
			
			Map<Integer, String> ftID2ftMap = ModelUtils
					.getFeatureIDToFeatureMap(sle.getMapping());
		
//			PrintWriter pw1 = new PrintWriter(new FileWriter("features_map"));
//			for(Integer id : ftID2ftMap.keySet()) {
//				pw1.write(id.toString() + " - " + ftID2ftMap.get(id) + "\n");
//			}
//			pw1.close();
			int docCount = 0;
			while (docs.hasNext()) {
				Annotation doc = docs.next();
				List<CoreMap> sentences = doc
						.get(CoreAnnotations.SentencesAnnotation.class);
				for (CoreMap sentence : sentences) {
					// argument identification
					List<Argument> arguments = ai.identifyArguments(doc,
							sentence);
					// sentential instance generation
					List<Pair<Argument, Argument>> sententialInstances = sig
							.generateSententialInstances(arguments, sentence);
					for (Pair<Argument, Argument> p : sententialInstances) {
					
						Map<Integer, Double> perRelationScoreMap = sle
								.extractFromSententialInstanceWithAllFeatureScores(
										p.first, p.second, sentence, doc, featureWriter);
						if (extrResult != null) {
							Triple<String, Double, Double> extrScoreTriple = extrResult.first;
							if (!extrScoreTriple.first.equals("NA")) {
								//System.out.println(extrResult);
								Map<Integer, Double> featureScores = extrResult.second
										.get(rel2RelIdMap
												.get(extrResult.first.first));
								String rel = extrScoreTriple.first;
								List<Pair<String, Double>> featureScoreList = EvaluationUtils
										.getFeatureScoreList(featureScores,
												ftID2ftMap);

								//prepare extraction
								String docName = sentence
										.get(SentDocName.class);
									String senText = sentence
										.get(CoreAnnotations.TextAnnotation.class);
								Integer sentNum = sentence
										.get(SentGlobalID.class);
								Extraction e = new Extraction(p.first,
										p.second, docName, rel, sentNum,
										extrScoreTriple.third, senText);
								e.setFeatureScoreList(featureScoreList);
								extrs.add(e);
								bw.write(formatExtractionString(c, e) + "\n");
							}

						}
					}
				}
				docCount++;
				if (docCount % 100 == 0) {
					System.out.println(docCount + " docs processed");
					bw.flush();
				}	
			}
			
		}
		featureWriter.close();
		return EvaluationUtils.getUniqueList(extrs);
	}
	
	private static boolean exactlyOneNumber(Pair<Argument, Argument> p) {
		boolean firstHasNumber = p.first.getArgName().matches(".*\\d.*");
		boolean secondHasNumber = p.second.getArgName().matches(".*\\d.*");
		return firstHasNumber ^ secondHasNumber;
	}
	
	private static boolean secondNumber(Pair<Argument, Argument> p) {
		boolean secondHasNumber = p.second.getArgName().matches(".*\\d.*");
		return secondHasNumber;
	}
	

	private static double sigmoid(double score) {
		return 1 / (1 + Math.exp(-score));
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

}
