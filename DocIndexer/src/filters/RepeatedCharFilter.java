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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import java.io.IOException;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 *
 * Replace all occurrences of three or more characters by a two char sequence.
 *
 * @author Richard Boyd
 */
public class RepeatedCharFilter extends TokenFilter
{    
    private CharTermAttribute termAttribute;
    private final TypeAttribute typeAtt;

    public RepeatedCharFilter(TokenStream in)
    {
        super(in);        
        this.termAttribute = addAttribute(CharTermAttribute.class);
        this.typeAtt = addAttribute(TypeAttribute.class);
    }
    
    @Override
    public boolean incrementToken() throws IOException
    {                         
        if (!input.incrementToken())
            return false;
        
        String posTag = typeAtt.type();
        
        // do not break apart URLs
        if (!posTag.equals("U")) 
        {        
            char[] buf = termAttribute.buffer();
            int len = termAttribute.length();

            if (len > 0)
            {
                char cur, prev = buf[0];
                int copies = 1, j=1;

                // i is the source char index, j is the dest char index
                for (int i=1; i<len; ++i)
                {
                    cur = buf[i];

                    if ( (cur != prev) || (!Character.isLetter(cur)))
                    {
                        buf[j++] = cur;
                        prev = cur;
                        copies = 1;
                    }
                    else if (copies < 2)
                    {
                        buf[j++] = cur;
                        ++copies;
                    }
                }

                termAttribute.setLength(j);
            }
        }
        
        return true;
    }
}
