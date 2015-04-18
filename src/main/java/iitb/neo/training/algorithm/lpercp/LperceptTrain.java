package main.java.iitb.neo.training.algorithm.lpercp;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.lang.NotImplementedException;
import org.apache.xml.utils.UnImplNode;

import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.meta.LRGraphMemoryDatasetWithoutConfusedLocationRels;
import edu.washington.multirframework.multiralgorithm.Dataset;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;

/**
 * Driver for the local percepton perceptron algorithm
 * @author aman
 *
 */
public class LperceptTrain {

	public static void train(String dir, Random r, int numIterations, double regularizer, boolean finalAvg) throws IOException {		
		Model model = new Model();
		model.read(dir + File.separatorChar + "model");
		
		LocalAveragedPerceptron lpton = new LocalAveragedPerceptron(model, r, numIterations, regularizer, finalAvg);
		
		Dataset<LRGraph> train = (Dataset<LRGraph>) new LRGraphMemoryDatasetWithoutConfusedLocationRels(dir + File.separatorChar + "train");

		System.out.println("starting training with regularizer = " + regularizer + ", iterations = " + numIterations + " finalAvg = " + finalAvg);
		
		long start = System.currentTimeMillis();
		Parameters params = lpton.train(train);
		long end = System.currentTimeMillis();
		System.out.println("training time " + (end-start)/1000.0 + " seconds");

		params.serialize(dir + File.separatorChar + "params");
	}
	
	public static void train(String dir) throws IOException {
		throw new NotImplementedException("Insufficient parameters to start training");

	}
	
	
	
	public static void main(String [] args) throws IOException{
		train(args[0]);
	}
}
