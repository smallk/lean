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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This filter removes unicode codepoints for certain punctuation, all emoji, 
 * and various other unhelpful unicode elements.
 * 
 * @author Richard Boyd
 */
public class UnicodeCleanupFilter extends TokenFilter
{
    private final CharTermAttribute termAttribute;
        
    // These regexes assume that the tokens have already been lower-cased.
    // The Lucene AsciiFoldingFilter handles conversion of actual Unicode
    // chars to ASCII equivalent, if any exist.
    
    // match explicit unicode dash codepoints \u2012 - \u2015
    // replace with ascii hyphen/minus '-'
    private final Pattern regexDash = Pattern.compile("\\\\u201[2-5]");
    private final String ASCII_HYPHEN = "-";
    
    // match explicit unicode quotation codepoints \u2018 - \u201f
    // replace with ASCII apostrophe '
    private final Pattern regexQuotes = Pattern.compile("\\\\u201[8-9a-f]");
    private final String ASCII_APOSTROPHE = "'";
    
    // match all explicit unicode punctuation codepoints (\u2000 - \u206f)
    // replace with empty string; run this after dash replacement
    private final Pattern regexUnicodePunct = Pattern.compile("\\\\u20[0-6][0-9a-f]");
    private final String EMPTY_STRING = "";
    
    // match all explicit Unicode superscripts and subscripts \x{2070}-\x{209f}
    private final Pattern regexSuperSub = Pattern.compile("\\\\u20[7-9][0-9a-f]");
    
    // match all explicit Unicode control chars \u0000-\u001F, \u007F-\u009F
    private final Pattern regexCtrl1 = Pattern.compile("\\\\u00[0-1][0-9a-f]");
    private final Pattern regexCtrl2 = Pattern.compile("\\\\u00(7f|[8-9][0-9a-f])");
        
    // invisible control chars and unused control points
    private final Pattern regexControl = Pattern.compile("\\p{C}");
    
    // match all explicit emoji codepoints; will replace with empty string
    
    // Range: 1F300-1F64F
    private final Pattern regexEmoji1 = Pattern.compile("[\\x{1f300}-\\x{1f64f}]");    
    // Range: 1F680-1F6FF
    private final Pattern regexEmoji2 = Pattern.compile("[\\x{1f680}-\\x{1f6ff}]");        
    // Range: 2600-27BF
    private final Pattern regexEmoji3 = Pattern.compile("[\\x{2600}-\\x{27bf}]");
    
    // match the backquote character
    private final Pattern regexBackquote = Pattern.compile("\\A([^`]+)[`]([^`]+)\\Z");
    
    // currency amounts, such as US$200, CAN$2000.37
    // 0x24 = dollar sign; 0xA2 = cent; 0xA3 = British Pound; 0xA4 = currency sign; 0xA5 = Yen sign
    private final Pattern regexCurrency = Pattern.compile("\\A[a-z]+([\\x{0024}\\x{00A2}\\x{00A3}\\x{00A4}\\x{00A5}][0-9,.]+)\\Z");
    
    public UnicodeCleanupFilter(TokenStream in)
    {
        super(in);
        termAttribute = addAttribute(CharTermAttribute.class);
    }
    
    @Override
    public boolean incrementToken() throws IOException
    {
        if (!input.incrementToken())
            return false;
       
        String token = termAttribute.toString();
                        
        boolean matched = false;
        
        // Note: these transformations could produce zero-length tokens.  
        // Zero-length tokens will be removed by a subsequent filter.

        // replace backquotes with apostrophe
        Matcher backquoteMatcher = regexBackquote.matcher(token);
        if (backquoteMatcher.matches())
        {
            // replace backquote with apostrophe
            token = backquoteMatcher.replaceAll("$1'$2");
            matched = true;
        }        
        
        // keep the currency sign associated with the amount
        Matcher currencyMatcher = regexCurrency.matcher(token);
        if (currencyMatcher.matches())
        {
            token = currencyMatcher.group(1);
            matched = true;
        }
        
        // Scan the token for the presence of explicit unicode codepoints.
        // If found, run the regex replacements.
        if (-1 != token.indexOf("\\u"))
        {            
            // replace all explict dashes with ascii hyphen
            Matcher m2014 = regexDash.matcher(token);
            token = m2014.replaceAll(ASCII_HYPHEN);        
                
            // replace all explicit quotation marks with '
            Matcher mQuotes = regexQuotes.matcher(token);
            token = mQuotes.replaceAll(ASCII_APOSTROPHE);
            
            // remove all explicit punctuation codepoints
            Matcher mPunct = regexUnicodePunct.matcher(token);
            token = mPunct.replaceAll(EMPTY_STRING);
                
            // remove all explicit superscripts and subscripts
            Matcher mSuperSub = regexSuperSub.matcher(token);
            token = mSuperSub.replaceAll(EMPTY_STRING);
            
            // remove explicit control points
            Matcher mCtrl1 = regexCtrl1.matcher(token);
            token = mCtrl1.replaceAll(EMPTY_STRING);
            
            Matcher mCtrl2 = regexCtrl2.matcher(token);
            token = mCtrl2.replaceAll(EMPTY_STRING);
            
            matched = true;            
        }        

        //remove all emoji icons
        Matcher mEmoji1 = regexEmoji1.matcher(token);
        if (mEmoji1.matches())
        {
            token = mEmoji1.replaceAll(EMPTY_STRING);
            matched = true;
        }
        
        Matcher mEmoji2 = regexEmoji2.matcher(token);
        if (mEmoji2.matches())
        {
            token = mEmoji2.replaceAll(EMPTY_STRING);
            matched = true;
        }
        
        Matcher mEmoji3 = regexEmoji3.matcher(token);
        if (mEmoji3.matches())
        {
            token = mEmoji3.replaceAll(EMPTY_STRING);
            matched = true;
        }
        
        // strip invisible control and other chars
        Matcher controlMatcher = regexControl.matcher(token);
        if (controlMatcher.matches())
        {
            token = controlMatcher.replaceAll(EMPTY_STRING);
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
