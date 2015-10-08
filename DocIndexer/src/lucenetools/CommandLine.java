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

import analyzers.AnalyzerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;

/**
 * This class represents the command line interface that handles
 * the execution of the application.
 * 
 * @author Richard Boyd
 */
public class CommandLine {

    private final String NONE = "<NONE>";
    private final String YES = "Yes";
    private final String NO = "No";

    private final String ARG_CONFIGFILE = "config";
    private final String ARG_ANALYZE = "analyze";
    private final String ARG_HELP = "help";
    private final String ARG_COL = "collection";
    private final String ARG_OUTDIR = "outdir";
    
    private final String DEFAULT_STOP_FILE = "default_stopwords.txt";
    private final String DEFAULT_SPELL_FILE = "default_spellings.txt";
    private final String DEFAULT_SLANG_FILE = "default_slang.txt";
    private final String TWITTER_MODEL_FILE = "cmu_twitter_model";

    // these match the 'identifier:' strings in the config file
    private final String CONFIGDIR_TOKEN = "config_path";
    private final String USER_STOP_FILE_TOKEN = "user_stopword_file";
    private final String USER_SPELL_FILE_TOKEN = "user_spelling_file";
    private final String ANALYZER_TOKEN = "analyzer";
    private final String INDIR_TOKEN = "indir";
    private final String OUTDIR_TOKEN = "outdir";

    private final String IGNORE_URL_TOKEN = "IGNORE_URLS";
    private final String IGNORE_NUMERALS_TOKEN = "IGNORE_NUMERALS";
    private final String IGNORE_HASHTAGS_TOKEN = "IGNORE_HASHTAGS";
    private final String IGNORE_AT_MENTIONS_TOKEN = "IGNORE_AT_MENTIONS";
    private final String IGNORE_ALL_BUT_ALPHANUM = "IGNORE_ALL_BUT_ALPHANUM";
    private final String IGNORE_RETWEETS_TOKEN = "IGNORE_RETWEETS";
    private final String DISABLE_STEMMING_TOKEN = "DISABLE_STEMMING";
    private final String DISABLE_CUSTOM_FILTERS_TOKEN = "DISABLE_CUSTOM_FILTERS";

    // mongo
    private final String MONGO_HOST_TOKEN = "host";
    private final String MONGO_PORT_TOKEN = "port";
    private final String MONGO_DB_TOKEN = "database";
    private final String MONGO_COL_TOKEN = "collection";
    private final String MONGO_LIM_TOKEN = "limit";
    private final String MONGO_IN_TOKEN = "include";
    private final String MONGO_EX_TOKEN = "exclude";
    private final String MONGO_LANG_TOKEN = "lang";

    public CommandLine() {
    }

    /**
     * Check for valid directory; create if needed.
     * 
     * @param   path         path to input directory
     * @param   argName      command line argument
     * @return  boolean      whether or not the directory exists
     */
    private boolean ensureDirExists(String path, String argName) {
        boolean result = true;

        File dir = new File(path);
        if (!dir.exists()) {
            // try to create the dir if it does not already exist            
            try {
                result = dir.mkdirs();
            } catch (SecurityException e) {
                ErrorReporter.reportException(e.getClass().toString(), e.getMessage());
                result = false;
            }
        } else if (!isValidDir(path, argName)) {
            // not a valid directory
            result = false;
        }

        return result;
    }

    /**
     * This method determines if a file is readable.
     * 
     * @param   filepath    path to file
     * @return  boolean      whether or not the path is readable
     */
    private boolean isReadableFile(String filepath) {
        File file = new File(filepath);
        if (!file.exists() || !file.canRead()) {
            System.out.println("The file '" + filepath
                    + "' is not readable.");
            return false;
        }
        return true;
    }

    /**
     * This method determines if a file exists
     * 
     * @param   filepath    path to file
     * @return  boolean     whether or not the file exists
     */
    private boolean fileExists(String filepath) {
        File file = new File(filepath);
        return file.exists();
    }

    /**
     * This method determines if a file is valid
     * 
     * @param   filepath    path to file
     * @param   argName     command line argument name
     * @return  boolean     whether or not the file is valid
     */
    private boolean isValidFile(String filepath, String argName) {
        File file = new File(filepath);
        if (file.isDirectory()) {
            System.out.println("The " + argName + " argument"
                    + " must be a file, not a directory.");
            return false;
        }

        if (!file.exists() || !file.canRead()) {
            System.out.println("The file '" + filepath
                    + "' is not readable.");
            return false;
        }

        return true;
    }

    /**
     * This method determines if a directory is valid
     * 
     * @param   filepath    path to directory
     * @param   argName     command line argument name
     * @return  boolean     whether or not the directory is valid
     */
    private boolean isValidDir(String path, String argName) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("The " + argName + " argument '" + path
                    + "' is not a directory.");
            return false;
        }

        return true;
    }

    /**
     * This method interprets the boolean options from the config file
     * 
     * @param   config      config mapping from file
     * @param   optToken    command line argument name
     * @return  boolean     whether or not the options were valid
     */
    private boolean getBooleanOption(Map config, String optToken) {
        if (null != config.get(optToken)) {
            String s = config.get(optToken).toString().toLowerCase();
            if (s.equals("yes") || s.equals("1") || s.equals("true")) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    /**
     * This method interprets the boolean options from the config file.
     * 
     * @param   opts                    options object containing config settings
     * @return  boolean                 whether or not the options were valid
     * @throws  FileNotFoundException   could not find a file or directory
     * @throws  IOException             error with file loading
     */
    public boolean isValid(Options opts) throws FileNotFoundException, IOException {
        if (null == opts.configFile) {
            System.out.println("A config file must be specified.");
            return false;
        }

        if (!isValidFile(opts.configFile, "config")) {
            return false;
        }

        File configFile = new File(opts.configFile);
        opts.configFile = configFile.getAbsolutePath();

        InputStream input = new FileInputStream(configFile);
        Yaml yaml = new Yaml();

        Map config = null;
        try {

            Object object = yaml.load(input);
            config = (Map) object;
            input.close();

        } catch (ParserException e) {
            ErrorReporter.reportException(e.getClass().toString(), e.getMessage());
            System.out.println("YAML configuration file syntax error.");
            return false;
        }

        // Check for the presence of the INDIR_TOKEN and the MONGO_HOST_TOKEN.
        // Only one should be present.  If both are present, or if both are
        // null, this config file is invalid.
        Object objIndir = config.get(INDIR_TOKEN);
        Object objHost = config.get(MONGO_HOST_TOKEN);
        if ((null != objIndir) && (null != objHost)) {
            System.out.println("Invalid config file - conflicting input sources. "
                    + "Choose either " + INDIR_TOKEN
                    + " or MongoDB as the input source.");
            return false;
        } else if ((null == objIndir) && (null == objHost)) {
            System.out.println("Invalid config file - missing input source. "
                    + " Choose either " + INDIR_TOKEN
                    + " or MongoDB as the input source.");
            return false;
        }

        // get path to the config dir
        if (null != config.get(CONFIGDIR_TOKEN)) {
            opts.configDir = config.get(CONFIGDIR_TOKEN).toString();
            if (!isValidDir(opts.configDir, CONFIGDIR_TOKEN)) {
                return false;
            }
        }

        // get the analyzer
        boolean usingTwitter = false;
        if (null == config.get(ANALYZER_TOKEN)) {
            System.out.println("YAML configuration file must include the "
                    + ANALYZER_TOKEN + " field.");
            return false;
        } else {

            if (null != config.get(ANALYZER_TOKEN)) {
                opts.analyzerName = config.get(ANALYZER_TOKEN).toString();

                if (opts.analyzerName.toLowerCase().contains("twitter")) {
                    usingTwitter = true;
                }
            }

            // construct path names of default files
            Path configDir = Paths.get(opts.configDir);
            Path defaultStopPath = configDir.resolve(DEFAULT_STOP_FILE);
            Path defaultSpellPath = configDir.resolve(DEFAULT_SPELL_FILE);
            Path defaultSlangPath = configDir.resolve(DEFAULT_SLANG_FILE);

            // check for twitter model file if using twitter analyzer        
            opts.modelFile = null;
            if (usingTwitter) {
                Path twitterModelFilePath = configDir.resolve(TWITTER_MODEL_FILE);
                opts.modelFile = twitterModelFilePath.toAbsolutePath().toString();

                //check for exists first - if not, tell user to check the path to config/
                if (!fileExists(opts.modelFile)) {
                    System.out.println("The config directory path '"
                            + opts.configDir + "' is invalid.");
                    return false;
                }

                if (!isReadableFile(opts.modelFile)) {
                    return false;
                }
            }

            // check if the default stop file can be read (not a dir, by construction)
            opts.defaultStopFile = defaultStopPath.toAbsolutePath().toString();
            if (!isReadableFile(opts.defaultStopFile)) {
                opts.defaultStopFile = null;
            }

            // ditto for the default spelling file
            opts.defaultSpellFile = defaultSpellPath.toAbsolutePath().toString();
            if (!isReadableFile(opts.defaultSpellFile)) {
                opts.defaultSpellFile = null;
            }

            // ditto for the default slang file
            if (!usingTwitter) {
                opts.defaultSlangFile = null;
            } else {
                opts.defaultSlangFile = defaultSlangPath.toAbsolutePath().toString();
                if (!isReadableFile(opts.defaultSlangFile)) {
                    opts.defaultSlangFile = null;
                }
            }

            // check for a user stopword file
            opts.userStopFile = null;
            if (null != config.get(USER_STOP_FILE_TOKEN)) {
                opts.userStopFile = config.get(USER_STOP_FILE_TOKEN).toString();
                if (!isValidFile(opts.userStopFile, USER_STOP_FILE_TOKEN)) {
                    opts.userStopFile = null;
                }
            }

            // check for a user spelling file
            opts.userSpellFile = null;
            if (null != config.get(USER_SPELL_FILE_TOKEN)) {
                opts.userSpellFile = config.get(USER_SPELL_FILE_TOKEN).toString();
                if (!isValidFile(opts.userSpellFile, USER_SPELL_FILE_TOKEN)) {
                    opts.userSpellFile = null;
                }
            }

            // check the outdir and create if necessary
            // check the command line
            if (null == opts.outputDir) {
                // check the config file
                if (null == config.get(OUTDIR_TOKEN)) {
                    System.out.println("YAML configuration file must include the "
                            + OUTDIR_TOKEN + " field.");
                    return false;
                } else {
                    opts.outputDir = config.get(OUTDIR_TOKEN).toString();

                    // create the outdir if it doesn't already exist, but not in analyze mode
                    if ((0 == opts.analyze) && !ensureDirExists(opts.outputDir, OUTDIR_TOKEN)) {
                        return false;
                    }

                    // replace with absolute path
                    File outDir = new File(opts.outputDir);
                    opts.outputDir = outDir.getAbsolutePath();
                }
            } else {
                // create the outdir if it doesn't already exist, but not in analyze mode
                if ((0 == opts.analyze) && !ensureDirExists(opts.outputDir, OUTDIR_TOKEN)) {
                    return false;
                }

                // replace with absolute path
                File outDir = new File(opts.outputDir);
                opts.outputDir = outDir.getAbsolutePath();
            }

            // check boolean options
            opts.tokenOpts.ignoreURLs = false;
            opts.tokenOpts.ignoreNumerals = false;
            opts.tokenOpts.ignoreHashtags = false;
            opts.tokenOpts.ignoreAtMentions = false;
            opts.tokenOpts.ignoreAllButAlphanum = false;
            opts.tokenOpts.ignoreRetweets = false;
            opts.tokenOpts.disableStemming = false;
            opts.tokenOpts.disableAllFilters = false;
            if (usingTwitter) {
                opts.tokenOpts.ignoreURLs
                        = getBooleanOption(config, IGNORE_URL_TOKEN);
                opts.tokenOpts.ignoreHashtags
                        = getBooleanOption(config, IGNORE_HASHTAGS_TOKEN);
                opts.tokenOpts.ignoreAtMentions
                        = getBooleanOption(config, IGNORE_AT_MENTIONS_TOKEN);
                opts.tokenOpts.ignoreRetweets
                        = getBooleanOption(config, IGNORE_RETWEETS_TOKEN);
            }

            opts.tokenOpts.ignoreNumerals
                    = getBooleanOption(config, IGNORE_NUMERALS_TOKEN);
            opts.tokenOpts.ignoreAllButAlphanum
                    = getBooleanOption(config, IGNORE_ALL_BUT_ALPHANUM);
            opts.tokenOpts.disableStemming
                    = getBooleanOption(config, DISABLE_STEMMING_TOKEN);
            opts.tokenOpts.disableAllFilters
                    = getBooleanOption(config, DISABLE_CUSTOM_FILTERS_TOKEN);

            if (null != objIndir) {
                opts.useMongo = false;

                // filesystem input
                opts.inputDir = objIndir.toString();
                if (!isValidDir(opts.inputDir, INDIR_TOKEN)) {
                    return false;
                }

                // replace with absolute path
                File indir = new File(opts.inputDir);
                opts.inputDir = indir.getAbsolutePath();
            } else {
                opts.useMongo = true;

                if (!loadMongoConfig(config, opts)) {
                    return false;
                }
            }

            // opts.analyze must be > 0
            if (opts.analyze < 0) {
                System.out.println("The --" + ARG_ANALYZE + " option must be"
                        + " a nonnegative integer.");
                return false;
            }

            return true;

        }
    }

    /**
     * This method loads the mongo details from the config file
     * 
     * @param   config  map of config details from file
     * @return  opts    options object to fill with config details
     */
    public boolean loadMongoConfig(Map config, Options opts) {
        // get host field
        if (null == config.get(MONGO_HOST_TOKEN)) {
            System.out.println("Mongo configuration must include the "
                    + MONGO_HOST_TOKEN + " field.");
            return false;
        } else {
            opts.host = config.get(MONGO_HOST_TOKEN).toString();

        }
        // get port field
        if (null == config.get(MONGO_PORT_TOKEN)) {
            System.out.println("Mongo configuration must include the "
                    + MONGO_HOST_TOKEN + " field.");
            return false;
        } else {
            try {
                opts.port = Integer.parseInt(config.get(MONGO_PORT_TOKEN).toString());
            } catch (NumberFormatException nfe) {
                System.out.println("The " + MONGO_PORT_TOKEN + " field must be a valid integer.");
                return false;
            }
        }
        // get database field
        if (null == config.get(MONGO_DB_TOKEN)) {
            System.out.println("Mongo configuration must include the "
                    + MONGO_DB_TOKEN + " field.");
            return false;
        } else {
            opts.database = config.get(MONGO_DB_TOKEN).toString();

        }
        // get collection field
        // check if it was on the command line
        if (null == opts.collection) {
            // check the config file
            if (null == config.get(MONGO_COL_TOKEN)) {
                System.out.println("Mongo configuration must include the "
                        + MONGO_COL_TOKEN + " field.");
                return false;
            } else {
                opts.collection = config.get(MONGO_COL_TOKEN).toString();

            }
        }
        // get limit field
        if (null == config.get(MONGO_LIM_TOKEN)) {
            System.out.println("Mongo configuration must include the "
                    + MONGO_LIM_TOKEN + " field.");
            return false;
        } else {
            try {
                opts.limit = Integer.parseInt(config.get(MONGO_LIM_TOKEN).toString());
            } catch (NumberFormatException nfe) {
                System.out.println("The " + MONGO_LIM_TOKEN + " field must be a valid integer.");
                return false;
            }

        }
        // get include field
        if (null == config.get(MONGO_IN_TOKEN)) {
            opts.include = "";
        } else {
            opts.include = config.get(MONGO_IN_TOKEN).toString();

        }
        // get exclude field
        if (null == config.get(MONGO_EX_TOKEN)) {
            opts.exclude = "";
        } else {
            opts.exclude = config.get(MONGO_EX_TOKEN).toString();

        }
        return true;
    }

    /**
     * This method prints out the configuration details before running the applicaiton.
     * 
     * @param   opts    options object containing config settings
     */
    public void printOpts(Options opts) {
        System.out.println("Configuration data: \n");
        System.out.print("\t   Configuration file: ");
        System.out.println(opts.configFile);

        System.out.print("\t   Default slang file: ");
        if (null != opts.defaultSlangFile) {
            System.out.println(opts.defaultSlangFile);
        } else {
            System.out.println(NONE);
        }

        System.out.print("\tDefault stopword file: ");
        if (null != opts.defaultStopFile) {
            System.out.println(opts.defaultStopFile);
        } else {
            System.out.println(NONE);
        }

        System.out.print("\t   User stopword file: ");
        if (null != opts.userStopFile) {
            System.out.println(opts.userStopFile);
        } else {
            System.out.println(NONE);
        }

        System.out.print("\tDefault spelling file: ");
        if (null != opts.defaultSpellFile) {
            System.out.println(opts.defaultSpellFile);
        } else {
            System.out.println(NONE);
        }

        System.out.print("\t   User spelling file: ");
        if (null != opts.userSpellFile) {
            System.out.println(opts.userSpellFile);
        } else {
            System.out.println(NONE);
        }

        if (!opts.useMongo) {
            System.out.print("\t\t    Input dir: ");
            System.out.println(opts.inputDir);
        } else {
            // print MongoDB config data
            System.out.print("\t\t         Host: ");
            System.out.println(opts.host);
            System.out.print("\t\t         Port: ");
            System.out.println(opts.port);
            System.out.print("\t\t     Database: ");
            System.out.println(opts.database);
            System.out.print("\t\t   Collection: ");
            System.out.println(opts.collection);
        }

        System.out.print("\t               Outdir: ");
        System.out.println(opts.outputDir);
        System.out.print("\t             Analyzer: ");
        System.out.println(opts.analyzerName);

        System.out.print("       Disable custom filters: ");
        System.out.println(opts.tokenOpts.disableAllFilters ? YES : NO);

        if (!opts.tokenOpts.disableAllFilters) {
            if (opts.analyzerName.toLowerCase().contains("twitter")) {
                // valid only for Twitter
                System.out.print("\t          Ignore URLs: ");
                System.out.println(opts.tokenOpts.ignoreURLs ? YES : NO);
                System.out.print("\t      Ignore hashtags: ");
                System.out.println(opts.tokenOpts.ignoreHashtags ? YES : NO);
                System.out.print("\t   Ignore at-mentions: ");
                System.out.println(opts.tokenOpts.ignoreAtMentions ? YES : NO);
                System.out.print("\t      Ignore retweets: ");
                System.out.println(opts.tokenOpts.ignoreRetweets ? YES : NO);
            } else {
                // valid only for standard analyzer
                System.out.print("    Ignore all but <ALPHANUM>: ");
                System.out.println(opts.tokenOpts.ignoreAllButAlphanum ? YES : NO);
            }

            System.out.print("\t      Ignore numerals: ");
            System.out.println(opts.tokenOpts.ignoreNumerals ? YES : NO);
            System.out.print("\t     Disable stemming: ");
            System.out.println(opts.tokenOpts.disableStemming ? YES : NO);
        }

        System.out.println();
    }

    /**
     * This method displays command line options.
     */
    public void showHelp() {
        String usage = "java -jar /path/to/DocIndexer.jar\n"
                + "\t\t --" + ARG_CONFIGFILE + "    <filename>     YAML configuration file\n"
                + "\t\t[--" + ARG_ANALYZE + "        n   ]     show analysis for first n documents; do not index\n";

        System.out.println("\nUsage: " + usage);
    }

    /**
     * This method parses the command line options.
     * 
     * @param   args    command line arguments
     * @param   opts    options object containing config settings
     */
    public boolean parse(String[] args, Options opts) {
        // all defaults are set in config.yml
        opts.analyze = 0;
        opts.configFile = null;
        opts.showHelp = false;

        int k = 0;
        boolean ok = true;

        while (k < args.length) {
            char c0 = args[k].charAt(0);
            char c1 = args[k].charAt(1);

            if (('-' == c0) && (('h' == c1) || ('H' == c1))) {
                // user specified short help option
                opts.showHelp = true;
                break;
            } else if (('-' == c0) && ('-' == c1)) {
                // found two dashes, so extract arg name                
                String thisArg = args[k].substring(2);
                String lowercaseArg = thisArg.toLowerCase();

                // arg requires an option
                if (args.length < k + 1) {
                    System.err.println("Missing option for argument " + thisArg);
                    return false;
                }

                switch (lowercaseArg) {
                    case ARG_CONFIGFILE:
                        opts.configFile = args[k + 1];
                        k += 2;
                        break;
                    case ARG_ANALYZE:
                        opts.analyze = Integer.parseInt(args[k + 1]);
                        k += 2;
                        break;
                    case ARG_COL:
                        opts.collection = args[k + 1];
                        k += 2;
                        break;
                    case ARG_OUTDIR:
                        opts.outputDir = args[k + 1];
                        k += 2;
                        break;
                    case ARG_HELP:
                        opts.showHelp = true;
                        break;
                    default:
                        System.err.println("Unknown option: " + args[k].substring(2));
                        ok = false;
                        break;
                }

                if (!ok) {
                    return false;
                }
            } else {
                System.err.println("Invalid command line.");
                return false;
            }

            if (opts.showHelp) {
                break;
            }
        }

        return true;
    }
}
