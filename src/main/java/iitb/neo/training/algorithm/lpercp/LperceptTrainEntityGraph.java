package main.java.iitb.neo.training.algorithm.lpercp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import main.java.iitb.neo.training.ds.EntityGraph;
import main.java.iitb.neo.training.meta.EntityGraphMemoryDataset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;

import edu.washington.multir.development.MakeAverageModel;
import edu.washington.multirframework.multiralgorithm.Dataset;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;

/**
 * Driver for the local percepton perceptron algorithm
 * @author aman
 *
 */
public class LperceptTrainEntityGraph {
	
	public static int numberOfAverages = 1;

	public static void train(String dir, Random r, int numIterations, double regularizer, boolean finalAvg, boolean ignoreConfusion, String mappingFile) throws IOException {		
		Model model = new Model();
		model.read(dir + File.separatorChar + "model");
		
		LocalAveragedPerceptronEntityGraph lpton = new LocalAveragedPerceptronEntityGraph(model, r, numIterations, regularizer, finalAvg, mappingFile);
		
		Dataset<EntityGraph> train = null;
	
		train = (Dataset<EntityGraph>) new EntityGraphMemoryDataset(dir + File.separatorChar + "train");
		

		System.out.println("starting training with regularizer = " + regularizer + ", iterations = " + numIterations + ", finalAvg = " + finalAvg + ", ignoreConfusion = " + ignoreConfusion);
		
		if(numberOfAverages != 1){
			List<File> randomModelFiles = new ArrayList<File>();
			for(int avgIter = 0; avgIter < numberOfAverages; avgIter++){
				
				System.out.println("Average Iteration: " + avgIter);
				
				long start = System.currentTimeMillis();
				Parameters params = lpton.train(train);
				long end = System.currentTimeMillis();
				System.out.println("training time " + (end-start)/1000.0 + " seconds");
				params.serialize(dir + File.separatorChar + "params");
				
				File newModelFile = new File(dir+"avgIter"+avgIter);
				if(!newModelFile.exists()) newModelFile.mkdir();
				randomModelFiles.add(newModelFile);
				File oldParams = new File(dir+"/params");
				File newParams = new File(newModelFile.getAbsolutePath()+"/params");
				FileUtils.copyFile(oldParams, newParams);
				
				System.gc();
			}
			MakeAverageModel.run(randomModelFiles,new File(dir));
		}
		else{
			long start = System.currentTimeMillis();
			Parameters params = lpton.train(train);
			long end = System.currentTimeMillis();
			System.out.println("training time " + (end-start)/1000.0 + " seconds");
	
			params.serialize(dir + File.separatorChar + "params");
		}
	}
	
	public static void train(String dir) throws IOException {
		throw new NotImplementedException("Insufficient parameters to start training");

	}
	
	
	
	public static void main(String [] args) throws IOException{
		train(args[0]);
	}
}
