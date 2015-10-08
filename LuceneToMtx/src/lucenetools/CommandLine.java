/**************************************************************
Copyright (c) 2015, Georgia Tech Research Institute
All rights reserved.

This unpublished material is the property of the Georgia Tech
Research Institute and is protected under copyright law.
The methods and techniques described herein are considered
trade secrets and/or confidential. Reproduction or distribution,
in whole or in part, is forbidden except by the express written
permission of the Georgia Tech Research Institute.
***************************************************************/



package lucenetools;

import java.io.File;

/**
 *
 * @author Richard Boyd
 */
public class CommandLine 
{
    private final String ARG_INDIR = "indir";
    private final String ARG_OUTDIR = "outdir";
    private final String ARG_FREQ  = "minfreq";
    private final String ARG_MAXPRCT  = "maxprct";
    private final String ARG_FIELDS  = "fields";
    private final String ARG_HELP = "help";
    
    public CommandLine() {} 
    
    /**
     * This method interprets the boolean options from the config file
     * 
     * @param   opts     options object containing config settings
     */
    public boolean isValid(Options opts) 
    {
        File indir = new File(opts.indexDir);
        if (!indir.exists())
        {
            System.err.println("The directory '" + opts.indexDir + 
                    "' does not exist.");
            return false;
        }
        
        if (!indir.isDirectory())
        {
            System.err.println("The argument '" + indir.getPath() +
                    "' is not a directory.");
            return false;
        }
        
        File outdir = new File(opts.outDir);        
        if (!outdir.exists())
        {
            // create the output dir
            System.out.println("Creating directory '" + 
                    outdir.getPath() + "'.");
            try
            {
                new File(opts.outDir).mkdir();
            }
            catch (SecurityException e)
            {
                System.out.println(" caught a " + e.getClass() +
                "\n with message: " + e.getMessage());
                return false;
            }
        }
        
        if (opts.minTermFreq <= 0) 
        {
            System.err.println("The minimum term frequency must be a positive integer.");
            return false;
        }       
        
        return true;
    }
    
    /**
     * This method prints out the configuration details before running the applicaiton.
     * 
     * @param   opts    options object containing config settings
     */
    public void printOpts(Options opts) 
    {
        System.out.println("Command line options: \n");
        System.out.print("\t " + ARG_FREQ + ": ");
        System.out.println(opts.minTermFreq);
        System.out.print("\t   " + ARG_MAXPRCT + ": ");
        System.out.println(opts.maxTermPercentage);
        System.out.print("\t   " + ARG_INDIR + ": ");
        System.out.println(opts.indexDir);
        System.out.print("\t  " + ARG_OUTDIR + ": ");
        System.out.println(opts.outDir);
        System.out.print("\t  " + ARG_FIELDS + ": ");
        System.out.println(opts.fields);
        System.out.println();
    }
    
    /**
     * This method displays command line options.
     */
    public void showHelp() 
    {    
        String usage = "java -jar /path/to/LuceneToMtx.jar\n"
                + "\t\t --" + ARG_INDIR + "      <path to Lucene index dir>\n"
                + "\t\t[--" + ARG_OUTDIR + "]    result files written here\n"
                + "\t\t[--" + ARG_FREQ + " 1] minimum term freq cutoff value\n"
                + "\t\t[--" + ARG_MAXPRCT + " 100] maximum term occurrence\n"
                + "\t\t[--" + ARG_FIELDS + " 0] write out all fields in the index\n";
                
        System.out.println("\nUsage: " + usage);
    }
    
    /**
     * This method parses the command line options.
     * 
     * @param   args    command line arguments
     * @param   opts    options object containing config settings
     */
    public boolean parse(String[] args, Options opts) 
    {        
        // set defaults
        opts.indexDir = "index";
        opts.maxTermPercentage = 100;
        
        // default to current dir
        opts.outDir = System.getProperty("user.dir");
        opts.minTermFreq = 1;
        
        int k=0;
        boolean ok = true;
        
        while (k < args.length) 
        {
            char c0 = args[k].charAt(0);
            char c1 = args[k].charAt(1);
            
            if ( ('-' == c0) && ( ('h' == c1) || ('H' == c1)))
            {
                // user specified short help option
                opts.showHelp = true;
                break;
            }             
            else if ( ('-' == c0) && ('-' == c1)) 
            {
                // found two dashes, so extract arg name                
                String thisArg = args[k].substring(2);
                String lowercaseArg = thisArg.toLowerCase();
                
                // arg requires an option
                if (args.length < k+1) 
                {
                    System.err.println("Missing option for argument " + thisArg);
                    return false;
                }                
                
                switch (lowercaseArg) 
                {
                    case ARG_INDIR:                            
                        opts.indexDir = args[k+1];
                        k += 2;
                        break;
                    case ARG_OUTDIR:
                        opts.outDir = args[k+1];
                        k += 2;
                        break;
                    case ARG_FREQ:
                        opts.minTermFreq = Integer.parseInt(args[k+1]);
                        k += 2;
                        break;
                    case ARG_MAXPRCT:
                        opts.maxTermPercentage = Integer.parseInt(args[k+1]);
                        k += 2;
                        break;
                    case ARG_FIELDS:
                        opts.fields = Integer.parseInt(args[k+1]);
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
                
                if (!ok)
                    return false;
            }
            else
            {
                System.err.println("Invalid command line.");
                return false;
            }
            
            if (opts.showHelp)
                break;
        }
        
        return true;
    }
}
