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

package analyzers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import lucenetools.TokenOptions;

/**
 *
 * @author Richard Boyd, Ashley Beavers
 */
public class AnalyzerFactory 
{

    /**
    * Create the analyzer.
    * <p>
    * This method determines the analyzer to be used based on the what is specified in the config
    * file. It will determine the most inclusive constructer to use based on what is available 
    * for the chosen analyzer.
    *
    * @param    analyzerName                the analyzer name specified in the config file
    * @param    stopWordSet                 user-provided set of stopwords, if any
    * @param    spellingHashtable           user-provided set of spelling corrections, if any
    * @param    slangHashtable              user-provided set of slang corrections, if any
    * @param    tokenOpts                   options for token processing from the config file
    * @param    modelFile                   modelfile for Twitter POS tagging
    * @throws   IOException                 if file not found
    * @throws   ClassNotFoundException      if class not found
    * @throws   NoSuchMethodException       if method not found
    * @throws   InstantiationException      cannot instantiate class
    * @throws   IllegalAccessException      cannot access class
    * @throws   IllegalArgumentException    wrong set of arguments
    * @throws   InvocationTargetException   instantiated class has a problem
    * @return   the analyzer
    */
    public static Analyzer create(String analyzerName,
                                  CharArraySet stopWordSet,
                                  HashMap<String, String> spellingHashtable,
                                  HashMap<String, String> slangHashtable,
                                  TokenOptions tokenOpts,
                                  String modelFile) 
            throws IOException, ClassNotFoundException, NoSuchMethodException, 
                InstantiationException, IllegalAccessException, 
                IllegalArgumentException, InvocationTargetException
    {
        Analyzer analyzer = null;
        Class<?> clazz = null;
        
        try {
            clazz = Class.forName(analyzerName);
        } catch (ClassNotFoundException cnfe) {
            return analyzer;
        }
 
        // find the most inclusive constructor
        try {
            // option 1
            Constructor<?> ctor = clazz.getConstructor(String.class, 
                CharArraySet.class,
                HashMap.class,
                HashMap.class,
                TokenOptions.class);
            analyzer = (Analyzer)ctor.newInstance(new Object[] { modelFile,
                                                        stopWordSet,
                                                        spellingHashtable,
                                                        slangHashtable,
                                                        tokenOpts});
        } catch (NoSuchMethodException nsme) {
            try {
                // option 2
                Constructor<?> ctor = clazz.getConstructor(CharArraySet.class,
                    HashMap.class,
                    TokenOptions.class);
                analyzer = (Analyzer)ctor.newInstance(new Object[] { stopWordSet,
                                                        spellingHashtable,
                                                        tokenOpts});
            } catch (NoSuchMethodException nsm) {
                try {
                    // option 3
                    Constructor<?> ctor = clazz.getConstructor(String.class,
                        CharArraySet.class,
                        TokenOptions.class);
                    analyzer = (Analyzer)ctor.newInstance(new Object[] { modelFile,
                                                        stopWordSet,
                                                        tokenOpts});
                } catch (NoSuchMethodException ns) {
                    try {
                        if (stopWordSet.isEmpty()) {
                            // option 4
                            Constructor<?> ctor = clazz.getConstructor();
                            analyzer = (Analyzer)ctor.newInstance();
                        }
                        else {
                            try {
                                //option 5
                                Constructor<?> ctor = clazz.getConstructor(CharArraySet.class);
                                analyzer = (Analyzer)ctor.newInstance(stopWordSet);
                            } catch (NoSuchMethodException n) {
                                // option 4 again
                                Constructor<?> ctor = clazz.getConstructor();
                                analyzer = (Analyzer)ctor.newInstance();
                                
                            }
                        }
                    } catch (NoSuchMethodException n) {

                    }
                }
            }
        }
        
        return analyzer;
    }    
}
