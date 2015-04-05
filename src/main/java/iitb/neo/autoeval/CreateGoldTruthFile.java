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
import main.java.iitb.neo.util.RegExpUtils;
import main.java.iitb.neo.util.UnitsUtils;

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
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.Extraction;

/**
 * This class creates a gold test set to be fed to an automatic evaluator
 * 
 * @author aman
 * 
 */
public class CreateGoldTruthFile {

	private String corpusPath;
	private CorpusInformationSpecification cis;
	private RuleBasedDriver rbased;
	private String trueFile;
	private ArgumentIdentification ai;
	private List<SententialInstanceGeneration> sigs;

	public CreateGoldTruthFile(String propertiesFile) throws FileNotFoundException, IOException,
			InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
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

		String aiClass = NtronExperiment.getStringProperty(properties, "ai");
		if (aiClass != null) {
			ai = (ArgumentIdentification) ClassLoader.getSystemClassLoader().loadClass(aiClass)
					.getMethod("getInstance").invoke(null);
		}

		trueFile = NtronExperiment.getStringProperty(properties, "trueFile");

	}

	private void generateGoldFile(Corpus c) throws IOException, SQLException {
		List<Extraction> extrs = new ArrayList<Extraction>();
		BufferedWriter bw = new BufferedWriter(new FileWriter(trueFile));
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

					Integer sentNum = sentence.get(SentGlobalID.class);
					String docName = sentence.get(SentDocName.class);

					System.out.println("Processing sentence number " + sentNum);
					for (Pair<Argument, Argument> p : sententialInstances) {
						if (!(RegExpUtils.exactlyOneNumber(p) && RegExpUtils.secondNumber(p) && !RegExpUtils
								.isYear(p.second.getArgName()))) {
							continue;
						}
						ArrayList<String> compatibleRels = UnitsUtils.unitsCompatible(p.second, sentence);
						String senText = sentence.get(CoreAnnotations.TextAnnotation.class);

						for (String compatibleRel : compatibleRels) {
							Extraction e = new Extraction(p.first, p.second, docName, compatibleRel, sentNum, 0.0,
									senText);
							bw.write(main.java.iitb.neo.extract.ExtractFromCorpus.formatExtractionString(c, e) + "\n");
							extrs.add(e);
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
		bw.close();

	}

	public void run() throws SQLException, IOException {
		Spotting spotter = new Spotting(corpusPath, cis, rbased);
		Corpus c = new Corpus(corpusPath, cis, true);
		generateGoldFile(c);

	}

	public static void main(String args[]) throws FileNotFoundException, InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException, SQLException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		CreateGoldTruthFile createTruth = new CreateGoldTruthFile(args[0]);
		createTruth.run();
	}

}
