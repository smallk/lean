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



package lucenetools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

/**
 * Class to represent the spelling correction file.
 *
 * @author Richard Boyd
 */
public class SpellingFile 
{
    private static int pos;
    private static final char COMMENT_CHAR = '#';
    private static final char COMMA_CHAR   = ',';
    
    private static final TreeMap<String, String> treeMap = new TreeMap<>();
    private static HashMap<String, String> hashMap;
    
    public static HashMap<String, String> getHashtable()
    {
        // return a deep copy of the hashMap
        HashMap<String, String> theCopy = new HashMap<>(hashMap.size());        
        for (Map.Entry<String, String> entry : hashMap.entrySet())
        {
            theCopy.put(entry.getKey(), entry.getValue());
        }
        return theCopy;
    }
    
    private static void skipWhitespace(String line)
    {        
        char c = line.charAt(pos);
        while (Character.isWhitespace(c))
        {
            ++pos;
            if (pos >= line.length())
                break;
            
            c = line.charAt(pos);
        }       
    }
    
    private static String readToken(String line)
    {
        StringBuilder buffer = new StringBuilder();
        char c = line.charAt(pos);
        while ( (COMMA_CHAR != c) && !Character.isWhitespace(c))
        {
            buffer.append(c);
            ++pos;
            if (pos >= line.length())
                break;
            
            c = line.charAt(pos);
        }
        return buffer.toString();        
    }
    
    public static boolean consolidate(String [] files) throws IOException
    {
        treeMap.clear();
        
        for (String s : files)
        {
            int lineCounter = 0;
            if (null == s)
                continue;
            File path = new File(s);
            FileInputStream fis = new FileInputStream(path);        
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            // load the stop word strings, assuming one per line
            String line;
            while (true)
            {
                line = reader.readLine();
                // EOF returns null
                if (null == line)
                    break;
                if (line.isEmpty())
                    continue;
                // pos is the character offset into the current line
                pos = 0;
                if (COMMENT_CHAR == line.charAt(pos))
                    continue;
                // skip any leading whitespace
                skipWhitespace(line);
                // read correctly-spelled token
                String token = readToken(line);                
                // skip whitespace between token and misspellings
                skipWhitespace(line);                
                // read all misspellings
                while (pos < line.length())
                {
                    // get the next misspelling
                    String word = readToken(line);                   
                    // add to treeMap if not already present
                    if (!treeMap.containsKey(word))
                        treeMap.put(word, token);
                    if (line.length() <= pos)
                        break;
                    // skip any whitespace following the word
                    skipWhitespace(line);
                    if (line.length() <= pos)
                        break;
                    // the next char must be a comma
                    if (COMMA_CHAR != line.charAt(pos))
                    {
                        char c = line.charAt(pos);
                        String msg = "Unexpected character ->" + c + "<- on line " + 
                                lineCounter + " of file " + s;
                        throw new IOException(msg);
                    }
                    // skip over the comma
                    if (line.length() <= ++pos)
                        break;
                    // skip any whitespace between comma and next misspelling
                    skipWhitespace(line);
                }
                ++lineCounter;                
            }
            fis.close();
        }            
        // Now load all the mappings into the hashtable
        hashMap = new HashMap<>(treeMap.size());
        for (Map.Entry<String, String> entry : treeMap.entrySet())
        {
            hashMap.put(entry.getKey(), entry.getValue());
        }
        return true;
    }
}
