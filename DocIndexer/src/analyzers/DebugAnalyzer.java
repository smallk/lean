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

package analyzers;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lucenetools.ErrorReporter;
import twitter.TwitterTextValueReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author Richard Boyd
 */
public class DebugAnalyzer 
{
    // character encoding of the source documents
    private static Charset charset = StandardCharsets.UTF_8;
    
    private static final int MAX_CHARS = TwitterTextValueReader.DEFAULT_BUFFER_SIZE;
    private static final char[] cbuf = new char[MAX_CHARS];    
        
    private static int counter = 0;
    
    /**
    * This method outputs token-by-token analysis of documents.
    *
    * @param    reader        the reader for the documents
    * @param    analyzer      the analyzer 
    * @throws   IOException   cannot load stream
    */
    public static void showAnalysisFromStream(Reader reader, Analyzer analyzer) throws IOException 
    {
        TokenStream stream = analyzer.tokenStream("text", reader);
        CharTermAttribute cta = stream.addAttribute(CharTermAttribute.class);
        OffsetAttribute oa = stream.addAttribute(OffsetAttribute.class);
        TypeAttribute typeAtt = stream.addAttribute(TypeAttribute.class);
        
        
        try
        {
            stream.reset();
            while (stream.incrementToken())
            {
                // get starting and ending offsets
                int start = oa.startOffset();
                int end   = oa.endOffset();

                // text of the token
                String token = cta.toString();

                // part of speech tag for the token
                String tag = typeAtt.type();

                System.out.printf("start: %4d\tend: %4d\tlength: %4d\ttag: %s\ttoken: %s\n",
                        start, end, token.length(), tag, token);   
            }            
        }
        finally
        {
            stream.close();
        }         
    }
    
    /**
    * This method begins the document analysis output for files.
    *
    * @param    n             the number of documents to analyze
    * @param    inputDir      input directory for documents
    * @param    analyzer      the analyzer 
    * @throws   IOException   cannot open file
    */
    public static void showDocAnalysis(int n, String inputDir, Analyzer analyzer) throws IOException
    {
        counter = 0;        
        boolean isTwitter = false;
        File pathName = new File(inputDir);
        showDocAnalysisRecursive(n, isTwitter, pathName, analyzer);
    }    
    
    /**
    * This method begins the document analysis output for tweets.
    *
    * @param    n             the number of documents to analyze
    * @param    inputDir      input directory for tweets JSON
    * @param    analyzer      the analyzer 
    * @throws   IOException   cannot open file
    */
    public static void showTweetAnalysis(int n, String inputDir, Analyzer analyzer) throws IOException
    { 
        counter = 0;
        boolean isTwitter = true;
        File pathName = new File(inputDir);
        showDocAnalysisRecursive(n, isTwitter, pathName, analyzer);
    }
    
    /**
    * This method extracts tweets from the JSON file
    *
    * @param    n             the number of tweets to analyze
    * @param    fis           stream for tweets
    * @param    jsonFile      JSON file
    * @param    analyzer      the analyzer 
    * @throws   IOException   cannot open file
    */
    public static void extractTweets(int n, 
                                     FileInputStream fis, 
                                     File jsonFile, 
                                     Analyzer analyzer) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
        TwitterTextValueReader reader = new TwitterTextValueReader(br);        
                
        while (true)
        {
            // read the next tweet from the file
            int tweetLen = reader.read(cbuf, 0, MAX_CHARS);
            if (-1 == tweetLen)
                break;
            
            DBObject tweetJSON = (DBObject)JSON.parse(new String(cbuf, 0, tweetLen));
            String tweet = tweetJSON.get("text").toString();

            System.out.printf("[%d]\t%s\n", (counter+1), tweet);
            
            showAnalysisFromStream(new StringReader(tweet), analyzer);
            
            System.out.println();
            
            ++counter;
            if (counter >= n)
                break;
        }       
    }    
            
    /**
    * This method recurses through the file directory to display analysis results.
    *
    * @param    n             the number of tweets to analyze
    * @param    isTwitter     whether or not these are twitter JSON files
    * @param    pathName      directory path
    * @param    analyzer      the analyzer 
    * @throws   IOException   cannot open file
    */
    private static void showDocAnalysisRecursive(int n, 
                                                 boolean isTwitter,
                                                 File pathName, 
                                                 Analyzer analyzer) throws IOException
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
                        showDocAnalysisRecursive(n, isTwitter, new File(pathName, files[i]), analyzer);
                        
                        // for twitter count individual tweets, not files
                        if (!isTwitter)                        
                            ++counter;
                        
                        if (counter >= n)
                            return;                        
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
                        
                if (isTwitter)
                {
                    // the Twitter analyzer requires JSON files
                    if (pathName.toString().toLowerCase().endsWith(".json"))
                    {
                        System.out.println("\nAnalysis results for file " + 
                                pathName.getPath() + ", " + n + " tweets max:\n");
 
                        extractTweets(n, fis, pathName, analyzer);
                    }
                    else
                    {
                        System.out.println("Skipping non-JSON file " + pathName.getPath());
                    }                    
                }
                else
                {
                    System.out.println("Analysis results for file " + 
                            (counter+1) + ": " + pathName.getPath() + ", " + 
                            n + " files max:\n");
                    
                    extractContent(fis, pathName, analyzer);
                }
                
                fis.close();
            }
        }               
    }        
    
    /**
    * This method extracts content from non-JSON files.
    *
    * @param    fis           stream for document content
    * @param    inputFile     filepath for document
    * @param    analyzer      the analyzer 
    * @throws   IOException   cannot open file
    */    private static void extractContent(FileInputStream fis, 
                                             File inputFile, 
                                             Analyzer analyzer) throws IOException
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
            System.out.println("Indexed " + counter + " documents.");
            System.out.println("Failed on file " + inputFile.toString());
            System.out.println("Exiting...");
            System.exit(-1);
        }        

        if (!parseOK)
        {
            System.out.println("Skipping file " + inputFile);
        }

        if (0 == contentHandler.toString().length())
        {
            System.out.println("\t*** Document format not supported. ***\n");
        }                
        else
        {
            StringReader reader = new StringReader(contentHandler.toString());
            System.out.print(contentHandler.toString());
            showAnalysisFromStream(reader, analyzer);
            System.out.println();
        }        
    }
}
