package main.java.iitb.neo.util;

import main.java.org.tartarus.snowball.SnowballStemmer;

public class StemUtils {
	public static SnowballStemmer stemmer = null;
	
	static{
		@SuppressWarnings("rawtypes")
		Class stemClass = null;
		try {
			stemClass = Class.forName("main.java.org.tartarus.snowball.englishStemmer");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			stemmer = (SnowballStemmer) stemClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public static String getStemWord(String word){
		 stemmer.setCurrent(word);
		 stemmer.stem();
		 return stemmer.getCurrent();
	}
}
