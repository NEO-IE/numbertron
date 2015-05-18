package main.java.iitb.neo.training.algorithm.lpercp;

import main.java.iitb.neo.training.ds.LRGraph;
import edu.washington.multirframework.multiralgorithm.Parameters;

public interface CI {
	public Parse infer(LRGraph lrg, Scorer scorer, Parameters params);
}
