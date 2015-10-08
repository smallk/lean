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

import filters.SlangNormalizationFilter;
import filters.RepeatedCharFilter;
import filters.TokenSplitterFilter;
import filters.TweetNLPTagFilter;
import filters.UnicodeCleanupFilter;
import filters.SpellingCorrectionFilter;
import filters.CompoundRemovalFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.util.Version;
import java.util.HashMap;
import java.io.Reader;
import java.io.IOException;
import lucenetools.TokenOptions;
import filters.TokenSplitterEngine;
import filters.TrimFilter;
import org.apache.lucene.analysis.util.CharArraySet;

/**
 * This class is a custom Analyzer for Twitter data.
 * 
 * @author Richard Boyd
 */
public class TwitterAnalyzer extends Analyzer
{    
    // CMU Twitter model file
    private String modelFile = null;        
    private CharArraySet stopWordSet = null;
    HashMap<String, String> spellingHashtable;
    HashMap<String, String> slangHashtable;
    TokenOptions tokenOpts;
    public TwitterAnalyzer(String modelFileIn,
                           CharArraySet stopSet,
                           HashMap<String, String> spellingTable,
                           HashMap<String, String> slangTable,
                           TokenOptions tokenOptions) throws IOException
    {
        modelFile = modelFileIn;
        if (null == modelFile)
            throw new IllegalArgumentException("A model file must be specified.");
        stopWordSet = stopSet;
        spellingHashtable = spellingTable;
        slangHashtable = slangTable;
        tokenOpts = tokenOptions;
    }
    
    /**
    * Define how tokens are processed.
    *
    * @param    fieldName    required input
    * @param    reader       reader for document
    */
    @Override
    protected TokenStreamComponents createComponents(final String fieldName, final Reader reader)
    {
        Tokenizer tokenizer = new TwitterTokenizer(reader, modelFile, tokenOpts);        
        TokenStream chain = tokenizer;
        
        if (!tokenOpts.disableAllFilters)
        {
            // the chain of token filters...

            // throw out noise tokens as identified by the TweetNLP tagger
            chain = new TweetNLPTagFilter(chain, tokenOpts);

            // convert all letters to lowercase
            chain = new LowerCaseFilter(chain);        

            // replace accented chars with non-accented ASCII equivalents
            chain = new ASCIIFoldingFilter(chain);            
            
            // remove stop words
            chain = new StopFilter(chain, stopWordSet);

            // replace letters repeated more than three times
            chain = new RepeatedCharFilter(chain);      

            // trim strange chars from start and end of tokens
            chain = new UnicodeCleanupFilter(chain);        

            // trim any leading and trailing punctuation or other invalid chars
            chain = new TrimFilter(chain);

            // convert slang to standard English
            if (!slangHashtable.isEmpty())
                chain = new SlangNormalizationFilter(chain, slangHashtable);                  

            // At this point, all slang not contained in compound tokens
            // (such as /anarchy/peace/freedom) has been corrected.  Split
            // the compound tokens, then trim and collect any slang that
            // emerges.  Correct all misspellings at the end.
            
            // split compound tokens and replace with individual words;
            // must occur after the cleanup filter because adjacent explicit 
            // codepoints (\u2014\u2015) would be split on the backslash
            chain = new TokenSplitterFilter(chain, new TokenSplitterEngine());

            // remove compound tokens that were previously split
            chain = new CompoundRemovalFilter(chain);

            // trim any leading and trailing punctuation or other invalid chars
            chain = new TrimFilter(chain);
            
            // convert slang to standard English
            if (!slangHashtable.isEmpty())
                chain = new SlangNormalizationFilter(chain, slangHashtable);                  

            // remove 'apostrophe s' at the end of words
            chain = new EnglishPossessiveFilter(Version.LATEST, chain);        

            // spelling correction
            if (!spellingHashtable.isEmpty())
                chain = new SpellingCorrectionFilter(chain, spellingHashtable);

            // run the stop filter again to clean up any new stop words
            chain = new StopFilter(chain, stopWordSet);
            
            if (!tokenOpts.disableStemming)
            {
                // Krovets stemmer (smarter than the Porter stemmer)
                chain = new KStemFilter(chain);
            }        
        }
        
        return new TokenStreamComponents(tokenizer, chain);
    }
}


