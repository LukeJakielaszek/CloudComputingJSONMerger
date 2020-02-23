/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cloudcomputingjsonmerger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javatuples.Pair;
import org.json.JSONObject;

/**
 *
 * @author jakie
 */
public class CloudComputingJSONMerger {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // ensure 1 argument is passed through
        if(args.length != 1){
            System.out.println("Error: Must supply only 1 argument.");
            System.exit(-1);
        }
        
        // store argument
        String directory = args[0];
        System.out.println(directory);
        
        File[] files = getDirContents(directory);
        
        MergeJSONFiles(files);
        
    }
    
    public static Map<String, List<JSONObject>> MergeJSONFiles(File[] files){
        // list to hole like files
        Map<String, List<Pair<String, File>>> groupedFiles = groupFiles(files);
        
        Map<String, List<JSONObject>> processedObjects = new HashMap<>();
        
        for(String articleId : groupedFiles.keySet()){
            // get the list of files for the same article ID
            List<Pair<String, File>> articleFiles = groupedFiles.get(articleId);
            
            System.out.println(articleId + ":");
            for(Pair<String, File> DateFilePair : articleFiles){
                String date = DateFilePair.getValue0();
                File curFile = DateFilePair.getValue1();
                
                System.out.println("\t" + curFile.getName() + " " + date);
            }
            
        }
        
        return processedObjects;
    }
    
    // groups files with same article id in map
    public static Map<String, List<Pair<String, File>>> groupFiles(File[] files){
        // list to hole like files
        Map<String, List<Pair<String, File>>> groupedFiles = new HashMap<>();
        
        int file_count = 0;
        for(File file : files){
            // get the name of the file
            String fullName = file.getName();
            
            // split by . to find extension
            String[] fileProc = fullName.split("\\.");
            if(fileProc.length != 2 || !fileProc[1].equals("txt")){
                System.out.println("Skipping " + fullName + " due to extension");
                // skip non text files
                continue;
            }
            
            // obtain only the file name
            String fname = fileProc[0];
            
            // split by [articleid , date]
            String[] fname_date = fname.split("_");
            if(fname_date.length != 2){
                System.out.println("Skipping " + fullName + " due to invalid name format");
                // skip names that do not follow convention
                continue;
            }
            
            // parse the article id and date
            String articleID = fname_date[0];
            String date = fname_date[1];
            
            // pair the file with its date
            Pair<String, File> fileDatePair = Pair.with(date, file);
           
            if(groupedFiles.containsKey(articleID)){
                // if we have seen this article, add the pair
                groupedFiles.get(articleID).add(fileDatePair);
            }else{
                // if not, add the key and pair
                ArrayList<Pair<String, File>> temp = new ArrayList<>();
                temp.add(fileDatePair);
                groupedFiles.put(articleID, temp);
            }
            
            // log the # of files processed
            file_count++;
        }
        
        // display total valid files
        System.out.println("Processing " + file_count + " files");
        
        // return our mapping
        return groupedFiles;
    }
    
    // gets all files below a directory
    public static File[] getDirContents(String folder){
        // attempt to open the file
        File directory = new File(folder);
        
        if(directory == null){
            // did file open?
            System.out.println("Error: Invalid Path");
            System.exit(-3);
        }else if(!directory.isDirectory()){
            // is file directory?
            System.out.println("Error: Not a valid directory");
            System.exit(-2);
        }
        
        // list directory contents
        File[] files = directory.listFiles();
        
        // return list of files
        return files;
    }
    
}
