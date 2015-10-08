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

import java.util.Map;
import java.nio.charset.Charset;

/**
 * Class to contain options from config file.
 *
 * @author Richard Boyd
 */
public class Options 
{
    Options()
    {
        tokenOpts = new TokenOptions();
    }
    
    // configuration file
    public String configFile;
    
    // path to config directory
    public String configDir;
    
    public String defaultStopFile; 
    public String defaultSpellFile;
    public String defaultSlangFile;

    public String userStopFile;
    public String userSpellFile;
        
    public String analyzerName;

    // name of the twitter model file
    public String modelFile;    
    
    // output directory for the Lucene index
    public String outputDir;
    
    // base directory for filesystem input
    public String inputDir;
      
    // inputs needed for mongo input
    public String host;
    public int port;
    public String database;
    public String collection;
    public int limit;
    public String include;
    public String exclude;    
    
    // only display analysis results for first n tweets
    public int analyze;
    
    public boolean showHelp;
    public boolean useMongo;
    
    TokenOptions tokenOpts;
}
