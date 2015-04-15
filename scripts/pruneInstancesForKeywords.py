#sg
#This script is aimed at pruning the keywords to include only those sentences that include a keyword corresponding to the relation in hand.


def prune(instancesFile, sentIdRelMap):
    pruned = 0
    scanned = 0
    for line in open(instancesFile, 'r'):
        lineSplit = line.strip("\n").split("\t")
        rel = lineSplit[9]
        sentid = int(lineSplit[8])
        relsApplicable = sentIdRelMap[sentid]
        if(rel in relsApplicable):
            print line.strip("\n")
        else:
            pruned += 1
        scanned += 1
        if(scanned % 100000 == 0):
            print "Processed = %d, Pruned = %d" %(scanned, pruned)

    print "Processed = %d, Pruned = %d" %(scanned, pruned)


#returns a map that stores valid relations corresponding to each relation id
def getSentRelMap(sentencesFile):
    kw = {}
    kw["POP"] = ["population", "people", "inhabitants", "natives", "residents", "people"]
    kw["AGL"] = ["area", "land", "large", "largest"]
    kw["FDI"] = ["foreign", "fdi", "direct", "investments", "investment"]
    kw["GOODS"] = ["goods", "exports", "export", "exporter", "exported", "ships", "shipped"]
    kw["ELEC"] = ["electricity", "kilowatthors", "terawatt", "generation", "production", "sector"]
    kw["CO2"] = ["carbon", "emission", "CO2", "co2", "emissions", "kilotons"]
    kw["INF"] = ["Inflation", "Price", "Rise", "rate"]
    kw["INTERNET"] = ["Internet", "users", "usage", "penetration", "use", "user"]
    kw["GDP"] = ["gross",  "domestic", "GDP", "gdp", "product"]
    kw["LIFE"] = ["life", "expectancy", "deaths", "rose"]
    kw["DIESEL"] = ["diesel", "price", "priced", "fuel", "prices"]
    rels = ["POP", "AGL", "FDI", "ELEC", "GOODS", "CO2", "INF", "INTERNET", "GDP", "LIFE", "DIESEL"]
    sentIdRelMap = {}
    i=0
    for sentenceLine in open(sentencesFile, 'r'):
        i += 1
        relsApplicable = []
        sentenceLineSplit = sentenceLine.split("\t")
        id = int(sentenceLineSplit[0])
        words = sentenceLineSplit[1].split()
        for rel in rels:
            keywords = kw[rel]
            for keyword in keywords:
                if(keyword in words):
                    relsApplicable.append(rel)
        sentIdRelMap[id] = relsApplicable
        if(i % 1000 == 0):
            print i, 'sentences processed'
    return sentIdRelMap



if __name__ == '__main__':
    import sys
    intancesFile = sys.argv[1]
    sentencesFile = sys.argv[2]
    sentIdRelMap = getSentRelMap(sentencesFile)
    prune(intancesFile, sentIdRelMap)


