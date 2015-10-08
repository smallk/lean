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

import java.util.HashMap;
import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Use default or user-provided spelling correction file to update tokens.
 *
 * @author richardboyd
 */
public class SpellingCorrectionFilter extends TokenFilter
{
    private final CharTermAttribute termAttribute;
    private final HashMap<String, String> hashtable;    
    
    public SpellingCorrectionFilter(TokenStream in, HashMap<String, String> spellingTable)
    {
        super(in);
        termAttribute = addAttribute(CharTermAttribute.class);
        
        hashtable = spellingTable;
    }
 
    @Override
    public boolean incrementToken() throws IOException
    {
        if (!input.incrementToken())
            return false;
        
        String token = termAttribute.toString();
        
        if (hashtable.containsKey(token))
        {            
            String newToken = hashtable.get(token);
            
            termAttribute.copyBuffer(newToken.toCharArray(), 0, newToken.length());
            termAttribute.setLength(newToken.length());
        }
                
        return true;
    }    
}
