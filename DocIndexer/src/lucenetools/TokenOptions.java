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

/**
 * Class to contain token options from config file.
 *
 * @author Richard Boyd
 */
public class TokenOptions 
{
    TokenOptions()
    {
        ignoreURLs           = false;
        ignoreNumerals       = false;
        ignoreHashtags       = false;
        ignoreAtMentions     = false;
        ignoreAllButAlphanum = false;
        ignoreRetweets       = false;
        disableStemming      = false;
        disableAllFilters    = false;
    }
    
    public boolean ignoreURLs;
    public boolean ignoreNumerals;
    public boolean ignoreHashtags;
    public boolean ignoreRetweets;
    public boolean ignoreAtMentions;
    public boolean ignoreAllButAlphanum;
    
    public boolean disableStemming;
    public boolean disableAllFilters;
}
