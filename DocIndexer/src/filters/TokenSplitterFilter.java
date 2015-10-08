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
import java.util.ArrayList;
import java.util.Stack;
import java.util.EmptyStackException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 * This class adapted from the book Lucene In Action by Michael McCandless, 
 * Erik Hatcher, Otis Gospodnetic, pages 131-134.
 *
 * @author Ashley Beavers
 * 
 */
public class TokenSplitterFilter extends TokenFilter 
{
    public static final String TOKEN_TYPE_SYNONYM = "SYNONYM";
    public static final String DISCARD_TAG = "q";
    
    private Stack<String> synonymStack, tempStack;
    private SynonymEngine engine;
    private AttributeSource.State current;
    private final CharTermAttribute termAtt;
    private final TypeAttribute typeAtt;
    private final PositionIncrementAttribute posIncrAtt;
    private final OffsetAttribute offsetAtt;
    String posTag;
    String compoundToken;
    int position, searchPos, startOffset, endOffset;
    private char[] charArray;
    
    public TokenSplitterFilter(TokenStream in, SynonymEngine engine) 
    {
        super(in);
        synonymStack = new Stack<String>();
        this.engine = engine;
        this.termAtt = addAttribute(CharTermAttribute.class);
        this.typeAtt = addAttribute(TypeAttribute.class);
        this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
        this.offsetAtt = addAttribute(OffsetAttribute.class);
    }
    
    public boolean incrementToken() throws IOException 
    {
        if (synonymStack.size() > 0) 
        {
            String syn = synonymStack.pop();
            restoreState(current);
            charArray = syn.toCharArray();
            termAtt.copyBuffer(charArray, 0, syn.length());
            typeAtt.setType(posTag);
            
            // find the position of the synonym in the compound token; begin
            // searching at 'searchPos'
            int pos = compoundToken.indexOf(syn, searchPos);
            if (-1 == pos)
                throw new IOException("SynonymFilter: synonym not found in compound token.");

            offsetAtt.setOffset(startOffset + pos, startOffset + pos + syn.length());
            posIncrAtt.setPositionIncrement(1);
            searchPos += syn.length();
            
            return true;
        }
        
        if (!input.incrementToken())
              return false;

        // save attributes of the compound token
        compoundToken = termAtt.toString();
        posTag = typeAtt.type();
        position = posIncrAtt.getPositionIncrement();
        startOffset = offsetAtt.startOffset();
        endOffset = offsetAtt.endOffset();
        searchPos = 0;
        
        // do not break apart URLs or numerals (which includes dates)
        if (! (posTag.equals(TweetNLPTagFilter.URL_TAG) || 
               posTag.equals(TweetNLPTagFilter.NUMERAL_TAG)))
        {        
            if (addAliasesToStack()) {
                
                // Set the part of speech tag of the compound token to the 
                // discard tag, so that it can be identified and removed
                // downstream.
                typeAtt.setType(DISCARD_TAG);            
                
                current = captureState();                
            }
        }
        
        return true;
    }

    private boolean addAliasesToStack() throws IOException 
    {
        ArrayList<String> synonyms = engine.getSynonyms(termAtt.toString());
        
        if (synonyms.isEmpty()) {
            return false;   
        }
        
        for (String synonym : synonyms) {
            synonymStack.push(synonym);
        }
        return true;
    }    
}