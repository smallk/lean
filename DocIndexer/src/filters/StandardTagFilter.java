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
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

/**
 * Removes tokens according to their part-of-speech tag.
 * 
 * @author Richard Boyd
 */
public class StandardTagFilter extends FilteringTokenFilter
{
    private final TokenOptions tokenOpts;    
    
    public static final String NUMERAL_TAG  = "<NUM>";
    public static final String ALPHANUM_TAG = "<ALPHANUM>";
    
    private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
    
    public StandardTagFilter(TokenStream in,
                             TokenOptions tokenOptions)
    {
        super(in);
        tokenOpts = tokenOptions;
    }
            
    @Override
    protected boolean accept()
    {
        // get the part-of-speech tag for the current token
        String type = typeAttribute.type();
        
        if (tokenOpts.ignoreAllButAlphanum)
            return (type.equals(ALPHANUM_TAG));        
        else if (tokenOpts.ignoreNumerals)
            return (!type.equals(NUMERAL_TAG));
        else
            return true;
    }
}
