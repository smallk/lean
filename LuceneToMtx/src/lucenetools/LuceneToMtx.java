/**************************************************************
Copyright 2015 Georgia Institute of Technology

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
***************************************************************/

package lucenetools;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexNotFoundException;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Collections;

/**
 * This class holds the index and count information for a single term.
 *
 * @author Richard Boyd
 */
class TermData 
{  
    public TermData() {}
    
    public TermData(int ti, int tc)
    {
        termIndex = ti;
        termCount = tc;
    }

    // index of this term in the dictionary
    public int termIndex;
    
    // number of occurrences of this term 
    public int termCount;
}

/**
 * This class reads a Lucene index, builds a sparse term-frequency
 * matrix, and writes the results to a MatrixMarket file.
 *
 * @author Richard Boyd
 */
public class LuceneToMtx 
{   
    private static final String VERSION = "0.2";
    private static final String MATRIXFILE = "matrix.mtx";
    private static final String DICTFILE = "dictionary.txt";
    private static final String DOCFILE = "documents.txt";
    private static final String PATHFIELD = "path";
    private static final String CONTENTSFIELD = "contents";
    
    /**
     * Extract all terms from the indexed "contents" field.  Ignore
     * any terms with freq counts less than the minTermFreq value or 
     * greater than the maxPercentage value. Assign each term a 
     * dictionary index.
     *
     * @param  reader        reader for index
     * @param  dictMap       mapping of index to term
     * @param  minTermFreq   minimum value for frequency else pruned
     * @param  numDocs       total number of documents in corpus
     * @param  maxPercentage maximum value for percentage else pruned
     * @return boolean       whether or not there was an error
     */
    public static boolean extractTerms(IndexReader reader, 
                                       Map<String, Integer> dictMap,
                                       int minTermFreq, int numDocs, int maxPercentage)
    {
        // a TreeSet sorts the dictionary strings
        Set<String> dictionary = new TreeSet<>();
        int maxOccurrences = (int)(((double)maxPercentage/100.0) * (double)numDocs);        
        try 
        {
            Fields fields = MultiFields.getFields(reader);
            if (null != fields) 
            {                                                   
                // get terms derived from content
                Terms terms = fields.terms(CONTENTSFIELD);
                TermsEnum te = terms.iterator(null);                

                while (te.next() != null) 
                {
                    String term_str = te.term().utf8ToString();

                    // number of occurrences of this term in all docs (rowsum)
                    int total_term_freq = (int) (te.totalTermFreq());
                    if (total_term_freq >= minTermFreq)
                    {
                        if (maxPercentage != 100){
                            if (total_term_freq <= maxOccurrences) {
                                dictionary.add(term_str);                        
                            }
                        } else {
                            dictionary.add(term_str);
                        }

                    }
                }                
            } 
            else 
            {                
                System.err.println("No fields found in index.");
                return false;
            }
        }
        catch (IOException e) 
        {
            System.out.println(" caught a " + e.getClass() +
                "\n with message: " + e.getMessage());
            return false;
        }

        // build a map associating the dictionary index with each term
        int index = 0;
        dictMap.clear();
        for (String s : dictionary)
            dictMap.put(s, index++);
        
        return true;
    }
    
    /**
     * Build a feature vector for a particular document
     *
     * @param  fv        feature vector object
     * @param  te        Lucene container for terms
     * @param  dictMap   mapping of index to term
     * @return int       number of terms in this document
     */
    public static int buildFeatureVector(FeatureVector fv,
                                         TermsEnum te,
                                         Map<String, Integer> dictMap)
    {
        int numTerms = 0;
        
        try
        {
            while (te.next() != null) 
            {
                String term_string = te.term().utf8ToString();
                if (!dictMap.containsKey(term_string))
                    continue;

                int rowIndex = dictMap.get(term_string);

                // term freq for THIS DOC ONLY (since te addressed the terms 
                // in a term vector from a single document
                int termFreq = (int) te.totalTermFreq();

                fv.add(rowIndex, termFreq);

                ++numTerms;
            }
        }
        catch (IOException e) 
        {
            System.out.println(" caught a " + e.getClass() +
                "\n with message: " + e.getMessage());
            return 0;
        }
        
        return numTerms;
    }
      
    /**
     * Write the dictionary file.
     *
     * @param  f         file to write
     * @param  dictMap   mapping of index to term
     */
    public static void writeDictionaryFile(File f,
                                           Map<String, Integer> dictMap)
    {
        PrintWriter out = null;
        String fileName = f.getPath();
        
        try
        {
            out = new PrintWriter(fileName);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Dictionary file creation failed.");
            System.exit(1);
        }
        
        // We expect the map traversal to extract the terms in sorted
        // order.  Compare with the stored index to double-check. It is
        // critical to assign the correct index to each term.
        
        int expectedIndex = 0;
        for (Map.Entry<String, Integer> entry : dictMap.entrySet())
        {
            int index = entry.getValue();
            if (index != expectedIndex)
            {           
                System.err.println("Unexpected order for dictionary terms.");
                System.exit(1);
            }
            
            out.println(entry.getKey());
            ++expectedIndex;
        }
        
        out.close();
    }
    
    /**
     * Write the documents file.
     *
     * @param  f            file to write
     * @param  matrixData   list containing doucment names
     */
    public static void writeDocumentFile(File f,
                                         LinkedList<FeatureVector> matrixData)
    {
        PrintWriter out = null;
        String fileName = f.getPath();
        
        try
        {
            out = new PrintWriter(fileName);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Document file creation failed.");
            System.exit(1);
        }
        
        // walk the list and write the document names
        ListIterator<FeatureVector> iter = matrixData.listIterator();
        while(iter.hasNext())
        {
            FeatureVector v = iter.next();
            out.println(v.docPath);
        }
        
        out.close();
    }
    
    /**
     * Write the matrix file.
     *
     * @param  f            file to write
     * @param  fileName     filename
     * @param  matrixData   list containing doucment names
     * @param  numRows      number of rows
     * @param  numCols      number of columns
     * @param  nnz          number of nonzeros
     */
    public static void writeMatrixMarketFile(File f,
                                             LinkedList<FeatureVector> matrixData, 
                                             int numRows, 
                                             int numCols, 
                                             int nnz)
    {
        PrintWriter out = null;
        String fileName = f.getPath();
        try
        {
            out = new PrintWriter(fileName);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Matrix file creation failed.");
            System.exit(1);
        }
        
        // write banner
        out.println("%%MatrixMarket matrix coordinate real general");
            
        // write height, width, number of nonzeros
        out.print(numRows);
        out.print(" ");
        out.print(numCols);
        out.print(" ");
        out.println(nnz);

        int colIndex = 0;
        
        // write (rowIndex, colIndex, termCount) triplets
        ListIterator<FeatureVector> iter = matrixData.listIterator();
        while (iter.hasNext()) 
        {
            FeatureVector v = iter.next();

            // MM files use ones-based indexing for rows and cols
            //int colIndex = v.docIndex + 1;
            ++colIndex;
            
            ArrayList<TermData> colVector = v.data;
            Iterator<TermData> it = colVector.iterator();
            while (it.hasNext()) {
                TermData td = it.next();
                out.print(td.termIndex + 1);
                out.print(" ");
                out.print(colIndex);
                out.print(" ");
                out.println(td.termCount);
            }            
        }

        out.close();
    }
    
    /**
     * Write any additional field files.
     *
     * @param  f            output directory
     * @param  fields       Lucene fields to extract
     * @param  matrixData   list containing doucment names
     */
    public static void writeFieldFiles(File outdir, Collection<String> fields,
                                         LinkedList<FeatureVector> matrixData)
    {
        PrintWriter out = null;
        for (String field : fields) {
            File f = new File(outdir + "/" + field + ".txt");
            String fileName = f.getPath();
            try
            {
                out = new PrintWriter(fileName);
            }
            catch (FileNotFoundException e)
            {
                System.err.println("File creation failed for field: " + field + ".");
                System.exit(1);
            }
            
            // walk the list and write the field values
            ListIterator<FeatureVector> iter = matrixData.listIterator();
            while(iter.hasNext())
            {
                FeatureVector v = iter.next();
                out.println(v.fields.get(field));
            }
            out.close();
            System.out.println("Wrote " + field + ".txt.");
        }

    }  
    
    /**
     * Main application. 
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        Options opts = new Options();
        CommandLine commandLine = new CommandLine();
        
        // if no command line options specified, user wants help
        if (0 == args.length) 
        {
            commandLine.showHelp();
            System.exit(0);
        }

        // extract command line args and store in opts
        if (!commandLine.parse(args, opts))
            System.exit(1);
        
        if (opts.showHelp)
        {
            commandLine.showHelp();
            System.exit(0);
        }
        
        // validate all command line options
        if (!commandLine.isValid(opts))
            System.exit(1);            
        
        // report all command line options to the user
        System.out.println("\nLuceneToMtx version " + VERSION + ".");
        commandLine.printOpts(opts);       
        
        long maxMemory = Runtime.getRuntime().maxMemory()/1024/1024;
        System.out.println("Java runtime max memory: " + maxMemory + " MB.");        
        
        // Build a map and assign a dictionary index to each term.
        // Include only those terms that survive the min term freq cutoff.
        Map<String, Integer> dictMap = new TreeMap<>();
        
        File file = null;
        System.out.println("Processing index...");
        try 
        {
            file = new File(opts.indexDir);
            IndexReader reader = DirectoryReader.open(FSDirectory.open(file));
            TermsEnum te = null;
            int nnz = 0, numCols = 0, maxDocs = reader.maxDoc();
            LinkedList<FeatureVector> matrixData = new LinkedList<>();            
            
            // add other fields
            Collection<String> fields = new ArrayList<>();
            if (opts.fields > 0){
                fields = MultiFields.getIndexedFields(reader);
                fields.remove(CONTENTSFIELD);
                fields.remove(PATHFIELD);
            }
            
            if (!extractTerms(reader, dictMap, opts.minTermFreq, maxDocs-1, opts.maxTermPercentage))
                System.exit(1);
            
            
            // set of field names to extract
            Set<String> fieldSet = new HashSet<>();
            fieldSet.add(PATHFIELD);
            for (String s : fields) {
                fieldSet.add(s);
            }
            
            for (int i=0; i<maxDocs; ++i)
            {
                // get term vector for next document
                Terms terms = reader.getTermVector(i, CONTENTSFIELD);
                if (terms == null)
                    continue;
                
                te = terms.iterator(te);                
                FeatureVector fv = new FeatureVector(numCols);
                
                int numEntries = buildFeatureVector(fv, te, dictMap);
                if (numEntries > 0)
                {
                    // extract document path and save with FeatureVector
                    Document doc = reader.document(i, fieldSet);
                    fv.docPath = doc.get(PATHFIELD);
                    
                    // add any additional fields
                    for (String s : fields) {
                        fv.fields.put(s, doc.get(s));
                    }
                    
                    //System.out.println("processing document:" + fv.docPath);
                    
                    matrixData.add(fv);                                        
                    nnz += numEntries;
                    ++numCols;
                }
            }
            
            // Sort the feature vectors by their document path field.  Write 
            // the matrix columns in this sorted order.
            Collections.sort(matrixData, new FeatureVectorComparator());
            File outdir = new File(opts.outDir);
            writeMatrixMarketFile(new File(outdir, MATRIXFILE), matrixData, dictMap.size(), numCols, nnz);
            System.out.println("Wrote " + MATRIXFILE + ".");
            writeDictionaryFile(new File(outdir, DICTFILE), dictMap);
            System.out.println("Wrote " + DICTFILE + ".");
            writeDocumentFile(new File(outdir, DOCFILE), matrixData);
            System.out.println("Wrote " + DOCFILE + ".");
            writeFieldFiles(outdir, fields, matrixData);
        }
        catch (IndexNotFoundException e)
        {
            if (null != file)
            {
                System.out.println("Lucene index not found in: " +
                        file.getAbsolutePath());
            }
        }
        catch (IOException e) 
        {
            System.out.println("LuceneToMtx exception: caught a " + 
                    e.getClass() + 
                    "\nMessage: " + e.getMessage());
        }
    }    
}
