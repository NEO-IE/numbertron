package main.java.iitb.neo.extract;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import main.java.iitb.neo.training.algorithm.lpercp.FullInference;
import main.java.iitb.neo.training.algorithm.lpercp.Scorer;
import main.java.iitb.neo.training.ds.LRGraph;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
import edu.washington.multirframework.multiralgorithm.Mappings;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;
import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;

/**
 * Accepts a feature vector and returns a list of possible relations sorted by score.
 * @author aman
 *
 */
public class SentLevelExtractor {
	
	private FeatureGenerator fg;
	private ArgumentIdentification ai;
	private SententialInstanceGeneration sig;

	private String dir;
	private Mappings mapping;
	private Model model;
	private Parameters params;
	private Scorer scorer;

	public Map<Integer, String> relID2rel = new HashMap<Integer, String>();

	public SentLevelExtractor(String pathToMultirFiles, FeatureGenerator fg,
			ArgumentIdentification ai, SententialInstanceGeneration sig) {
		this.fg = fg;
		this.ai = ai;
		this.sig = sig;
		dir = pathToMultirFiles;
		try {
			mapping = new Mappings();
			mapping.read(dir + "/mapping");

			model = new Model();
			model.read(dir + "/model");

			params = new Parameters();
			params.model = model;
			params.deserialize(dir + "/params");

			scorer = new Scorer();

			for (String key : mapping.getRel2RelID().keySet()) {
				Integer id = mapping.getRel2RelID().get(key);
				relID2rel.put(id, key);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Map<Integer, Double> extractFromSententialInstanceWithAllFeatureScores(
			Argument arg1, Argument arg2, CoreMap sentence, Annotation doc, BufferedWriter genFeature) throws IOException {
		String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
		String arg1ID = null;
		String arg2ID = null;
		if (arg1 instanceof KBArgument) {
			arg1ID = ((KBArgument) arg1).getKbId();
		}
		if (arg2 instanceof KBArgument) {
			arg2ID = ((KBArgument) arg2).getKbId();
		}
		List<String> features = fg.generateFeatures(arg1.getStartOffset(),
				arg1.getEndOffset(), arg2.getStartOffset(),
				arg2.getEndOffset(), arg1ID, arg2ID, sentence, doc);
		genFeature.write(senText + "\t" + arg1.getArgName() + "\t" + arg2.getArgName() + "\t"
				+ features + "\n");
		return getPerRelationScoreMap(features, arg1, arg2, senText);
	}


	/**
	 * Conver features and args to MILDoc and run Multir sentential extraction
	 * algorithm, return null if no extraction was predicted.
	 * 
	 * @param features
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	private Map<Integer, Double> getPerRelationScoreMap(
			List<String> features, Argument arg1, Argument arg2, String senText) {

		LRGraph lrg = new LRGraph();
		lrg.location = arg1.getArgName();
		lrg.relation = arg2.getArgName();
		
		lrg.numMentions = 1;// sentence level prediction just one sentence
		lrg.setCapacity(1);
		SparseBinaryVector sv = lrg.features[0] = new SparseBinaryVector();

		SortedSet<Integer> ftrset = new TreeSet<Integer>();
		for (String f : features) {
			int ftrid = mapping.getFeatureID(f, false);
			if (ftrid >= 0) {
				ftrset.add(ftrid);
			}
		}

		sv.num = ftrset.size();
		sv.ids = new int[sv.num];

		int k = 0;
		for (int f : ftrset) {
			sv.ids[k++] = f;
		}


		return FullInference.getRelationScoresPerMention(lrg, scorer, params);
		
	}
	
	public Mappings getMapping() {
		return mapping;
	}
}
