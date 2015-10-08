# Introduction
LEAN is a set of Java tools for generating term-frequency matrices from text documents.  The LEAN tools are designed for compatibility with the GTRI/GA Tech SmallK software distribution, which consumes term-frequency matrices and performs hierarchical and flat clustering.

The LEAN software distribution currently consists of two tools: DocIndexer and LuceneToMtx. The DocIndexer application ingests documents in various formats and encodings, analyzes them with a user-configurable Lucene analyzer, and generates a Lucene inverted index.  The LuceneToMtx application reads the index, performs optional filtering on the terms, and generates a term-frequency matrix with matching dictionary and document files.

####Key Features of LEAN

**Lucene**: Lucene is a search engine library with fast indexing capabilities and many readily-available Natural Language Processing extensions.

**Scalability**: Lucene is the search engine used to power Apache Solr, a highly reliable, scalable and fault tolerant search platform providing distributed indexing and replication. While LEAN itself does not leverage Solr for these features currently, the ability to do so is available if the scalability requirements of a particular application are substantial.

**Performance**: Many other software options for text processing use less performant languages (like MATLAB or Python) or persist large portions of the documents being indexed in memory (WEKA). As each document is processed in LEAN, Lucene writes that document's index to the filesystem, avoiding the need to allocate enormous amounts of resources to the process. In our experiments, we have found LEAN to be 2x-4x faster than our other standard tools.

**Extensibility**: Lucene is used extensively in appliations all over the world and is maintained by a large community of developers. As a result, there is ample documentation for how to use Lucene for many different use-cases. There are also many extensions available for Lucene that provide specific functionality, particularly for NLP applications.

####Other Software Options
Other software packages exist in various languages that perform similar functionality. However, these are either based on programming paradigms that are not scalable or require custom implementations of new features.

**TMG**: Text to Matrix Generator (TMG) is a MATLAB(R) toolbox that can be used for various tasks in text mining.

**WEKA**: Machine learning software in Java.

**gensim**: Python package for topic modeling.

**JFreq**: Word frequency matrix generation in Java.

####Supported Input Formats
LEAN uses Apache Tika to ingest documents; Tika detects and extracts metadata and text from over a thousand different file types (list available [here](https://tika.apache.org/1.7/formats.html)). If LEAN encounters a document that Tika cannot parse, it will print a message to the console and skip this file, continuing through the rest of the corpus.


# Installation

**1. Install prerequisites** 

Java SE 8

Apache Ant

MongoDB (optional)

**2. Build DocIndexer**

From the top-level LEAN folder, run these commands:

```
cd DocIndexer
ant jar
```

The build process should complete successfully.

**3. Build LuceneToMtx**

From the top-level LEAN folder, run these commands:

```
cd LuceneToMtx
ant jar
```

**4. Test the installation**

Unzip the MongoDB test files by running the following commands from the top-level LEAN folder:

```
cd data
unzip mongo.zip -d mongo_files
```

If MongoDB is installed, run these commands from the top-level LEAN folder:

```
cd data
mongod --dbpath mongo_files/mongo --port 27019
```

Open a new terminal window and execute this command from the top-level LEAN folder:

```
sh run_tests.sh
```

The tests will start running; all tests should pass if MongoDB was launched as specified above. If MongoDB is not running, the mongo tests will time out and three failures will be reported. If the tests pass, the LEAN tools are ready for use.

#Quickstart

*The steps in this quickstart can be followed using the files in the* `quickstart` *directory.*

*From the top level directory:*

```
cd quickstart
```

###Step 0: Inputs

Consider the directory `quickstart/data/` that contains the following simple files:

File: fox.txt

```
The quick brown fox jumps over the lazy dog.
```

File: goblin.txt

```
The quick onyx goblin jumps over the lazy dwarf.
```

We can use LEAN to create an index from these and then create files suitable for topic modeling from that index.

### Step 1: Prep the config file
LEAN's DocIndexer application uses a YAML configuration file to control many aspects of the application flow. For this example, we will use the `quickstart.yml` file in the `quickstart/` directory. There are more details on and examples of the config files further down in the documentation. At it's simplest, a config file for indexing documents from a directory must contain the following four fields:

**config_path**: Specify the path to the LEAN repository's 'config' folder. This folder contains default stopword and spelling files, among other items. Items in this folder are loaded automatically at startup.

**outdir**: Write the Lucene index to this folder.  If this folder does not exist it will be created.

**analyzer**: Specify the analyzer to use.  An analyzer consists of a tokenizer and a chain of zero or more token filters.  The filters perform various transformations on the tokens as they pass down the chain. `org.apache.lucene.analysis.en.EnglishAnalyzer` is an Analyzer provided by Lucene that is designed for English text analysis. Many other readily available Lucene Analyzers can be used by specifying the fully qualified class name in the YAML configuration file.

**indir**: Specify the path to the root folder of the file tree to be analyzed.  DocIndexer will begin at this root folder and recursively process all documents in supported formats throughout the tree.

We can use the `quickstart.yml` configuration file (copied below) to apply the DocIndexer application to our test files. 

```
config_path: ../config/
outdir: index
analyzer: org.apache.lucene.analysis.en.EnglishAnalyzer
indir: data/

```

Running the application in `analyze` mode will allow us to see how the Analyzer transforms the tokens as each document is processed.


```
java -jar ../DocIndexer/dist/DocIndexer.jar --config quickstart.yml --analyze 2
```

This command will produce the following output:

```
DocIndexer version 0.3.

Configuration data: 

	   Configuration file: /path/to/lean/quickstart/quickstart.yml
	   Default slang file: <NONE>
	Default stopword file: /path/to/lean/quickstart/../config/default_stopwords.txt
	   User stopword file: <NONE>
	Default spelling file: /path/to/lean/quickstart/../config/default_spellings.txt
	   User spelling file: <NONE>
		        Input dir: /path/to/lean/quickstart/data
	               outdir: /path/to/lean/quickstart/index
	             analyzer: org.apache.lucene.analysis.en.EnglishAnalyzer
   Disable custom filters: No
Ignore all but <ALPHANUM>: No
	      Ignore numerals: No
	     Disable stemming: No

Java runtime max memory: 3641 MB.
Analysis results for file 1: /path/to/lean/quickstart/data/fox.txt, 2 files max:

The quick brown fox jumps over the lazy dog.

start:    4	end:    9	length:    5	tag: <ALPHANUM>    token: quick
start:   10	end:   15	length:    5	tag: <ALPHANUM>    token: brown
start:   16	end:   19	length:    3	tag: <ALPHANUM>    token: fox
start:   20	end:   25	length:    4	tag: <ALPHANUM>    token: jump
start:   26	end:   30	length:    4	tag: <ALPHANUM>    token: over
start:   35	end:   39	length:    4	tag: <ALPHANUM>    token: lazi
start:   40	end:   43	length:    3	tag: <ALPHANUM>    token: dog

Analysis results for file 2: /path/to/lean/quickstart/data/goblin.txt, 2 files max:

The quick onyx goblin jumps over the lazy dwarf.

start:    4	end:    9	length:    5	tag: <ALPHANUM>    token: quick
start:   10	end:   14	length:    4	tag: <ALPHANUM>    token: onyx
start:   15	end:   21	length:    6	tag: <ALPHANUM>    token: goblin
start:   22	end:   27	length:    4	tag: <ALPHANUM>    token: jump
start:   28	end:   32	length:    4	tag: <ALPHANUM>    token: over
start:   37	end:   41	length:    4	tag: <ALPHANUM>    token: lazi
start:   42	end:   47	length:    5	tag: <ALPHANUM>    token: dwarf
```

To generate the actual index files, we need to run this application without the `analyze` option. 

```
java -jar ../DocIndexer/dist/DocIndexer.jar --config quickstart.yml
```

You will see the following output:


```
DocIndexer version 0.3.

Configuration data: 

	   Configuration file: /path/to/lean/quickstart/quickstart.yml
	   Default slang file: <NONE>
	Default stopword file: /path/to/lean/quickstart/../config/default_stopwords.txt
	   User stopword file: <NONE>
	Default spelling file: /path/to/lean/quickstart/../config/default_spellings.txt
	   User spelling file: <NONE>
		        Input dir: /path/to/lean/quickstart/data
	               outdir: /path/to/lean/quickstart/index
	             analyzer: org.apache.lucene.analysis.en.EnglishAnalyzer
   Disable custom filters: No
Ignore all but <ALPHANUM>: No
	      Ignore numerals: No
	     Disable stemming: No

Java runtime max memory: 3641 MB.
Writing index to: '/path/to/lean/quickstart/index' ...


Indexed 2 documents.
Elapsed time: 0.92 seconds, avg. rate: 2.17 docs/s.
```

This will write the Lucene index files to `/path/to/lean/quickstart/index/`:

```
/path/to/lean/quickstart/index/
	_0.cfe
	_0.cfs
	_0.si
	segments.gen
	segments_1
```

It is not important for most purposes to understand the details of these index files. They contain an inverted index for each document and will be ingested by the LuceneToMtx application to output the files for topic modeling. To do this, we need to point the LuceneToMtx appliation at this index directory.

```
java -jar ../LuceneToMtx/dist/LuceneToMtx.jar --indir index --outdir results/
```

This will generate the following output:

```
Creating directory 'results'.

LuceneToMtx version 0.1.
Command line options: 

	 minfreq: 1
	   maxprct: 100
	   indir: index
	  outdir: results/
	  fields: 0

Java runtime max memory: 3641 MB.
Processing index...
Wrote matrix.mtx.
Wrote dictionary.txt.
Wrote documents.txt.

```

This will write the output files to the `results/` directory.

```
results/
	dictionary.txt
	documents.txt
	matrix.mtx
```

These files are sufficient for topic modeling and can be directly used in the SmallK suite of document clustering tools.

The `dictionary.txt` file contains a newline-delimited list of terms in the term-document matrix. The contents for this example would be as follows:

```
brown
dog
dwarf
fox
goblin
jump
lazi
onyx
over
quick
```

The `documents.txt` file contains a newline-delimited list of document IDs that identify the documents used to construct the term-document matrix.

```
fox.txt
goblin.txt
```

The `matrix.mtx` file contains a file formatted as a MatrixMarket sparse matrix file. The first column of numbers identifies the term index (line number in the `dictionary.txt` file), the second column identifies the document index (line number in the `documents.txt` file), and the third column identifies the count for that term in that document.

```
%%MatrixMarket matrix coordinate real general
10 2 14
1 1 1
2 1 1
4 1 1
6 1 1
7 1 1
9 1 1
10 1 1
3 2 1
5 2 1
6 2 1
7 2 1
8 2 1
9 2 1
10 2 1
``` 



# DocIndexer Usage
There are two modes of operation for DocIndexer.  In analysis mode, DocIndexer displays the tokenized and analyzed text without writing anything to a Lucene index.  This mode of operation is useful for investigating and experimenting with various analysis techniques.  In normal mode, DocIndexer fully analyzes the documents and writes the tokens to a Lucene index.

DocIndexer’s normal mode of operation is to open a directory on disk and recurse through the file tree rooted at that folder, extracting content from all recognized document formats.  DocIndexer prints a message to the screen if it encounters a file it cannot recognize.

DocIndexer can also ingest raw Twitter JSON data, either from files on disk or from a MongoDB database.  Twitter’s servers return data in a known format (UTF8-encoded JSON), so TIKA is not needed to parse it (the data files must have a .json extension).  We have written our own fast Twitter data parser that simply pulls the tweet text from JSON files as fast as it can. For Twitter data stored in a Mongo database, DocIndexer uses the MongoDB Java Driver.

The DocIndexer application is an executable jar file.  If you run the jar file without any command-line arguments, or with the –h or --help options, the application will output the command-line options supported.  For instance, from the top-level LEAN folder, display the DocIndexer’s command-line options by running this command:

```
java -jar DocIndexer/dist/DocIndexer.jar
```
You should see the following output:

Usage:

```
	java -jar /path/to/DocIndexer.jar
	         	--config    <filename>   YAML configuration file
	         [--analyze        n   ]  	show analysis for first n documents; do not index
```

### YAML Configuration Files

DocIndexer requires the use of a YAML configuration file to set various runtime options.  Three default configuration files are included in the LEAN repository and include detailed inline comments.  Use these as the starting point for your own configuration files by copying them, renaming them, and setting the paths as appropriate for your system.  

These default files are: `default_docs_config.yml`, which configures the DocIndexer to recurse through a directory tree of documents; `default_twitter_config.yml`, for processing Twitter JSON data from a directory tree; and `default_mongo_config.yml`, for processing Twitter data from a MongoDB database.

Example configuration file:

```
""" 
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
"""

###############################################################################
#                                                                             #
#                      GENERAL CONFIGURATION DETAILS                          #
#                                                                             #
#     Configure DocIndexer to processes documents in a directory tree.        #
#                                                                             #
###############################################################################

# [REQUIRED] Specify the full path to the LEAN repository's 'config' folder.
# This folder contains default stopword and spelling files, among other items.
# Items in this folder are loaded automatically at startup.

config_path: /path/to/lean/config

# [REQUIRED] Write the Lucene index to this folder.  If this folder does not
# exist it will be created.

outdir: /path/to/index

# [REQUIRED] Specify the analyzer to use.  An analyzer consists of a tokenizer
# and a chain of zero or more token filters.  The filters perform various
# transformations on the tokens as they pass down the chain.  The first four
# analyzers are provided by Lucene and are not customizable; the 'custom'
# analyzers can be easily altered and recompiled.

# org.apache.lucene.analysis.core.WhitespaceAnalyzer      Split text on whitespace only
# org.apache.lucene.analysis.standard.StandardAnalyzer    Lucene's default text analyzer
# org.apache.lucene.analysis.standard.ClassicAnalyzer     Lucene's StandardAnalyzer pre v3.1
# org.apache.lucene.analysis.en.EnglishAnalyzer           Lucene's English-specific text analyzer
# analyzers.FormalAnalyzer                                GTRI analyzer for formal documents
# twitter.TwitterAnalyzer                                 GTRI custom analyzer for Twitter

analyzer: analyzers.FormalAnalyzer

# [REQUIRED] Specify the full path to the root folder of the file tree to be
# analyzed.  DocIndexer will begin at this root folder and recursively process
# all documents in supported formats throughout the tree.

indir: /path/to/input_folder_root

# [OPTIONAL] Specify the absolute path to a 'user' stopword file.  Use this
# file to give DocIndexer additional stopwords that should be removed from the
# token stream, but that are not contained in the default stopword list
# (found in the config folder).  This file can also be used to remove specific
# tokens that may not be of interest for a given data set.  The file name does
# not have to be 'user_stopwords.txt'.  To NOT use a user stopword file,
# comment the following line.

#user_stopword_file: /path/to/user_stopwords.txt

# [OPTIONAL] Specify the absolute path to a 'user' spelling file.  Use this
# file to give DocIndexer additional spelling corrections that should be
# performed on the token stream, but that are not contained in the default list
# of spelling corrections (found in the config folder).  This file can be used
# to remove or 'normalize' domain-specific slang.  The file name does not have
# to be 'user_spelling.txt'.  To NOT use a user spelling file,
# comment the next line.

#user_spelling_file: /path/to/user_spelling.txt

###############################################################################
#                                                                             #
#                            Boolean flags (Yes/No)                           #
#                                                                             #
###############################################################################

# [OPTIONAL - CUSTOM ANALYZERS ONLY] Whether to discard all tokens tagged with
# the <NUM> tag by the tokenizer.  

IGNORE_NUMERALS: No

# [OPTIONAL - CUSTOM_FORMAL ANALYZER ONLY] Whether to discard all tokens except
# those tagged with the <ALPHANUM> tag.  Overrides the IGNORE_NUMERALS option.

IGNORE_ALL_BUT_ALPHANUM: No

# [OPTIONAL - CUSTOM ANALYZERS ONLY] Whether to disable stemming.  Stemming
# removes characters from selected tokens, which may not be desirable in all
# scenarios.

DISABLE_STEMMING: No

# [OPTIONAL] Whether to disable all filters in the CUSTOM analyzers.  If
# this option is selected, tokenization is the only operation performed on the
# data.  Use this option if you want to see the full set of tokens that emerge
# from the tokenizer.  This option does not apply to the Lucene-provided
# analyzers.

DISABLE_CUSTOM_FILTERS: No

```

The `config_path`, `outdir`, and `indir` entries must all be directories, not files.The `user` files exist to augment the default sets of stop words and spelling corrections provided by DocIndexer.  The default sets are small rather than large, to keep the impact on the text minimal.  Some users may prefer this; other users may want large stop word or spelling correction lists.  To disable the use of a user file, simply comment the appropriate line in the config file.  The default configuration is to not use user files.Users can also alter the default files located in the config folder.  Stop words and spell correction mappings can be added or removed.  If the default files are deleted or have no content the DocIndexer will simply ignore them.If you find the default list of stop words and spell correction mappings to be inadequate, you can augment these default files with additional entries that should be applied to all data sets.  The `user` stopword file could then be used for removal of specific tokens or words relevant to the particular data set being examined.  For instance, an analysis of documents about Justin Bieber could benefit from removing the token ‘bieber’ from the analysis, since it would likely appear as a top term in many (if not most) of the clusters.  This can be achieved by making ‘bieber’ an entry in the user stop word file.This file also contains several Boolean flags that can be used to selectively remove tokens from the stream.  These should be self-explanatory.  The DISABLE_CUSTOM_FILTERS tag will allow tokenization of the input text but no filtering.  Set this flag to ‘Yes’ if you only want to see the raw token stream at the output of the tokenizer.  This option is useful for investigating the effects of the analyzer on the various tokens via before-and-after comparisons in analysis mode.

###Usage

When the `--anaylze` option is provided, the output will show the tokens after they have passed through all of the filters in the Analyzer that was selected. Note that the Lucene index will NOT be created when this option is provided. 

From the top-level LEAN folder, run:

```
java -jar DocIndexer/dist/DocIndexer.jar --config tests/test_twitter/test_twitter_config.yml --analyze 1
```

You will see the following output:

```
DocIndexer version 0.3.

	   Configuration file: /path/to/lean/tests/test_twitter/test_twitter_config.yml
	   Default slang file: /path/to/lean/config/default_slang.txt
	Default stopword file: /path/to/lean/config/default_stopwords.txt
	   User stopword file: <NONE>
	Default spelling file: /path/to/lean/config/default_spellings.txt
	   User spelling file: <NONE>
		    Input dir: /path/to/lean/data/twitter_test
	               outdir: /path/to/lean/tests/test_twitter/index
	             analyzer: twitter.TwitterAnalyzer
       Disable custom filters: No
	          Ignore URLs: No
	      Ignore hashtags: No
	   Ignore at-mentions: No
	      Ignore retweets: No
	      Ignore numerals: No
	     Disable stemming: No

Java runtime max memory: 3641 MB.

Analysis results for file /Users/AshleyScripka/development/projects/xdata/lean/data/twitter_test/samples.json, 1 tweets max:

	[1]	“The more toppings a man has on his pizza, I believe, the more manly he is,” Cain opined.  #OWS http://t.co/iPsUdnGw
	start:    5	end:    9	length:    4	tag: A	token: more
	start:   10	end:   18	length:    7	tag: N	token: topping
	start:   21	end:   24	length:    3	tag: N	token: man
	start:   36	end:   41	length:    5	tag: N	token: pizza
	start:   45	end:   52	length:    7	tag: V	token: believe
	start:   58	end:   62	length:    4	tag: A	token: more
	start:   63	end:   68	length:    5	tag: A	token: manly
	start:   77	end:   81	length:    4	tag: ^	token: cain
	start:   82	end:   88	length:    5	tag: ^	token: opine
	start:   91	end:   95	length:    4	tag: #	token: #ows
	start:   96	end:  116	length:   20	tag: U	token: http://t.co/ipsudngw

```

After generating the Lucene index by running the DocIndexer application without the `--analyze` option, the LuceneToMtx application must be run to generate the SmallK input files.

# LuceneToMtx
LuceneToMtx is an executable jar file.  If you run the jar file without any command-line arguments, or with the –h or --help arguments, the application will output the command-line options supported.  For instance, from the top-level LEAN folder, display the supported command-line options by running this command:

```
java -jar LuceneToMtx/dist/LuceneToMtx.jar 
```
You should see the following output:

```
Usage: java -jar /path/to/LuceneToMtx.jar
		 --indir      <path to Lucene index dir>
		[--outdir]    result files written here
		[--minfreq 1] minimum term freq cutoff value
		[--maxprct 100] maximum term occurrence
		[--fields 0] write out all fields in the index
```

The LuceneToMtx application outputs a matrix.mtx file, a dictionary.txt file, and a documents.txt file. These files can be used as input to the SmallK suite of document clustering tools. The default values for `--minfreq` and `--maxprct` mean that all terms will be kept. These can be adjusted as desired. Setting the `--fields` argument to 1 will mean that any additional fields that have been added to the Lucene index via the DocIndexer application will be written to files as well as the default set of output documents. This is helpful for when there is value in tracking additional features of the text, such as latitude and longitude points for tweets.

# Performance Considerations
It can be tempting to take full advantage of the options available within Lucene to do NLP by creating and using lots of filters in a single analyzer. However, each filter and each tokenizer consumes resources for every token in the corpus of documents being indexed. Therefore careful consideration should be made for each tokenizer and filter used to be confident that its use is more beneficial than costly. 

As usual, there is a tradeoff between the speed and the quality of results. For example, the costliest componet of the custom TwitterAnalyzer is the use of a Twitter-aware tokenizer. This tokenizer uses a trained model to make decisions about the part-of-speech for each of the tokens in the tweet. This can be very useful information and can help reduce noisy and undesirable terms in the intermediate and final results. This cleanness comes at a cost, though, nearly doubling the processing time from approximately 2k tweets/sec without this special tokenizer to 1k tweets/sec with the tokenizer. The needs of the use-case should dictate which tokenizer is more appropriate.

