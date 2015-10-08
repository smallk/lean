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

package twitter;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexWriter;

/**
 * Custom Utils class with addDocument function.
 * 
 * @author Ashley Beavers
 */
public interface Utils {
    /**
    * Add the document to the Lucene index.
    * <p>
    * This method controls whether or not a particular document is added to the Lucene index
    * and controls what fields in the index are associated with that document.
    *
    * @param    r               the string with the document text
    * @param    path            source location for this document
    * @param    writer          writer to use to write the index
    * @throws   IOException     if there is an issue with the document
    */    
    public static void addDocument(String r, String path, IndexWriter writer) throws IOException{
        // each tweet is a new Lucene document
        Document doc = new Document();
        DBObject tweetJSON = (DBObject)JSON.parse(r);
        String tweet = tweetJSON.get("text").toString();

        // Add the contents of the tweet to a field named "contents".
        // Specify a reader so that the text of the file is tokenized
        // and indexed, but not stored.  Note that FileReader expects
        // the file to be in UTF-8 encoding.  If that's not the case,
        // searches for special characters will fail.
        FieldType contentOps = new FieldType();
        contentOps.setIndexed(true);
        contentOps.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS);
        contentOps.setStored(false);
        contentOps.setStoreTermVectors(true);
        contentOps.setTokenized(true);                        
        Field contentField = new Field("contents", tweet, contentOps);                    
        doc.add(contentField);

        // Add the path of the file relative to the input dir as a
        // field named "path".  This will be used as a unique id
        // for the file.  Use a field that is indexed
        // (i.e. searchable), but don't tokenize the field into
        // separate words and don't index term frequency or
        // positional information.
        Field pathField = new StringField("path", path, Field.Store.YES);
        doc.add(pathField);

        // new index, so we just add the document
        // (no old document can be present)
        writer.addDocument(doc);
    }
}
