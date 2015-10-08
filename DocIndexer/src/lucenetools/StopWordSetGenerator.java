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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.CharArraySet;

/**
 * Class to create stopword lists.
 *
 * @author Richard Boyd
 */
public class StopWordSetGenerator 
{
    StopWordSetGenerator() {}
    public static CharArraySet generate(String[] stopFiles) throws IOException
    {
        // Compute the union of all stop words in all stop files
        TreeSet<String> set = new TreeSet<>();
        for (String s : stopFiles)
        {
            if (null == s)
                continue;
            // load the stop words and generate a CharArraySet for a StopFilter
            File path = new File(s);
            FileInputStream fis = new FileInputStream(path);        
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            // load the stop word strings, assuming one per line
            String line;
            while (true)
            {
                line = reader.readLine();
                if (null == line)
                    break;
                // lowercase and add to list
                set.add(line.toLowerCase());
            }
            fis.close();
        }
        // 2nd arg == true means ingoreCase
        CharArraySet charArraySet = new CharArraySet(set.size(), true);
        Iterator<String> it = set.iterator();
        while (it.hasNext())
            charArraySet.add(it.next());   
        return charArraySet;
    }
}
