package main.java.iitb.neo.extract;

import iitb.rbased.meta.RelationMetadata;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import main.java.iitb.neo.training.algorithm.lpercp.FullInference;
import main.java.iitb.neo.training.algorithm.lpercp.Scorer;
import main.java.iitb.neo.training.ds.LRGraph;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multir.util.ModelUtils;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
import edu.washington.multirframework.multiralgorithm.DenseVector;
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

	private String dir;
	private Mappings mapping;
	private Model model;
	private Parameters params;
	private Scorer scorer;
	
	public Map<Integer, String> relID2rel = new HashMap<Integer, String>();

	public SentLevelExtractor(String pathToMultirFiles, FeatureGenerator fg) {
		this.fg = fg;
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
	
	private List<String> getFeatureList(Argument arg1, Argument arg2, CoreMap sentence, Annotation doc) {
		
		String arg1ID = null;
		String arg2ID = null;
		if (arg1 instanceof KBArgument) {
			arg1ID = ((KBArgument) arg1).getKbId();
		}
		if (arg2 instanceof KBArgument) {
			arg2ID = ((KBArgument) arg2).getKbId();
		}
		return fg.generateFeatures(arg1.getStartOffset(),
				arg1.getEndOffset(), arg2.getStartOffset(),
				arg2.getEndOffset(), arg1ID, arg2ID, sentence, doc);
	}

	public Map<Integer, Double> extractFromSententialInstanceWithAllRelationScores(
			Argument arg1, Argument arg2, CoreMap sentence, Annotation doc) throws IOException {
		String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
		List<String> features = getFeatureList(arg1, arg2, sentence, doc);
		return getPerRelationScoreMap(features, arg1, arg2, senText);
	}

	LRGraph makeGraph(Argument arg1, Argument arg2, List<String> features) {
		LRGraph lrg = new LRGraph();

		
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
		return lrg;
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


		LRGraph lrg = makeGraph(arg1, arg2, features);
		return FullInference.getRelationScoresPerMention(lrg, scorer, params);
		
	}
	
	/**
	 * returns a mapping from the feature fired to the corresponding score for the best relation
	 * Primarily to be used for analysis
	 * @param relationNumber
	 * @return
	 */
	public void firedFeaturesScores(Argument arg1, Argument arg2, CoreMap sentence, Annotation doc, String rel, BufferedWriter bw) throws IOException{
		bw.write("_____________________________________________________\n");
		bw.write(sentence + "\n");
		bw.write(arg1 + "\t" + arg2  + "\t" + rel + "\n");
		
		List<String> features = getFeatureList(arg1, arg2, sentence, doc);
		LRGraph lrg = makeGraph(arg1, arg2, features);
		SparseBinaryVector sv = lrg.features[0];
		
		DenseVector p = params.relParameters[mapping.getRelationID(rel, false)];
		
		Map<Integer, String> ftID2ftMap = ModelUtils.getFeatureIDToFeatureMap(getMapping());
		
		for(Integer featureNumber: sv.ids) {
			if(featureNumber == -1) { 
				continue;	
			}
			bw.write(ftID2ftMap.get(featureNumber) + "  " + p.vals[featureNumber] + "\n");
		}
		bw.write("_____________________________________________________\n");
		
		//now writing for all the other relations
		Set<String> relNames = RelationMetadata.getRelations();
		
		for(String relName: relNames) {
			bw.write("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-\n");			
			bw.write(relName +"\n");
			DenseVector featuresForInf = params.relParameters[mapping.getRelationID(relName	, false)];
			for(Integer featureNumber: sv.ids) {
				if(featureNumber == -1) { 
					continue;	
				}
				bw.write(ftID2ftMap.get(featureNumber) + "  " + featuresForInf.vals[featureNumber] + "\n");
			}
			bw.write("_____________________________________________________\n");
			
		}
		
		bw.write("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n");
		bw.write("_____________________________________________________\n");
	
	}
	public Mappings getMapping() {
		return mapping;
	}
}
