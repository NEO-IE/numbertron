package main.java.iitb.neo.autoeval;

import iitb.rbased.main.RuleBasedDriver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import main.java.iitb.neo.NtronExperiment;
import main.java.iitb.neo.extract.SentLevelExtractor;
import main.java.iitb.neo.pretrain.spotting.Spotting;

import org.apache.commons.io.IOUtils;

import com.cedarsoftware.util.io.JsonReader;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.Extraction;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;

/**
 * This class creates a gold test set to be fed to an automatic evaluator
 * @author aman
 *
 */
public class CreateGoldTruthFile {
	
	private String corpusPath;
	private CorpusInformationSpecification cis;
	private RuleBasedDriver rbased;
	private String outFile;
	private List<SententialInstanceGeneration> sigs;
	
	public CreateGoldTruthFile(String propertiesFile) throws FileNotFoundException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		String jsonProperties = IOUtils.toString(new FileInputStream(new File(propertiesFile)));
		Map<String, Object> properties = JsonReader.jsonToMaps(jsonProperties);

		rbased = new RuleBasedDriver(true);
		corpusPath = NtronExperiment.getStringProperty(properties, "corpusPath");
		cis = new CustomCorpusInformationSpecification();

		String altCisString = NtronExperiment.getStringProperty(properties, "cis");
		if (altCisString != null) {
			cis = (CustomCorpusInformationSpecification) ClassLoader.getSystemClassLoader().loadClass(altCisString)
					.newInstance();
		}
		
		List<String> sigClasses = NtronExperiment.getListProperty(properties, "sigs");
		
		sigs = new ArrayList<>();
		for (String sigClass : sigClasses) {
			sigs.add((SententialInstanceGeneration) ClassLoader.getSystemClassLoader().loadClass(sigClass)
					.getMethod("getInstance").invoke(null));
		}
		
		outFile = NtronExperiment.getStringProperty(properties, "resultFile");
		
	}
	
	private void generateGoldFile(Corpus c, ArgumentIdentification ai, List<SententialInstanceGeneration> sigs, List<String> modelPaths, BufferedWriter bw) {
		List<Extraction> extrs = new ArrayList<Extraction>();
		for (int i = 0; i < sigs.size(); i++) {
			Iterator<Annotation> docs = c.getDocumentIterator();
			SententialInstanceGeneration sig = sigs.get(i);
			
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
						if (!(exactlyOneNumber(p) && secondNumber(p) && !isYear(p.second.getArgName()))) {
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
	public void run() throws SQLException, IOException {
		Spotting spotter = new Spotting(corpusPath, cis, rbased);
		Corpus c = new Corpus(corpusPath, cis, true);
		spotter.iterateAndSpot(outFile, c);
		
	}
	
	public static void main(String args[]) throws FileNotFoundException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException {
		CreateGoldTruthFile createTruth = new CreateGoldTruthFile(args[0]);
		createTruth.run();
	}
	
}