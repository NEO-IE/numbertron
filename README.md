sg

```json

{
  "corpusPath" :
"jdbc:derby:/mnt/a99/d0/aman/MultirExperiments/data/numbers_corpus",
  "train" : "true",
  "testDocumentsFile" :
"data/emptyFile",
  "mintzfg" :
"main.java.iitb.neo.pretrain.featuregeneration.MintzKeywordFeatureGenerator",
"numberfg" :
"main.java.iitb.neo.pretrain.featuregeneration.NumberFeatureGenerator",
"countriesList":
"/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/countries_list_ids",
  "ai" :
"edu.washington.multirframework.argumentidentification.NERArgumentIdentification",
  "rm" :
"edu.washington.multirframework.argumentidentification.NERRelationMatching",
  "nec" :
"edu.washington.multirframework.distantsupervision.NegativeExampleCollectionByRatio",
  "necRatio" : "0.25",	
  "kbRelFile" : "/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/kb-facts-train.tsv.gz",
  "kbEntityFile" : "/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/entity-names-train.tsv.gz",
  "targetRelFile" : "/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/target_relations.tsv",
  "typeRelMap" : null,
  "sigs" : [
"edu.washington.multirframework.argumentidentification.DefaultSententialInstanceGeneration"
],
  "dsFiles" : [
"data/instances.tsv"
],
  "featureFiles" : [
"data/features_mintz_keywords.tsv"
],
  "models" : [
"data/model_features_mintz_keywords_match20perc_regul0.5" ],
  "cis" :
"edu.washington.multirframework.corpus.DefaultCorpusInformationSpecification",
  "si" : [
"edu.washington.multirframework.corpus.SentNamedEntityLinkingInformation",
"edu.washington.multirframework.corpus.SentFreebaseNotableTypeInformation"
],
  "ti" : [ "edu.washington.multirframework.corpus.TokenChunkInformation" ],
  "di" : [ ],
  "useFiger" : "false"
}
```



#Implementation notes; incomplete

##Training Data Preparation

###Finding relevant sentences


The first file, that involves collecting sentences that are relevant can be done
by first creating a file similar to the one created by multir and then
aggregating appropriately.

This creation will now involve additional steps like pruning out sentences that
have modifiers, and checking for units. 

Finally, we will have a file similar to the instances file. There is a crucial
difference between what we do and what multir does however. For us, this is not
distant supervision, just spotting. There is no distant supervision that has
happened yet (In some sense, we already know about the unit of the relation
which can be considered to be a source of distant supervision, but that is it).

###Collecting into Location Number pairs We need to find all the sentences that
possibly express a given relation for a given location. This is easy given the
file above. The first column will give us the location and the last one will
give us hte relation.


###Creating features Again, should be easy. *Each mention* (each location-number
pair) will form one spot, there would be features generated for each spot.
Finally, we'll have a set of spots that are related to a location relation pair.
(For multir, this is done by first creating the feature file and then sorted the
first 2 columns, and picking up the adjoining rows, in our case, we can do
something similar; sort on the first and the last column).

###Notations used by Multir
1. Mappings: The class which stores HashMaps from string to integer, giving a
   number to each of the relations and features.  Only features that exceed a
   certain threshold count are assigned an id, the rest are not. The function
   getMappingsFromTrainingData() is responsible for this portion.

