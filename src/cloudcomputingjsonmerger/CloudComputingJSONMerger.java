/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cloudcomputingjsonmerger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
        if(args.length != 2){
            System.out.println("Error: Must supply 2 arguments (in_directory out_directory).");
            System.exit(-1);
        }
        
        // store arguments
        String directory = args[0];
        System.out.println("Input Directory [" + directory + "]");
        
        String outDirectory = args[1];
        System.out.println("Out Directory: [" + outDirectory + "]");       
        
        File[] files = getDirContents(directory);
        
        MergeJSONFiles(files, outDirectory);
        
        }
    
    public static void MergeJSONFiles(File[] files, String outDirectory){     
        // map to hold all processed JSON object
        Map<String, PriorityQueue<JSONFile>> groupedFiles = groupFiles(files);
        
        // loop through each article
        for(String articleId : groupedFiles.keySet()){
            // get the list of files for the same article ID
            PriorityQueue<JSONFile> pq = groupedFiles.get(articleId);
            
            // a mapping for each node based on nodeid
            Map<String, FileNode> nodeMap = new HashMap<>();
            
            System.out.println(articleId + ":");
            // loop through all files per article
            while(!pq.isEmpty()){
                // obtain file
                JSONFile curFile = pq.poll();
                System.out.println("\t" + curFile.getFile().getName() + " " + curFile.getDate());
                
                // attempt to read file
                JSONObject fileWrapper = getFileContents(curFile);
                
                // check to see if json read correctly
                if(fileWrapper == null){
                    System.out.println("ERROR: Failed to read json from file " + curFile.getFile().getName());
                    // skip files with error
                    continue;
                }
                
                // get the JSON node array
                JSONArray nodeArray = fileWrapper.getJSONArray("response");
                
                // loop through every node in the current file
                for(int i = 0; i < nodeArray.length(); i++){
                    // get the current node
                    JSONObject curNode = nodeArray.getJSONObject(i);
                    
                    // get the id of the node
                    String id = curNode.getString("id");
                    
                    if(nodeMap.containsKey(id)){
                        // duplicate detected
                        FileNode storedNode = nodeMap.get(id);
                        //System.out.println("\t\tDuplicate Node Detected");
                        
                        // loop through all keys of the node
                        for(String key : curNode.keySet()){
                            if(storedNode.node.has(key)){
                                // must check if it is updated (since were doing oldest to
                                // newest, it doesnt matter if its updated
                                
                            }else{
                                // our old node does not have the key, so we add it
                                storedNode.node.put(key, curNode.get(key));
                            }
                        }
                        
                    }else{
                        nodeMap.put(id, new FileNode(curNode, id));
                    }
                }
                
                System.out.println("\t\t" + nodeArray.length() + " node(s) in file.");
            }
            
            // create a JSON array for output
            JSONArray articleContents = new JSONArray();
            
            System.out.println("\t\t\tTotal Nodes after merge: " + nodeMap.values().size());            
            // add all nodes to array
            for(FileNode node : nodeMap.values()){
                articleContents.put(node);
            }
            
            // open the output file
            String outFileName = outDirectory + articleId + ".txt";
            FileWriter outFile;
            try {
                // write the array to file
                outFile = new FileWriter(outFileName);
                outFile.write(articleContents.toString());
                outFile.close();
            } catch (IOException ex) {
                Logger.getLogger(CloudComputingJSONMerger.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Error: Failed to write [" + articleId + "] to [" + outFileName + "]");
                System.exit(-4);
            }
        }
    }
    
    public static JSONObject getFileContents(JSONFile jsonFile){                
        // attempt to read file
        JSONObject fileWrapper;
        try {
            InputStream is = new FileInputStream(jsonFile.getFile().getAbsolutePath());
            fileWrapper = new JSONObject(new JSONTokener(is));
        } catch (FileNotFoundException ex) {
            //skip files with error
            Logger.getLogger(CloudComputingJSONMerger.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (JSONException ex) {
            // skip files with bad json content
            return null;
        }
              
        // return json contents
        return fileWrapper;
    }
    
    // groups files with same article id in map
    public static Map<String, PriorityQueue<JSONFile>> groupFiles(File[] files){
        // list to hole like files
        Map<String, PriorityQueue<JSONFile>> groupedFiles = new HashMap<>();
        
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
            
            // convert the date to a dateobject
            Date testDate;
            try {
                testDate = new SimpleDateFormat("yyyyMMddHHmmss").parse(date);
            } catch (ParseException ex) {
                // skip malformed dates
                Logger.getLogger(CloudComputingJSONMerger.class.getName()).log(Level.SEVERE, null, ex);
                continue;
            }
            
            // pair the file with its date
            JSONFile fileDatePair = new JSONFile(testDate, articleID, file);
           
            if(groupedFiles.containsKey(articleID)){
                // if we have seen this article, add the pair
                groupedFiles.get(articleID).add(fileDatePair);
            }else{
                // if not, add the key and pair with descending order
                PriorityQueue<JSONFile> temp = new PriorityQueue<>(10, new JSONFileComparator().reversed());
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
