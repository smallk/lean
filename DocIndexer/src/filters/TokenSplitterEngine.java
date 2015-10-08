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

import filters.SynonymEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class splits a compound token into its individual constituent tokens.
 * A compound token is a construct such as anarchy/peace/freedom, 
 * you&ellie&tim, etc.  In other words, a series of words separated by a
 * non-word character.  Compound tokens also include constructs such as
 * here4you, go2him, etc.  Compound tokens occur frequently in Twitter data.
 * 
 * This class implements a recursive compound token splitter, so it can handle
 * compound tokens of arbitrary complexity.  Constituent words are assumed to
 * be comprised of these characters: {a-z, -, '}.  The separator character is
 * anything other than these characters except for a period or a dollar sign.
 * Periods are used in abbreviations, so these are not split.  Dollar signs are
 * sometimes used as substitutes for the letter 's', as in u$a.
 * 
 * One consequence of this is that a proper name such as AT&T will be split
 * by this class into the tokens "AT" and "T".  Use of named entity recognition
 * will hopefully mitigate this problem in the future.
 * 
 * @author Richard Boyd
 */
public class TokenSplitterEngine implements SynonymEngine 
{
    private final ArrayList<String> synonyms = new ArrayList<>();
    
    // recursion depth
    int depth;
        
    // regocnize only digits
    private final Pattern onlyDigits = Pattern.compile("\\A[0-9]+\\Z");
    
    // recognize the leftmost word in a '24' token, such as run2here, there4you
    private final Pattern leftmost24 = Pattern.compile("\\A([a-z_\\-']+)[24](.+)\\Z");
    
    // Recognize the leftmost word in a compound token; the break character 
    // excludes '.' (used in abbreviations) and the '$' character, since 
    // sometimes a dollar sign is used as a substitute for the letter 's'.
    // This will break words on an '&' character, so 'coal&oil' will be split
    // into 'coal' and 'oil'.  One unfortunate consequence of this is that
    // 'AT&T' will also be split.  Need to add named entity recognition and 
    // prevent this.
    private final Pattern leftmost = Pattern.compile("\\A([a-z\\-']+)[^a-z0-9\\-'.$](.+)\\Z");
    
    private void getSynonymsRecursive(String token)
    {
        // try to match the leftmost part of a compound token
        Matcher m = leftmost.matcher(token);
        Matcher m24 = leftmost24.matcher(token);
        
        if (m.matches())
        {
            ++depth;
            synonyms.add(m.group(1));                  
            
            // group(2) contains the remaining part, still possibly compound
            token = m.group(2);
            getSynonymsRecursive(token);            
        }       
        else if (m24.matches())
        {
            // if only digits remain in the 2nd group, do not separate, since 
            // we're trying to break apart a number (such as g20)
            Matcher mDigits = onlyDigits.matcher(m24.group(2));
            if (!mDigits.matches())
            {
                ++depth;
                synonyms.add(m24.group(1));           
            
                token = m24.group(2);
                getSynonymsRecursive(token);
            }
        }
        else
        {
            // If depth > 0, we originally had a compund token, so need to
            // include the final rightmost piece.  If depth == 0, this is not
            // a compound token, so ignore it.
            if (depth > 0)
            {
                synonyms.add(token);
            }
        }
    }
    
    @Override
    public ArrayList<String> getSynonyms(String s)
    {
        depth = 0;
        synonyms.clear();                    
        
        getSynonymsRecursive(s);
        
        // The token splitter filter uses a stack, so reverse the order of the 
        // components here...
        Collections.reverse(synonyms);
        
        return synonyms;
    } 
}