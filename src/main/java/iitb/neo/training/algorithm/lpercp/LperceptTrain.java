package main.java.iitb.neo.training.algorithm.lpercp;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import main.java.iitb.neo.training.ds.LRGraph;
import main.java.iitb.neo.training.meta.LRGraphMemoryDataset;
import main.java.iitb.neo.training.meta.LRGraphMemoryDatasetWithoutConfusedLocationRels;

import org.apache.commons.lang.NotImplementedException;

import edu.washington.multirframework.multiralgorithm.Dataset;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;

/**
 * Driver for the local percepton perceptron algorithm
 * @author aman
 *
 */
public class LperceptTrain {

	public static void train(String dir, Random r, int numIterations, double regularizer, boolean finalAvg, boolean ignoreConfusion) throws IOException {		
		Model model = new Model();
		model.read(dir + File.separatorChar + "model");
		
		LocalAveragedPerceptron lpton = new LocalAveragedPerceptron(model, r, numIterations, regularizer, finalAvg);
		
		Dataset<LRGraph> train = null;
		if(ignoreConfusion) {
			train = (Dataset<LRGraph>) new LRGraphMemoryDatasetWithoutConfusedLocationRels(dir + File.separatorChar + "train");
		} else {
			train = (Dataset<LRGraph>) new LRGraphMemoryDataset(dir + File.separatorChar + "train");
		}

		System.out.println("starting training with regularizer = " + regularizer + ", iterations = " + numIterations + ", finalAvg = " + finalAvg + ", ignoreConfusion = " + ignoreConfusion);
		
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
