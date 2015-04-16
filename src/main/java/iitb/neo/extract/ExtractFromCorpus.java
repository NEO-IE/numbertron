package main.java.iitb.neo.extract;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import main.java.iitb.neo.util.JsonUtils;
import main.java.iitb.neo.util.RegExpUtils;
import main.java.iitb.neo.util.UnitsUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
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

	private List<SententialInstanceGeneration> sigs;
	private List<String> ntronModelDir;
	private String corpusPath;


	private String resultsFile;
	private String verboseExtractionsFile;
	private double cutoff_confidence;
	private double cutoff_score;
	private FeatureGenerator mintzKeywordsFg;
	private FeatureGenerator numberFg;


	public ExtractFromCorpus(String propertiesFile) throws Exception {
		
		Map<String, Object> properties = JsonUtils.getJsonMap(propertiesFile);
		corpusPath = JsonUtils.getStringProperty(properties, "corpusPath");
		cutoff_confidence = Double.parseDouble(JsonUtils.getStringProperty(properties, "cutoff_confidence"));
		cutoff_score = Double.parseDouble(JsonUtils.getStringProperty(properties, "cutoff_score"));

		String mintzFeatureGeneratorClass = JsonUtils.getStringProperty(properties, "mintzKeywordsFg");
		String numbersFeatureGeneratorClass = JsonUtils.getStringProperty(properties, "numbersFg");
		
		if (mintzFeatureGeneratorClass != null && !mintzFeatureGeneratorClass.isEmpty()) {
			this.mintzKeywordsFg = (FeatureGenerator) ClassLoader.getSystemClassLoader().loadClass(mintzFeatureGeneratorClass).newInstance();
			
		}
		if(numbersFeatureGeneratorClass != null && !numbersFeatureGeneratorClass.isEmpty()) {
			this.numberFg = (FeatureGenerator) ClassLoader.getSystemClassLoader().loadClass(numbersFeatureGeneratorClass).newInstance();
		}
		

		String aiClass = JsonUtils.getStringProperty(properties, "ai");
		if (aiClass != null) {
			ai = (ArgumentIdentification) ClassLoader.getSystemClassLoader().loadClass(aiClass)
					.getMethod("getInstance").invoke(null);
		}
		List<String> sigClasses = JsonUtils.getListProperty(properties, "sigs");
		sigs = new ArrayList<>();
		for (String sigClass : sigClasses) {
			sigs.add((SententialInstanceGeneration) ClassLoader.getSystemClassLoader().loadClass(sigClass)
					.getMethod("getInstance").invoke(null));
		}
		ntronModelDir = new ArrayList<>();
		List<String> multirDirNames = JsonUtils.getListProperty(properties, "models");
		for (String multirDirName : multirDirNames) {
			ntronModelDir.add(multirDirName);
		}
		
		cis = new CustomCorpusInformationSpecification();

		String altCisString = JsonUtils.getStringProperty(properties, "cis");
		if (altCisString != null) {
			cis = (CustomCorpusInformationSpecification) ClassLoader.getSystemClassLoader().loadClass(altCisString)
					.newInstance();
		}

		resultsFile = JsonUtils.getStringProperty(properties, "resultsFile");
		verboseExtractionsFile = JsonUtils.getStringProperty(properties, "verboseExtractionFile");
	}

	public static void main(String[] args) throws Exception {

		ExtractFromCorpus efc = new ExtractFromCorpus(args[0]);
		Corpus c = new Corpus(efc.corpusPath, efc.cis, true);
		c.setCorpusToDefault();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(efc.resultsFile)));

		List<Extraction> extrs = efc.getExtractions(c, efc.ai, efc.mintzKeywordsFg, efc.sigs, efc.ntronModelDir);
		efc.writeExtractions(bw, c, extrs);
		
		System.out.println("Total extractions : " + extrs.size());
		bw.close();

	}
	
	
	public List<Extraction> getExtractions(String resultsFile, boolean writeExtractions) throws SQLException, IOException {
		Corpus c = new Corpus(corpusPath, cis, true);
		c.setCorpusToDefault();
		
		List<Extraction> extrs = getExtractions(c, ai, mintzKeywordsFg, sigs, ntronModelDir);
		if(writeExtractions) {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(resultsFile)));
			writeExtractions(bw, c, extrs);
			bw.close();
		}
		
		return extrs;
	}

	public List<Extraction> getExtractions(Corpus c, ArgumentIdentification ai, FeatureGenerator fg,
			List<SententialInstanceGeneration> sigs, List<String> modelPaths) throws SQLException,
			IOException {
		boolean ANALYZE = true;
		
		System.err.println("Extracting with a confidence of " + cutoff_confidence);
		BufferedWriter analysis_writer = null;
		if (ANALYZE) {
			this.verboseExtractionsFile = "verb_numbers_debug";
			analysis_writer = new BufferedWriter(new FileWriter(verboseExtractionsFile));
		}

		List<Extraction> extrs = new ArrayList<Extraction>();
		for (int i = 0; i < sigs.size(); i++) {
			Iterator<Annotation> docs = c.getDocumentIterator();
			SententialInstanceGeneration sig = sigs.get(i);
			String modelPath = modelPaths.get(i);
			SentLevelExtractor sle = new SentLevelExtractor(modelPath, mintzKeywordsFg, numberFg);

			// Map<String, Integer> rel2RelIdMap =
			// sle.getMapping().getRel2RelID();
			// Map<Integer, String> ftID2ftMap =
			// ModelUtils.getFeatureIDToFeatureMap(sle.getMapping());
			int sentNumber = 0;
			int docCount = 0;
			while (docs.hasNext()) {
				Annotation doc = docs.next();
				List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
				for (CoreMap sentence : sentences) {
					
					// argument identification
					List<Argument> arguments = ai.identifyArguments(doc, sentence);
					// sentential instance generation
					
					if(sentNumber++ % 50 == 0) {
						System.out.println("Extracting from sentence number " + sentNumber	);
					}
					List<Pair<Argument, Argument>> sententialInstances = sig.generateSententialInstances(arguments,
							sentence);
					for (Pair<Argument, Argument> p : sententialInstances) {
						if (p.first.getArgName().equals("years") || !(RegExpUtils.exactlyOneNumber(p) && RegExpUtils.secondNumber(p) && !RegExpUtils.isYear(p.second.getArgName()))) {
							continue;
						}
						Map<Integer, Double> perRelationScoreMap = sle
								.extractFromSententialInstanceWithAllRelationScores(p.first, p.second, sentence, doc);
						ArrayList<Integer> compatRels = UnitsUtils.unitsCompatible(p.second, sentence, sle.getMapping()
								.getRel2RelID());
						String relStr = null;
						Double extrScore = -1.0;
						for (Integer rel : perRelationScoreMap.keySet()) {
							if (compatRels.contains(rel)) {
								relStr = sle.relID2rel.get(rel);
								extrScore = perRelationScoreMap.get(rel);
								break;
							}
						}
						
						String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
						String docName = sentence.get(SentDocName.class);
						sentence.get(SentStartOffset.class);
					
						Integer sentNum = sentence.get(SentGlobalID.class);
						double max, min;
						ArrayList<Double> scores = new ArrayList<Double>(perRelationScoreMap.values());
						max = min = scores.get(0);
						for (int i1 = 1, l = scores.size(); i1 < l; i1++) {
							if (max < scores.get(i1)) {
								max = scores.get(i1);
							}
							if (min > scores.get(i1)) {
								min = scores.get(i1);
							}

						}
						double conf = 0.0;
						if(max != min) {
							conf = (extrScore - min) / (max - min);
						}
						if (conf <= cutoff_confidence) { // no compatible
															// extraction ||
							continue;
						}
						// System.out.println(extrResult);
						// prepare extraction
						if(null != relStr) {
							if(ANALYZE) {
								sle.firedFeaturesScores(p.first, p.second, sentence, doc, relStr, analysis_writer);
							}
							Extraction e = new Extraction(p.first, p.second, docName, relStr, sentNum, extrScore, senText);
							extrs.add(e);
							
						}

					}

				}
			}
			
			
			
			docCount++;
			if (docCount % 100 == 0) {
				System.out.println(docCount + " docs processed");
			}
		}
		if(ANALYZE) {
			analysis_writer.close();
		}
		
		return extrs;
	}

	public void writeExtractions(BufferedWriter bw, Corpus c, List<Extraction> extractions) throws IOException, SQLException {
		for(Extraction e: extractions) {
			bw.write(formatExtractionString(c, e) + "\n");
		}
	}
	
	public static String formatExtractionString(Corpus c, Extraction e) throws SQLException {
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
		sb.append(sentNum);
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
	
	public static String formatExtractionStringOriginalOffset(Corpus c, Extraction e) throws SQLException {
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
		sb.append(arg1Name);
		sb.append("\t");
		sb.append(arg1SentStartOffset);
		sb.append("\t");
		sb.append(arg1SentEndOffset);
		sb.append("\t");
		sb.append(arg2Name);
		sb.append("\t");
		sb.append(arg2SentStartOffset);
		sb.append("\t");
		sb.append(arg2SentEndOffset);
		sb.append("\t");
		sb.append(sentNum);
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
}
