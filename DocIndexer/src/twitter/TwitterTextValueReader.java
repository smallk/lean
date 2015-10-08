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

package twitter;

import java.util.Stack;
import java.io.Reader;
import java.io.IOException;
/**
 * This class scans a raw Twitter JSON file, finds the "text" field in
 * each JSON object, and extracts the associated value string (the tweet text) 
 * on each call to read().  Subsequent calls to read() continue to extract
 * tweet text until EOF is reached.
 * 
 * @author Richard Boyd
 */
public class TwitterTextValueReader extends Reader
{
    // default StringBuilder buffer size, in characters
    public static final int DEFAULT_BUFFER_SIZE = 2048;
    private static final char CHAR_OPENBRACE   = '{';
    private static final char CHAR_CLOSEBRACE  = '}';
    private static final char CHAR_DOUBLEQUOTE = '"';
    private static final char CHAR_COLON       = ':';    
    private Reader reader;
    private Stack<Character> stack;
    private StringBuilder sbText;
    // offsets of the next tweet in the sbText buffer
    private int start, end;
    // index of this tweet in the file
    int tweetIndex;
    
    /**
     * Constructor.
     * 
     * @param in    reader
     */
    public TwitterTextValueReader(Reader in)
    {
        reader = in;
        init(DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Constructor.
     * 
     * @param in    reader
     * @param sz    size of buffer
     */
    public TwitterTextValueReader(Reader in, int sz)
    {
        if (sz <= 0)
            throw new IllegalArgumentException("sz argument must be > 0");
        reader = in;
        init(sz);
    }
    
    /**
     * Creates the Stack and StringBuilder with the specified size.
     * 
     * @param sz    size of buffer
     */
    private void init(int sz)
    {
        tweetIndex = 0;
        stack = new Stack<>();
        sbText = new StringBuilder(sz);
    }
    
    /**
     * Return the index of the tweet.
     * 
     * @return int    index of tweet
     */
    public int getTweetIndex()
    {
        return tweetIndex;
    }
    
    /**
     * Close the reader.
     * 
     * @throws IOException      issue with reader
     */
    @Override
    public void close() throws IOException
    {
        reader.close();
    }
    
    /**
     * Read the next item from the file.
     * 
     * @param cbuf   character buffer
     * @param off    offset 
     * @param len    allowed length of item
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException
    {
        // loads the entire "text" value string into the buffer, 
        // unless limited by the len argument
        
//        int tweetLength = nextTweetText();
        int tweetLength = nextTweetJSON();
        if (tweetLength <= 0)
            return -1;
        
        // copy into the provided buffer
        int maxChars = Math.min(tweetLength, len);
        for (int c=0; c<maxChars; ++c)
            cbuf[off + c] = sbText.charAt(c);

        ++tweetIndex;
        sbText.setLength(0);
        return maxChars;
    }
    
    /**
     * Read the next character.
     * 
     * @throws IOException      issue with reader
     */
    private char nextChar() throws IOException
    {
        int charVal = reader.read();
        char c = (char)charVal;
        return c;
    }    

    /**
     * Extract the next JSON tweet object 
     * 
     * @throws IOException      issue with reader
     */
    private int nextJSONObject() throws IOException
    {
        start = -1;
        
        // skip leading whitespace chars
        char c = nextChar();
        while (Character.isWhitespace(c))
            c = nextChar();

        if (CHAR_OPENBRACE != c)
        {
            // EOF returns -1, which is OK
            if ( (char)-1 != c)
            {
                String msg = "expected '{', got " + c;
                throw new IOException(msg);
            }
           
            // at EOF
            //return null;
            return -1;
        }
        sbText.append(c);
        stack.clear();
        stack.push(CHAR_OPENBRACE);

        // scan to find the closing '}' outside of a quoted context
        boolean objComplete = false;
        boolean inQuotes = false;
        boolean isField  = true;
        boolean foundTextField = false;
        while (!objComplete)
        {
            c = nextChar();
            
            // ignore whitespace outside of a quoted context
            if (!inQuotes && Character.isWhitespace(c))
                continue;
            
            sbText.append(c);
            switch (c)
            {
                case CHAR_DOUBLEQUOTE:
                    // ignore embedded quotes, which must be escaped;
                    // the quote char has already been appended to the 
                    // sbText, so need len()-2
                    if ('\\' != sbText.charAt(sbText.length()-2))
                        inQuotes = !inQuotes;
                    break;
                case CHAR_OPENBRACE:
                    if (!inQuotes)
                        stack.push(CHAR_OPENBRACE);
                    break;
                case CHAR_CLOSEBRACE:
                    if (!inQuotes)
                    {
                        stack.pop();  

                        // completed another JSON object if empty
                        if (stack.empty())
                            objComplete = true;
                    }
                    break;
                case CHAR_COLON:
                    // divider between field and value
                    if (!inQuotes)
                        isField = !isField;

                    // Look for the tweet text following the "text": field,
                    // but only if "text": is a field name.  Check for
                    // !isField, since the colon has already been seen, and
                    // thus the transition between field and value has
                    // already occurred.
                    if (!foundTextField && !isField)
                    {
                        // buf must contain at least these 8 chars: {"text":
                        int len = sbText.length();
                        if (len >= 8)
                        {
                            // ':' is at offset len-1
                            if ( ('"' == sbText.charAt(len-2)) &&
                                 ('t' == sbText.charAt(len-3)) &&
                                 ('x' == sbText.charAt(len-4)) &&
                                 ('e' == sbText.charAt(len-5)) &&
                                 ('t' == sbText.charAt(len-6)) &&
                                 ('"' == sbText.charAt(len-7)))
                            {
                                foundTextField = true;
                                start = len;  // opening quote
                            }
                        }
                    }
                    break;
            }               
        }
        return start;
    }

    /**
     * Return the entire JSON tweet object.
     * 
     * @throws IOException      issue with reader
     */
    private int nextTweetJSON() throws IOException
    {
        start = nextJSONObject();
        if (-1 == start)
            return -1;//null;
        int len = sbText.length();
        return len;
    }

    /**
     * Return just the text field of the JSON tweet object.
     * 
     * @throws IOException      issue with reader
     */
    private int nextTweetText() throws IOException
    {
        start = nextJSONObject();
        if (-1 == start)
            return -1;//null;
        // find closing quote for text field
        end = start + 1; // opening quote + 1
        int len = sbText.length();
        while (end < len)
        {
            char c = sbText.charAt(end);
            if ( ('"' == c) && ('\\' != sbText.charAt(end-1)))
                break;
            else
                ++end;
        }
        // return the length of the tweet text in the stText buffer;
        // include chars in the range [start+1, end-1]

        return (end-1) - (start+1) + 1;
    }
}
