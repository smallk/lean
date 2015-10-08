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

package filters;

import lucenetools.TokenOptions;
import org.apache.lucene.analysis.util.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * Removes tokens according to their part-of-speech tag.
 * 
 * @author Richard Boyd
 */
public class TweetNLPTagFilter extends FilteringTokenFilter
{
    public final static String PRONOUN_TAG     = "O";
    public final static String PREPOSITION_TAG = "P";
    public final static String VERB_TAG        = "V";
    public final static String URL_TAG         = "U";
    public final static String NUMERAL_TAG     = "$";
    public final static String HASHTAG_TAG     = "#";
    public final static String AT_MENTION_TAG  = "@";
    
    // tokens having these tags are always ignored
    public final static char[] DEFAULT_TAGSET =
    {
        '&',  // coordinating conjunction
        '~',  // discourse marker, indications of continuation of a msg across multiple tweets
        'E',  // emoticon
        '!',  // interjection (often identifies emoji)
        ',',  // punctuation        
        'G'   // other abbreviations, foreign words, possessive endings, symbols, garbage
    };
    
    // lookup table for ASCII part of speech tag;
    // 1 == in table, so ignore tokens having this tag, 0 == not
    private final static int[] charMap = new int[128];
    
    private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
    
    public TweetNLPTagFilter(TokenStream in, TokenOptions tokenOpts)
    {
        super(in);
        
        for (int i=0; i<128; ++i)
            charMap[i] = 0;
        
        for (char c : DEFAULT_TAGSET)
        {
            ignoreTag(c);
        }
        
        if (tokenOpts.ignoreURLs)       {ignoreTag('U');}
        if (tokenOpts.ignoreNumerals)   {ignoreTag('$');}
        if (tokenOpts.ignoreHashtags)   {ignoreTag('#');}
        if (tokenOpts.ignoreAtMentions) {ignoreTag('@');}
    }
    
    private void ignoreTag(char c)
    {
        int asciiCode = (int)c;
        charMap[asciiCode] = 1;
    }
        
    @Override
    protected boolean accept()
    {
        // get the part-of-speech tag for the current token
        String type = typeAttribute.type();                
        
        // get ASCII code of the char
        int code = (int)type.charAt(0);
        
        // accept token if not in map
        return (0 == charMap[code]);
    }
}
