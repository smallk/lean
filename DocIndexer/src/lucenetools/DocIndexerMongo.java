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

import analyzers.DebugAnalyzer;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoTimeoutException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.Analyzer;

/**
 * This class controls indexing tweets from MongoDB.
 * 
 * @author Ashley Beavers
 */
public class DocIndexerMongo 
{
    // number of documents added to the Lucene index
    private static int docsIndexed = 0;    
    

    /**
     * This method extracts the keywords from the config parameters.
     * 
     * @param   kwString            csv string of keywrords
     * @return  ArrahList<String>   list of keywords
     */
    static ArrayList<String> getKeywords(String kwString) {
        //words to include and exclude
        ArrayList<String> keywords = new ArrayList<>();
        if (!"".equals(kwString)) {
            String[] keywordsBeforeTrim = kwString.split(",");
            for (String each : keywordsBeforeTrim) {
                keywords.add(each.trim());
            }
        }
        return keywords;
    }
    
    /**
     * This method returns the number of docs indexed.
     * 
     * @return int      number of docs indexed
     */
    static int getDocsIndexed() {
        return docsIndexed;
    }
    
    /**
     * This method creates the Mongo cursor object for the connection
     * details specified in the config file.
     * 
     * @param   opts                    options object
     * @return  DBCursor                mongo cursor
     * @throws  UnknownHostException    cannot connect to mongo host
     */
    static DBCursor getCursor(Options opts) throws UnknownHostException {
        MongoClient mongoClient = new MongoClient(opts.host, opts.port);
        DB db = mongoClient.getDB(opts.database);
        DBCollection coll = db.getCollection(opts.collection);
        DBCursor cursor = coll.find();
        return cursor;
    }
    
    /**
     * Determine if the words indicated are included or excluded
     * 
     * @param   text       text of tweet
     * @param   include    keywords to inlude
     * @param   exclude    keywords to exclude
     * @return  boolean    whether or not the keyword constraints are satisfied
     */
    static boolean eval(String text, ArrayList<String> include, ArrayList<String> exclude) {
        boolean in, ex;
        in = true;
        if (!include.isEmpty()) {
            in = false;
            for (String each : include) {
                if (text.contains(each)) {
                    in = true;
                    break;
                }
            }
        }
        ex = true;
        if (!exclude.isEmpty()) {
            ex = true;
            for (String each : exclude) {
                if (text.contains(each)) {
                    ex = false;
                    break;
                }
            }
        }
        return (in && ex);
    }
    
    /**
     * Construct a 'path' to identify this tweet.
     * 
     * @param   opts       options object
     * @return  String     'path' to identify this tweet
     */
    static String getPath(Options opts) {
        return opts.host + "_" + opts.port + "_" + opts.database + "_" + opts.collection;
    }

    /**
    * Indexes the given Mongo collection using the given writer. Each tweet
    * will be indexed as a separate document.
    *
    * @param    writer                      writer to use for indexing documents
    * @param    opts                        options object
    * @param    analyzer                    analyzer specified from config file
    * @throws   MongoTimeoutException       if the mongo instance cannot be connected to
    * @throws   IOException                 if there is an issue with the document
    * @throws   NoSuchMethodException       if method not found
    * @throws   IllegalAccessException      cannot access class
    * @throws   IllegalArgumentException    wrong set of arguments
    * @throws   InvocationTargetException   instantiated class has a problem
    */    
    static void indexDocs(IndexWriter writer, Options opts, Analyzer analyzer)
            throws MongoTimeoutException, IOException, NoSuchMethodException, 
            IllegalAccessException, IllegalArgumentException, InvocationTargetException 
    {
        DBCursor cursor = getCursor(opts);
        boolean noLimit = false;   
        int origLimit = opts.limit;
        if (-1 == opts.limit) {
            noLimit = true;
            opts.limit = 1;
        }
        ArrayList<String> include = getKeywords(opts.include);
        ArrayList<String> exclude = getKeywords(opts.exclude);
        String tweet, tweetLower, tweetIndex;
        try {
            if (cursor.size() == 0) {
                throw new UnsupportedOperationException();         
            }
            else {
                for (int i = 0; i < opts.limit; i++) {
                    if (cursor.hasNext()) {
                        // read the next tweet from mongo
                        DBObject tweetJSON = cursor.next();
                        tweet = tweetJSON.get("text").toString();
                        tweetLower = tweet.toLowerCase();
                        if (eval(tweetLower, include, exclude)) {
                            // the tweet index is the MongoID associated with that tweet
                            tweetIndex = tweetJSON.get("_id").toString();
                            // identify the db path to retrieve the tweet
                            String path = getPath(opts) + "_" + tweetIndex;
                            // use the analyzer's package name to find out if there is a custom
                            // implementation of the Utils class with the addDocument function
                            String packageName = opts.analyzerName.split("\\.")[0];
                            try {
                                Class<?> cla = Class.forName(packageName + ".Utils");
                                Method method = cla.getMethod("addDocument", String.class, String.class, IndexWriter.class);
                                method.invoke(null, tweetJSON.toString(), path, writer);
                            // use the default Utils implementaiton
                            } catch (ClassNotFoundException | NoSuchMethodException cnfe) {
                                Utils.addDocument(tweetJSON.toString(), path, writer);
                            }
                            ++docsIndexed;
                            if (0 == (docsIndexed % 1000))
                            {
                                System.out.print("Docs analyzed and indexed: " + docsIndexed + "\r");
                                System.out.flush();
                            }
                            // increment limit to emulate no limit being used
                            if (noLimit) {
                                ++opts.limit;
                            }
                        } else { //ensure that the limit is reached 
                            ++opts.limit;
                        }
                    } else {
                        if (origLimit != -1)
                            System.out.println("Maximum available documents is " + (i + 1) + 
                                    "; unable to reach desired limit of " + origLimit + ".");
                        break;
                    }                                   
                }
            }
            if (origLimit != -1)
                System.out.printf("Searched %d tweets to find %d that met the filter requirements.\n",
                        opts.limit-1, origLimit);
        } catch(MongoTimeoutException mte) {
            System.out.println("Timed out while waiting to connect." +
                    "\nEnsure that the host/port/database/collection " +
                    "configuration in your YAML file is correct.");
        } catch(UnsupportedOperationException uoe) {
            System.out.println("\nWARNING: Cursor returned with 0 documents - " +
                    "ensure that the database/collection \n" +
                    "configuration in your YAML file is correct.");
        }
        finally {
            cursor.close();
        }                
                          
    }
    
    /**
    * Show analysis for tweets
    *
    * @param    n             number of tweets to show the analysis for
    * @param    opts          options object
    * @param    analyzer      analyzer specified from config file
    * @throws   IOException   if there is an issue with the document
    */    
    public static void showTweetAnalysis(int n, Options opts, Analyzer analyzer) throws IOException
    {        
        DBCursor cursor = getCursor(opts);
        boolean noLimit = false;
        if (-1 == opts.limit) {
            noLimit = true;
            opts.limit = 1;
        }
        ArrayList<String> include = getKeywords(opts.include);
        ArrayList<String> exclude = getKeywords(opts.exclude);
        System.out.println("Analysis results for Mongo client" +
                ", " + n + " tweets max:\n");
        int count = 0;
        String tweet, tweetLower;
        try {
            if (cursor.size() == 0) {
                throw new UnsupportedOperationException();         
            }
            else {

                for (int i = 0; i < opts.limit; i++) {
                    if (cursor.hasNext()) {
                        // read the next tweet from mongo
                        DBObject tweetJSON = cursor.next();
                        tweet = tweetJSON.get("text").toString();
                        tweetLower = tweet.toLowerCase();
                        //check for keywords
                        if (eval(tweetLower, include, exclude)) {
                            System.out.println(tweet);
                            DebugAnalyzer.showAnalysisFromStream(new StringReader(tweet), analyzer);
                            System.out.println();
                            ++count;
                            if (count >= n) {
                                System.out.printf("Searched %d tweets to find %d that met the filter requirements.\n",
                                    i + 1, n);
                                break;
                            }
                            if (noLimit) 
                                ++opts.limit;
                        } else {
                            ++opts.limit;
                        }
                    }
                }
            }
        } catch(MongoTimeoutException mte) {
            System.out.println("Timed out while waiting to connect." +
                    "\nEnsure that the host/port/database/collection " +
                    "configuration in your YAML file is correct.");
        } catch(UnsupportedOperationException uoe) {
            System.out.println("\nWARNING: Cursor returned with 0 documents - " +
                    "ensure that the database/collection \n" +
                    "configuration in your YAML file is correct.");
        } finally {
            cursor.close();
        }   
    }
}
