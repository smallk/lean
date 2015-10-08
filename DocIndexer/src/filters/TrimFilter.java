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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * This filter trims any leading or trailing non-letter characters.
 *
 * @author Richard Boyd
 */
public class TrimFilter extends TokenFilter
{
    private final CharTermAttribute termAttribute;
    private final TypeAttribute typeAttribute;       
    
    // These regexes assume that the tokens have already been lower-cased.   
        
    // match any leading quote or other chars that can be safely stripped
    private final Pattern regexLeadingChars = Pattern.compile("\\A['\"\\p{Pi}_/\\-&|()\\^{`]+([^'\"\\p{Pi}_/\\-&|()\\^{`].*)\\Z");
    
    // match any trailing chars that can be safely stripped
    private final Pattern regexTrailingChars = Pattern.compile("\\A(.*?[^'\"\\p{Pf}_/\\-&|()\\^!$}`])['\"\\p{Pf}_/\\-&|()\\^!$}`]+\\Z");
    
    public TrimFilter(TokenStream in)
    {
        super(in);
        termAttribute = addAttribute(CharTermAttribute.class);
        typeAttribute = addAttribute(TypeAttribute.class);
    }
    
    @Override
    public boolean incrementToken() throws IOException
    {
        if (!input.incrementToken())
            return false;
       
        String match;
        String token = termAttribute.toString();
        String posTag  = typeAttribute.type();
                        
        boolean matched = false;
        
        // Note: these transformations could produce zero-length tokens.  
        // Zero-length tokens will be removed by a subsequent filter.
                
        // strip irrelevant leading chars
        Matcher leadingCharMatcher = regexLeadingChars.matcher(token);
        if (leadingCharMatcher.matches())
        {
            // text is in group 1
            match = leadingCharMatcher.group(1);
            if ( (null != match) && (match.length() > 0))
                token = match;
            
            matched = true;
        }
        
        // strip irrelevant trailing chars
        Matcher trailingCharMatcher = regexTrailingChars.matcher(token);
        if (trailingCharMatcher.matches())
        {
            // text is in group 1
            match = trailingCharMatcher.group(1);
            if ( (null != match) && (match.length() > 0))
                token = match;
            
            matched = true;
        }          
        if (matched)
        {
            termAttribute.copyBuffer(token.toCharArray(), 0, token.length());
            termAttribute.setLength(token.length());
        }
        
        return true;
    }    
}
