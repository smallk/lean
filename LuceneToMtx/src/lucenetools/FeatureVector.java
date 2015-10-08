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

import java.util.ArrayList;
import java.util.HashMap;
 
/**
 * This class represents a single feature vector in the final matrix.
 *
 * @author Richard Boyd
 */
public class FeatureVector 
{
    // column index in the matrix
    public int docIndex;
    
    // associated document path
    public String docPath;
    
    // additional fields
    public HashMap<String, String> fields = new HashMap<>();
    
    // nonzero entries
    public ArrayList<TermData> data;
    
    public FeatureVector(int documentIndex) 
    {
        docIndex = documentIndex;
        data = new ArrayList<>();
    }
    
    public void add (int termIndex, int termCount) 
    {
        TermData td = new TermData(termIndex, termCount);
        data.add(td);
    }    
}
