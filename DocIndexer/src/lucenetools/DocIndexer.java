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

import twitter.TwitterTextValueReader;
import analyzers.AnalyzerFactory;
import analyzers.DebugAnalyzer;   
import java.io.BufferedReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.util.CharArraySet;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;
import org.apache.tika.exception.TikaException;

/** 
 * This code is a (substantially) modified version of the demo program 
 * 'Indexer' that accompanies the book LuceneInAction, 2nd ed. by McCandless
 * et. al., Manning Press, 2010.
 *
 * This class controls indexing documents from directories.
 * 
 * @author Richard Boyd, Ashley Beavers
 * 
 */
public class DocIndexer 
{
    private static Path rootDir;
    private static final String VERSION = "0.3";
    
    // number of documents added to the Lucene index
    private static int docsIndexed = 0;
    
    // whether Twitter data is being analyzed
    private static boolean isTwitter = false;
    
    private static final int MAX_CHARS = TwitterTextValueReader.DEFAULT_BUFFER_SIZE;
    private static final char[] cbuf = new char[MAX_CHARS];
    private static Analyzer analyzer;
    
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.text.ParseException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static void main(String[] args) throws IOException, ParseException, 
            ClassNotFoundException, NoSuchMethodException, InstantiationException, 
            IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        // disable the exceedingly verbose node4j output from Tika
        Logger.getRootLogger().removeAllAppenders();
        Logger.getRootLogger().setLevel(Level.OFF);        
        
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
        

        // consolidate stop files into a single CharSetArray
        String [] stopFiles = {opts.defaultStopFile, opts.userStopFile};
        CharArraySet stopWordSet = StopWordSetGenerator.generate(stopFiles);
        
        // consolidate spelling files
        String [] spellingFiles = {opts.defaultSpellFile, opts.userSpellFile};
        SpellingFile.consolidate(spellingFiles);
        HashMap<String, String> spellingHashtable = SpellingFile.getHashtable();
        
        // generate the slang hash map
        String [] slangFiles = {opts.defaultSlangFile};
        SpellingFile.consolidate(slangFiles);
        HashMap<String, String> slangHashtable = SpellingFile.getHashtable();
        
        // create the user-specified analyzer
        analyzer = AnalyzerFactory.create(opts.analyzerName, stopWordSet, 
                                          spellingHashtable, slangHashtable,
                                          opts.tokenOpts, opts.modelFile);

        // check if the analyzer is valid
        if (analyzer == null) {
            System.out.println("Error: No analyzer with that name.");
            System.exit(1);
        }

        System.out.println("\nDocIndexer version " + VERSION + ".\n");
        commandLine.printOpts(opts);
        
        // naive way to determine whether to use Twitter document extraction
        // or assume each document is a single document
        isTwitter = opts.analyzerName.toLowerCase().contains("twitter");
        
        long maxMemory = Runtime.getRuntime().maxMemory()/1024/1024;
        System.out.println("Java runtime max memory: " + maxMemory + " MB.");
        
        if (opts.analyze > 0)
        {
            
            // show analysis results then exit
            if (opts.useMongo)
            {
                DocIndexerMongo.showTweetAnalysis(opts.analyze, opts, analyzer);           
                System.exit(0);

            } 
            else
            {
                if (isTwitter)
                    DebugAnalyzer.showTweetAnalysis(opts.analyze, opts.inputDir, analyzer);
                else
                    DebugAnalyzer.showDocAnalysis(opts.analyze, opts.inputDir, analyzer);
                System.exit(0);
            }
        }
        
        long start = System.nanoTime();
        try 
        {
            Path outpath = Paths.get(opts.outputDir);
            outpath = outpath.toAbsolutePath();
            System.out.println("Writing index to: '" + outpath.toString() + "' ...\n");
            
            Directory dir = FSDirectory.open(new File(opts.outputDir));
            IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
            
            // create a new index in the directory, removing any 
            // previously-indexed documents
            config.setOpenMode(OpenMode.CREATE);
            
            // Optional: for better indexing performance, if you are 
            // indexing many documents, increase the RAM buffer.  But if
            // you do this, increase the max heap size available to the 
            // JVM (eg add -Xmx512m or -Xmxlg).
            //config.setRAMBufferSizeMB(256.0);
            IndexWriter writer = new IndexWriter(dir, config);
            
            if (opts.useMongo)
            {
                // Parse the configuration file and use the connection
                // details to index the documents.
                DocIndexerMongo.indexDocs(writer, opts, analyzer);
                docsIndexed = DocIndexerMongo.getDocsIndexed();
            }
            else
            {            
                // Index documents from the provided input directory.
                final File docDir = new File(opts.inputDir);
                rootDir = Paths.get(docDir.getPath());
                indexDocs(writer, docDir, isTwitter, opts);            
            } 
            
            // NOTE: if you want to maximize search performance, you can
            // optionally call forceMerge here.  This can be a terribly 
            // costly operation, so generally it's only worth it when
            // your index is relatively static (i.e. you are finished
            // adding documents to it).
            //writer.forceMerge(1);
            
            // commit docs to the index
            writer.close();            
        } 
        catch (IOException e) 
        {
            ErrorReporter.reportException(e.getClass().toString(), e.getMessage());
            System.exit(-1);
        }
                
        long end = System.nanoTime();
        double elapsed = (end - start)*1.0e-9;
        
        System.out.println("\n\nIndexed " + docsIndexed + " documents.");
        System.out.printf("Elapsed time: %.2f seconds, avg. rate: %.2f docs/s.\n\n", 
                elapsed, docsIndexed / elapsed);
    }

    /**
     * Parse the files as JSON Twitter documents.
     * 
     * @param    writer                      writer to use for indexing documents
     * @param    pathName                    filepath
     * @param    fis                         stream for file
     * @param    opts                        options object
     * @throws   IOException                 if there is an issue with the document
     * @throws   NoSuchMethodException       if method not found
     * @throws   IllegalAccessException      cannot access class
     * @throws   IllegalArgumentException    wrong set of arguments
     * @throws   InvocationTargetException   instantiated class has a problem
    */
    static void indexTweetsFromFile(IndexWriter writer, File pathName, FileInputStream fis, Options opts) 
            throws IOException, NoSuchMethodException, IllegalAccessException, 
            IllegalArgumentException, InvocationTargetException
    {
        // Twitter servers return UTF-8 data
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        TwitterTextValueReader reader = new TwitterTextValueReader(br);
        
        // index of the tweet in the file
        int tweetIndex;

        while (true)
        {
            // read the next tweet JSON object from the file
            int tweetLen = reader.read(cbuf, 0, MAX_CHARS);
            if (-1 == tweetLen)
                break;
            String tweetJSON = new String(cbuf, 0, tweetLen);

            // the reader determines the index of the tweet in the file
            tweetIndex = reader.getTweetIndex();

            // construct the path that represents the original tweet location
            Path thisPath = Paths.get(pathName.getPath());
            Path relPath = rootDir.relativize(thisPath);
            String path = relPath.toString() + "_" + String.valueOf(tweetIndex);
            
            // use the analyzer's package name to find out if there is a custom
            // implementation of the Utils class with the addDocument function
            String packageName = opts.analyzerName.split("\\.")[0];
            try {
                Class<?> cla = Class.forName(packageName + ".Utils");
                Method method = cla.getMethod("addDocument", String.class, String.class, IndexWriter.class);
                method.invoke(null, tweetJSON, path, writer);
            // use the default Utils implementaiton
            } catch (ClassNotFoundException | NoSuchMethodException cnfe) {
                Utils.addDocument(tweetJSON, path, writer);
            } 

            ++docsIndexed;
            if (0 == (docsIndexed % 1000))
            {
                System.out.print("Docs analyzed and indexed: " + docsIndexed + "\r");
                System.out.flush();
            }                        
        } 
    }      
    
    /**
     * Parse the files as regular documents.
     * 
     * @param    writer                      writer to use for indexing documents
     * @param    pathName                    filepath
     * @param    fis                         stream for file
     * @param    opts                        options object
     * @throws   IOException                 if there is an issue with the document
     * @throws   NoSuchMethodException       if method not found
     * @throws   IllegalAccessException      cannot access class
     * @throws   IllegalArgumentException    wrong set of arguments
     * @throws   InvocationTargetException   instantiated class has a problem
    */    
    static void indexFile(IndexWriter writer, File pathName, FileInputStream fis, Options opts) 
            throws IOException, NoSuchMethodException, IllegalAccessException, 
            IllegalArgumentException, InvocationTargetException
    {
        // setup the Tika autodetect parser
        ContentHandler contentHandler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();            
        Parser parser = new AutoDetectParser();

        boolean parseOK = true;
        try
        {
            parser.parse(fis, contentHandler, metadata, parseContext);
        }
        catch (SAXException e)
        {
            parseOK = false;
            ErrorReporter.reportException(e.getClass().toString(), e.getMessage());
        }
        catch (TikaException e)
        {
            // Tika could not recognize the document
            parseOK = false;
            ErrorReporter.reportException(e.getClass().toString(), e.getMessage());
        }
        catch (OutOfMemoryError e)
        {                
            ErrorReporter.reportException(e.getClass().toString(), e.getMessage());
            System.out.println("The Java virtual machine is out of heap space. ");
            System.out.println("Indexed " + docsIndexed + " documents.");
            System.out.println("Failed on file " + pathName.toString());
            System.out.println("Exiting...");
            System.exit(-1);
        }

        if (!parseOK)
        {
            System.out.println("Skipping file " + pathName);
        }
        else if (0 == contentHandler.toString().length())
        {
            System.out.println("Document format not supported for file " + pathName);
        }
        else
        {
            Path thisPath = Paths.get(pathName.getPath());
            Path relPath = rootDir.relativize(thisPath);
            String docName = relPath.toString();
            
            // use the analyzer's package name to find out if there is a custom
            // implementation of the Utils class with the addDocument function
            String packageName = opts.analyzerName.split("\\.")[0];
            try {
                Class<?> cla = Class.forName(packageName + ".Utils");
                Method method = cla.getMethod("addDocument", String.class, String.class, IndexWriter.class);
                method.invoke(null, contentHandler.toString(), docName, writer);
            // use the default Utils implementaiton
            } catch (ClassNotFoundException | NoSuchMethodException cnfe) {
                Utils.addDocument(contentHandler.toString(), docName, writer);
            } 
            ++docsIndexed;
            if (0 == (docsIndexed % 10))
            {
                System.out.print("Docs analyzed and indexed: " + docsIndexed + "\r");
                System.out.flush();
            }
        }
    }
    
    /**
     * Indexes the given file using the given writer, or if a directory
     * is given, recurses over files and directories found under the given
     * directory.  
     * 
     * NOTE: this method indexes one document per file.
     * 
     * @param    writer                     writer to the index where the given file/dir info will be stored
     * @param    pathName                   the file to index, or the directory to recurse into to find files to index
     * @param    twitter                    true if files should be parsed as JSON
     * @param    opts                       options object determined by command line and config params
     * @throws   IOException                if there is a low-level I/O error
     * @throws   NoSuchMethodException      if method not found
     * @throws   IllegalAccessException     cannot access class
     * @throws   IllegalArgumentException   wrong set of arguments
     * @throws   InvocationTargetException  instantiated class has a problem
    */    
    static void indexDocs(IndexWriter writer, File pathName, boolean twitter, Options opts) 
            throws IOException, NoSuchMethodException, IllegalAccessException, 
            IllegalArgumentException, InvocationTargetException 
    {
        // do not try to index hidden files or files that cannot be read
        if (!pathName.isHidden() && pathName.canRead()) 
        {
            if (pathName.isDirectory()) 
            {
                String[] files = pathName.list();
                if (files != null) 
                {
                    for (int i=0; i<files.length; ++i) 
                    {                                                
                        indexDocs(writer, new File(pathName, files[i]), twitter, opts);
                    }
                }
            } 
            else 
            {
                FileInputStream fis;
                try 
                {
                    fis = new FileInputStream(pathName);
                } 
                catch(FileNotFoundException fnfe) 
                {
                    // at least on Windows, some temp files raise this exception
                    // with an "access denied" message; checking if the file can
                    // be read doesn't help
                    return;
                }
                if (twitter)
                {
                    // the Twitter analyzer requires JSON files
                    if (pathName.toString().toLowerCase().endsWith(".json")) {
                        indexTweetsFromFile(writer, pathName, fis, opts);
                    }
                    else
                        System.out.println("Skipping non-JSON file " + pathName.getPath());
                }
                else
                {
                    indexFile(writer, pathName, fis, opts);
                }
                
                fis.close();
            }
        }
    }
}
