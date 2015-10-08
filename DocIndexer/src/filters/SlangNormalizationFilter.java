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

import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 *
 * Normalize slang and common Twitter abbreviations.  Must come after
 * the lowercase filter in the token filter chain.
 *
 * @author Richard Boyd
 */
public class SlangNormalizationFilter extends TokenFilter
{    
    private final CharTermAttribute termAttribute;
    private final TypeAttribute typeAttribute;
    private final HashMap<String, String> hashtable;
        
    // match tokens such as 2lbs, 2lecture, 2mor-row, etc.
    private final Pattern regexLeading2 = Pattern.compile("\\A2([\\-\\w]+)\\Z");  
    
    // match tokens such as 'w/me', 'w/all', etc.
    private final Pattern regexWith = Pattern.compile("\\Aw/([\\-\\w]+)\\Z");
    
    public SlangNormalizationFilter(TokenStream in, 
                                    HashMap<String, String> slangTable)
    {
        super(in);
        hashtable = slangTable;
        termAttribute = addAttribute(CharTermAttribute.class);
        typeAttribute = addAttribute(TypeAttribute.class);
    }
    
    // write a new token into the termAttribute buffer after lookup
    private void updateTermAttribute(String token)
    {
        if ( (null != token) && (token.length() > 0))
        {
            if (hashtable.containsKey(token))
                token = hashtable.get(token);
            
            termAttribute.copyBuffer(token.toCharArray(), 0, token.length());
            termAttribute.setLength(token.length());            
        }        
    }
    
    @Override
    public boolean incrementToken() throws IOException
    {
        if (!input.incrementToken())
            return false;

        String token = termAttribute.toString();
        if ( (null != token) && (token.length() > 0))
        {
            if (hashtable.containsKey(token))
            {            
                // found token in map; replace and set new length
                String newToken = hashtable.get(token);        

                termAttribute.copyBuffer(newToken.toCharArray(), 0, newToken.length());
                termAttribute.setLength(newToken.length());
            }
            else
            {
                // Check to see if this token is of the form '2ne1', '2do', etc.
                // If so, strip the leading '2' from such tokens.  The 'to' is 
                // a stop word; the second token might be slang, so do another 
                // lookup on it. Do not do this for numerals.
                if (-1 != token.indexOf("2"))
                {
                    if (!TweetNLPTagFilter.NUMERAL_TAG.equals(typeAttribute.type()))
                    {
                        Matcher twoMatcher = regexLeading2.matcher(token);
                        if (twoMatcher.matches())
                        {
                            String match = twoMatcher.group(1);
                            updateTermAttribute(match);
                        }
                    }
                }

                // Look for tokens of the form 'w/all', 'w/music', etc.  Strip the
                // stop word 'with' (symbolized by the w/) and lookup the remaining
                // token.
                if (-1 != token.indexOf("w/"))
                {
                    Matcher withMatcher = regexWith.matcher(token);
                    if (withMatcher.matches())
                    {
                        String match = withMatcher.group(1);
                        updateTermAttribute(match);
                    }
                }
            }
        }
        
        return true;
    } 
}
