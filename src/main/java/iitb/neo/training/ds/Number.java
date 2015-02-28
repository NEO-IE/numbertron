package main.java.iitb.neo.training.ds;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "n" nodes	
 * Each nodes stores the absolute value of the number, and a list of Z nodes
 * to which it is attached. Every node is assumed to have a value at the 
 * moment.
 * @author aman
 *
 */
public class Number {

	public Number(String num, List<Integer> list) {
		this.zs_linked = new ArrayList<Integer>(list);
		this.value = Double.parseDouble(num);
	}
	ArrayList<Integer> zs_linked;
	Double value;
}
