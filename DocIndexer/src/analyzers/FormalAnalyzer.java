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

import filters.SpellingCorrectionFilter;
import filters.StandardTagFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.util.Version;
import java.io.Reader;
import java.io.IOException;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import java.util.HashMap;
import lucenetools.TokenOptions;

/**
 *This class contains a custom analyzer for formal documents.
 *
 * @author Richard Boyd
 */
public class FormalAnalyzer extends Analyzer
{        
    private CharArraySet stopWordSet = null;
    HashMap<String, String> spellingHashtable;
    private final TokenOptions tokenOpts;

    /**
    * Create the formal analyzer.
    *
    * @param    stopSet       user-provided set of stopwords, if any
    * @param    spellTable    user-provided set of spelling corrections, if any
    * @param    tokenOptions  options for token processing from the config file
    * @throws   IOException   cannot load files
    */
    public FormalAnalyzer(CharArraySet stopSet, 
                          HashMap<String, String> spellTable,
                          TokenOptions tokenOptions) throws IOException
    {
        stopWordSet = stopSet;
        spellingHashtable = spellTable;
        tokenOpts = tokenOptions;
    }
    
    /**
    * Define how tokens are processed.
    *
    * @param    fieldName    required input
    * @param    reader       reader for document
    */
    @Override
    protected Analyzer.TokenStreamComponents createComponents(final String fieldName, final Reader reader)
    {        
        Tokenizer tokenizer = new StandardTokenizer(reader);        
        TokenStream chain = tokenizer;
        
        if (!tokenOpts.disableAllFilters)
        {        
            // the chain of token filters...
            
            chain = new StandardFilter(chain);
            
            // discard tokens based on their type attribute
            chain = new StandardTagFilter(chain, tokenOpts);

            // convert tokens to lowercase
            chain = new LowerCaseFilter(chain);

            // replace accented chars with non-accented ASCII equivalents
            chain = new ASCIIFoldingFilter(chain);            
            
            // remove stop words (must come after lowercasing)
            chain = new StopFilter(chain, stopWordSet);                

            // remove 's
            chain = new EnglishPossessiveFilter(Version.LATEST, chain);                

            // spelling correction            
            if (!spellingHashtable.isEmpty())
                chain = new SpellingCorrectionFilter(chain, spellingHashtable);
                        
            if (!tokenOpts.disableStemming)
            {
                // Krovets stemmer (smarter than the Porter stemmer)
                chain = new KStemFilter(chain);
            }
        }
                
        return new Analyzer.TokenStreamComponents(tokenizer, chain);
    }
    
}
