##Training

###Collecting relevant sentences


The first file, that involves collecting sentences that are relevant can be done by first creating a file similar to the one created by multir and then aggregating appropriately.

This creation will now involve additional steps like pruning out sentences that have modifiers, and checking for units. 

Finally, we will have a file similar to the instances file. There is a crucial difference between what we do and what multir does however.
For us, this is not distant supervision, just spotting. There is no distant supervision that has happened yet (In some sense, we already 
know about the unit of the relation which can be considered to be a source of distant supervision, but that is it).


