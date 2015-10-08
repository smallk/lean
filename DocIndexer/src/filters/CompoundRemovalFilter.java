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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/*
 * Removes compound tokens tagged for removal by the TokenSplitterFilter, as well
 * as any zero length tokens that have entered the stream.
 * 
 * @author Richard Boyd
 */
public class CompoundRemovalFilter extends FilteringTokenFilter
{
    private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    
    // single punctuation character
    private final Pattern regexPunct = Pattern.compile("\\A[\\p{P}]\\Z");
    
    public CompoundRemovalFilter(TokenStream in)
    {
        super(in);
    }
    
    @Override
    protected boolean accept()
    {                
        // get the part-of-speech tag for the current token
        String posTag = typeAttribute.type();                

        // diacard the token if tagged for discard by the TokenSplitterFilter
        if (posTag.equals(TokenSplitterFilter.DISCARD_TAG))
            return false;
        
        String token = termAttribute.toString();
        
        // remove any zero-length tokens
        if (token.isEmpty())
            return false;
        
        // remove any isolated punctuation chars that may have emerged
        if (1 == token.length())
        {
            Matcher punctMatcher = regexPunct.matcher(token);
            if (punctMatcher.matches())
                return false;
        }
        
        return true;
    }
}
