package main.java.iitb.neo.training.algorithm.lpercp;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import main.java.iitb.neo.training.meta.LRGraphMemoryDataset;
import main.java.iitb.neo.training.meta.LRGraphMemoryDatasetWithoutConfusedLocationRels;
import edu.washington.multirframework.multiralgorithm.AveragedPerceptron;
import edu.washington.multirframework.multiralgorithm.Dataset;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;

/**
 * Driver for the local percepton perceptron algorithm
 * @author aman
 *
 */
public class LperceptTrain {

	public static void train(String dir, Random r) throws IOException {		
		Model model = new Model();
		model.read(dir + File.separatorChar + "model");
		
		LocalAveragedPerceptron lpton = new LocalAveragedPerceptron(model, r);
		
		Dataset train = (Dataset) new LRGraphMemoryDatasetWithoutConfusedLocationRels(dir + File.separatorChar + "train");

		System.out.println("starting training m");
		
		long start = System.currentTimeMillis();
		Parameters params = lpton.train(train);
		long end = System.currentTimeMillis();
		System.out.println("training time " + (end-start)/1000.0 + " seconds");

		params.serialize(dir + File.separatorChar + "params");
	}
	
	public static void train(String dir) throws IOException {
		
		train(dir, new Random(1));

	}
	
	
	
	public static void main(String [] args) throws IOException{
		train(args[0]);
	}
}
