# search-references
Searches recursively inside a directory in all files including jar and zip files for a set of key words

1. Extract the zip file.
2. Put your references list in the references.txt file seperated with comma's.
3. Put the directory which you want to search for in the path.txt file.
4. Put the List of False positives in the file 'falsePositives.txt' seperated by commas.
4. Simply double click on the JAR, or run command "java -jar path/to/jar/searchReferences.jar" if you want to look at the log.
5. The report is generated as HTML inside report folder.


Additions for Verion-2

1. Added ignore False Positives.
2. Added worp wrapping for the results.
