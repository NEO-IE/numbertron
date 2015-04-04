package main.java.iitb.neo.extract;

import iitb.rbased.meta.RelationMetadata;
import iitb.shared.EntryWithScore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import main.java.iitb.neo.NtronExperiment;
import main.java.iitb.neo.util.RegExpUtils;

import org.apache.commons.io.IOUtils;

import catalog.Unit;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

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
import eval.UnitExtractor;

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
	private List<String> ntronModelDir;
	private String corpusPath;
	private UnitExtractor ue;
	private Set<String> relations;

	private String resultsFile;
	private String verboseExtractionsFile;
	private double cutoff_confidence;
	private double cutoff_score;
	private String weightFile;

	public ExtractFromCorpus(String propertiesFile) throws Exception {
		String jsonProperties = IOUtils.toString(new FileInputStream(new File(propertiesFile)));
		Map<String, Object> properties = JsonReader.jsonToMaps(jsonProperties);
		corpusPath = NtronExperiment.getStringProperty(properties, "corpusPath");
		cutoff_confidence = Double.parseDouble(NtronExperiment.getStringProperty(properties, "cutoff_confidence"));
		cutoff_score = Double.parseDouble(NtronExperiment.getStringProperty(properties, "cutoff_score"));
		
		String featureGeneratorClass = NtronExperiment.getStringProperty(properties, "fg");
		if (featureGeneratorClass != null) {
			fg = (FeatureGenerator) ClassLoader.getSystemClassLoader().loadClass(featureGeneratorClass).newInstance();
		}

		String aiClass = NtronExperiment.getStringProperty(properties, "ai");
		if (aiClass != null) {
			ai = (ArgumentIdentification) ClassLoader.getSystemClassLoader().loadClass(aiClass)
					.getMethod("getInstance").invoke(null);
		}
		List<String> sigClasses = NtronExperiment.getListProperty(properties, "sigs");
		sigs = new ArrayList<>();
		for (String sigClass : sigClasses) {
			sigs.add((SententialInstanceGeneration) ClassLoader.getSystemClassLoader().loadClass(sigClass)
					.getMethod("getInstance").invoke(null));
		}
		ntronModelDir = new ArrayList<>();
		List<String> multirDirNames = NtronExperiment.getListProperty(properties, "models");
		for (String multirDirName : multirDirNames) {
			ntronModelDir.add(multirDirName);
		}
		weightFile = ntronModelDir.get(0) + "_weights";
		cis = new CustomCorpusInformationSpecification();

		String altCisString = NtronExperiment.getStringProperty(properties, "cis");
		if (altCisString != null) {
			cis = (CustomCorpusInformationSpecification) ClassLoader.getSystemClassLoader().loadClass(altCisString)
					.newInstance();
		}
		ue = new UnitExtractor();
		relations = RelationMetadata.getRelations();
		resultsFile = NtronExperiment.getStringProperty(properties, "resultsFile");
		verboseExtractionsFile = NtronExperiment.getStringProperty(properties, "verboseExtractionFile");
	}

	public static void main(String[] args) throws Exception {

		ExtractFromCorpus efc = new ExtractFromCorpus(args[0]);
		Corpus c = new Corpus(efc.corpusPath, efc.cis, true);
		c.setCorpusToDefault();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(efc.resultsFile)));

		List<Extraction> extrs = efc.getExtractions(c, efc.ai, efc.fg, efc.sigs, efc.ntronModelDir, bw);
		System.out.println("Total extractions : " + extrs.size());
		bw.close();

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
		sb.append(docName);
		sb.append("\t");
		sb.append(rel);
		sb.append("\t");
		sb.append(e.getScore());
		sb.append("\t");
		sb.append(sentenceText);
		return sb.toString().trim();

	}

	public List<Extraction> getExtractions(Corpus c, ArgumentIdentification ai, FeatureGenerator fg,
			List<SententialInstanceGeneration> sigs, List<String> modelPaths, BufferedWriter bw) throws SQLException,
			IOException {
		boolean ANALYZE = true;
		BufferedWriter analysis_writer = null;
		if (ANALYZE) {
			
			analysis_writer = new BufferedWriter(new FileWriter(verboseExtractionsFile));
		}

		List<Extraction> extrs = new ArrayList<Extraction>();
		for (int i = 0; i < sigs.size(); i++) {
			Iterator<Annotation> docs = c.getDocumentIterator();
			SententialInstanceGeneration sig = sigs.get(i);
			String modelPath = modelPaths.get(i);
			SentLevelExtractor sle = new SentLevelExtractor(modelPath, fg, ai, sig);

			// Map<String, Integer> rel2RelIdMap =
			// sle.getMapping().getRel2RelID();
			// Map<Integer, String> ftID2ftMap =
			// ModelUtils.getFeatureIDToFeatureMap(sle.getMapping());

			int docCount = 0;
			while (docs.hasNext()) {
				Annotation doc = docs.next();
				List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
				for (CoreMap sentence : sentences) {
					
					// argument identification
					List<Argument> arguments = ai.identifyArguments(doc, sentence);
					// sentential instance generation
					System.out.println("sent: " + sentence);
					List<Pair<Argument, Argument>> sententialInstances = sig.generateSententialInstances(arguments,
							sentence);
					for (Pair<Argument, Argument> p : sententialInstances) {
						if (!(RegExpUtils.exactlyOneNumber(p) && RegExpUtils.secondNumber(p) && !RegExpUtils.isYear(p.second.getArgName()))) {
							continue;
						}
						Map<Integer, Double> perRelationScoreMap = sle
								.extractFromSententialInstanceWithAllRelationScores(p.first, p.second, sentence, doc);
						ArrayList<Integer> compatRels = unitsCompatible(p.second, sentence, sle.getMapping()
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
						if (conf <= 0.95) { // no compatible
															// extraction ||
							continue;
						}
						System.out.println(extrScore);
						// System.out.println(extrResult);
						// prepare extraction
						if(ANALYZE && null != relStr) {
							sle.firedFeaturesScores(p.first, p.second, sentence, doc, relStr, analysis_writer);
							
						}
						
						Extraction e = new Extraction(p.first, p.second, docName, relStr, sentNum, extrScore, senText);

						extrs.add(e);

//						bw.write(formatExtractionString(c, e) + " " + conf + "\n");
						bw.write(formatExtractionString(c, e) + "\n");
					}

				}
			}
			
			
			
			docCount++;
			if (docCount % 100 == 0) {
				System.out.println(docCount + " docs processed");
				bw.flush();
			}
		}
		bw.close();
		analysis_writer.close();
		
		return extrs;
	}


	/*
	 * returns the list of relation compatible with numeric argument
	 */
	private ArrayList<Integer> unitsCompatible(Argument numArg, CoreMap sentence, Map<String, Integer> map) {
		String sentString = sentence.toString();
		String tokenStr = numArg.getArgName();
		String parts[] = tokenStr.split("\\s+");
		int beginIdx = sentString.indexOf(parts[0]);
		int endIdx = beginIdx + parts[0].length();

		String front = sentString.substring(0, beginIdx);
		if (front.length() > 20) {
			front = front.substring(front.length() - 20);
		}
		String back = sentString.substring(endIdx);
		if (back.length() > 20) {
			back = back.substring(0, 20);
		}
		String utString = front + "<b>" + parts[0] + "</b>" + back;
		float values[][] = new float[1][1];
		List<? extends EntryWithScore<Unit>> unitsS = ue.parser.getTopKUnitsValues(utString, "b", 1, 0, values);

		// check for unit here....

		String unit = "";
		if (unitsS != null && unitsS.size() > 0) {
			unit = unitsS.get(0).getKey().getBaseName();
		}

		ArrayList<Integer> validRelations = new ArrayList<Integer>();
		for (String rel : relations) {
			if (unitRelationMatch(rel, unit)) {
				validRelations.add(map.get(rel));
			}
		}
		return validRelations;
	}

	public boolean unitRelationMatch(String rel, String unitStr) {
		Unit unit = ue.quantDict.getUnitFromBaseName(unitStr);
		if (unit != null && !unit.getBaseName().equals("")) {
			Unit SIUnit = unit.getParentQuantity().getCanonicalUnit();
			if (SIUnit != null && !RelationMetadata.getUnit(rel).equals(SIUnit.getBaseName()) || SIUnit == null
					&& !RelationMetadata.getUnit(rel).equals(unit.getBaseName())) {
				return false; // Incorrect unit, this cannot be the relation.

			}
		} else if (unit == null && !unitStr.equals("") && RelationMetadata.getUnit(rel).equals(unitStr)) { // for
																											// the
																											// cases
																											// where
																											// units
																											// are
																											// compound
																											// units.
			return true;
		} else {
			if (!RelationMetadata.getUnit(rel).equals("")) {
				return false; // this cannot be the correct relation.
			}
		}
		return true;
	}

	private static double sigmoid(double score) {
		return 1 / (1 + Math.exp(-score));
	}

	
}
