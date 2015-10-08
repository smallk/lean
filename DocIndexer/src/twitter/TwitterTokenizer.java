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

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.Reader;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
import cmu.arktweetnlp.impl.Model;
import cmu.arktweetnlp.impl.ModelSentence;
import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.impl.features.FeatureExtractor;
import lucenetools.TokenOptions;

/**
 * This class is uses the CMU Twokenize class to tokenize tweets.
 * 
 * @author Richard Boyd
 */
public final class TwitterTokenizer extends Tokenizer 
{
    // default size for the token buffer (CharTermAttribute buffer)
    public static final int DEFAULT_BUFFER_SIZE = 256;
    
    // max number of tokens per tweet
    public static final int MAX_TOKENS = 1024;
    
    // max number of characters in any tweet's text value field 
    // (includes retweet ids) - how to determine this? [TBD]
    public static final int MAX_TWEET_CHARS = 8192;   
        
    // token offsets into the current tweet (must handle longest possible tweet)
    private final int[] startOffsets = new int[MAX_TOKENS];
    private final int[] endOffsets   = new int[MAX_TOKENS];
    private final char[] tweetBuf    = new char[MAX_TWEET_CHARS];
    
    // holds the tokens returned by the CMU tokenizer
    private List<String> tokens;
    private ArrayList<TaggedToken> taggedTokens;

    // index of next token to be returned to caller of incrementToken()
    private int tokenIndex = 0;    
    
    private int finalOffset;
    private CharTermAttribute termAtt;
    private OffsetAttribute offsetAtt;
    private TypeAttribute typeAtt;
    
    // for part-of-speech tagging
    private Model model;
    private FeatureExtractor featureExtractor;
    
    TokenOptions tokenOpts;
  
    private static class TaggedToken
    {
        public String token;
        public String tag;
    }
    
    /**
    * Constructor.
    *
    * @param    input      reader 
    * @param    modelFile  modelfile for POS tagging
    * @param    opts       token options
    */
    public TwitterTokenizer(Reader input, String modelFile, TokenOptions opts)
    {
        this(input, DEFAULT_BUFFER_SIZE, modelFile, opts);
    }

    /**
    * Constructor.
    *
    * @param    input       reader 
    * @param    bufferSize  specify a non-default buffer size
    * @param    modelFile   modelfile for POS tagging
    * @param    opts        token options
    */
    public TwitterTokenizer(Reader input, int bufferSize, String modelFile, TokenOptions opts)
    {
        super(input);
        if (bufferSize <= 0) 
        {
            throw new IllegalArgumentException("TwitterTokenizer: bufferSize must be > 0");
        }

        tokenOpts = opts;
        init(bufferSize, modelFile);
    }

    /**
    * Constructor.
    *
    * @param    factory     Lucene attribute factory 
    * @param    input       reader 
    * @param    bufferSize  specify a non-default buffer size
    * @param    modelFile   modelfile for POS tagging
    * @param    opts        token options
    */
    public TwitterTokenizer(AttributeFactory factory, Reader input, int bufferSize, String modelFile)
    {
        super(factory, input);
        if (bufferSize <= 0) 
        {
            throw new IllegalArgumentException("TwitterTokenizer: bufferSize must be > 0");
        }

        init(bufferSize, modelFile);
    } 
    
    /**
    * Initialize size and model.
    *
    * @param    bufferSize  specify a non-default buffer size
    * @param    modelFile   modelfile for POS tagging
    */
    private void init(int bufferSize, String modelFile)
    {       
        try
        {
            model = Model.loadModelFromText(modelFile);
            featureExtractor = new FeatureExtractor(model, false);
        }
        catch (IOException e)
        {
            System.err.println("TwitterTokenizer failed to load arktweet model file " + modelFile);
        }
        
        // configure attributes
        termAtt = addAttribute(CharTermAttribute.class);
        termAtt.resizeBuffer(bufferSize);
        offsetAtt = addAttribute(OffsetAttribute.class);
        typeAtt = addAttribute(TypeAttribute.class);
    }

    /**
    * Define how tokens are processed.
    *
    * @throws    IOException    issue with reader
    */
    @Override
    public final boolean incrementToken() throws IOException 
    {       
        clearAttributes();
                
        // get the next tweet if no more tokens remain in the 'tokens' list
        if ( (null == tokens) || (0 == tokens.size()))
        {
            // get next tweet; returns -1 if no more tweets
            int numChars = input.read(tweetBuf, 0, MAX_TWEET_CHARS);
            if (-1 == numChars)
                return false;
            String tweetText = new String(tweetBuf, 0, numChars);
                        
            // CMU tokenizer: convert HTML escapes to UTF-8 characters
            tweetText = Twokenize.normalizeTextForTagger(tweetText);
            
            // CMU tokenizer: tokenize the converted text
            tokens = Twokenize.tokenize(tweetText);
            if (tokens.isEmpty())
                return false;
            if (tokenOpts.ignoreRetweets)
            {
                // this is a retweet if the first token is "RT"
                String firstToken = tokens.get(0);
                if (firstToken.equals("RT"))
                {
                    tokens.clear();
                    return false;
                }
            }
            
            // CMU tagger: compute part-of-speech tags
            Sentence sentence = new Sentence();
            sentence.tokens = tokens;
            ModelSentence ms = new ModelSentence(sentence.T());
            featureExtractor.computeFeatures(sentence, ms);
            model.greedyDecode(ms, false);

            // garbage collect previous taggedToken list - [TBD] faster to call clear()??
            taggedTokens = new ArrayList<>();
            for (int t=0; t<sentence.T(); t++) 
            {
                TaggedToken tt = new TaggedToken();
                tt.token = tokens.get(t);
                tt.tag = model.labelVocab.name( ms.labels[t] );
                taggedTokens.add(tt);
            }
                        
            // TBD - remove non-linguistic tokens - add to new list
            // use a temp list for the CMU results
            // (have POS tags of #, @, ~, U, E, G)
            
            // compute offsets within converted tweetText for each token
            if ( (null != tokens) && (tokens.size() > 0))
            {
                int q=0, pos=0;
                for (String s : tokens)
                {                    
                    // find the offset of token s in tweetText; begin
                    // the search at the end offset of the previous token;
                    // will return -1 if not found
                    startOffsets[q] = tweetText.indexOf(s, pos);
                    if (-1 == startOffsets[q])
                        throw new IOException("token '" + s + "' not found int tweet text");
                    
                    // the end offset is one past the final char
                    endOffsets[q]   = startOffsets[q] + s.length();                    
                    pos = endOffsets[q];
                    ++q;
                }

                finalOffset = correctOffset(endOffsets[q-1]);
                
                // index of next token to be returned
                tokenIndex = 0;
            }
        }
            
        String s = tokens.remove(0);
        TaggedToken tt = taggedTokens.remove(0);

        // set part-of-speech as the type attribute
        typeAtt.setType(tt.tag);
        
        // copy into token buffer (handles resizing and setting the valid char length)
        termAtt.copyBuffer(s.toCharArray(), 0, s.length());
        offsetAtt.setOffset(correctOffset(startOffsets[tokenIndex]), 
                            correctOffset(endOffsets[tokenIndex]));
        ++tokenIndex;
        return true;        
    }    
      
    /**
    * End the tokenizer.
    *
    * @throws    IOException    issue with reader
    */
    @Override
    public final void end() throws IOException 
    {
        super.end();
        // set final offset 
        offsetAtt.setOffset(finalOffset, finalOffset);
    }
    
    /**
    * Reset the tokenizer.
    *
    * @throws    IOException    issue with reader
    */
    @Override
    public void reset() throws IOException 
    {
        super.reset();        
    }
}
