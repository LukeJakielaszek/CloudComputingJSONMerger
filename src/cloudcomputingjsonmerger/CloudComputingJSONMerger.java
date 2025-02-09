/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cloudcomputingjsonmerger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
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
        Scanner scan = new Scanner(System.in);
        
        // get directory
        System.out.print("Enter an input directory >");
        String directory = scan.nextLine();
        System.out.println("Input Directory [" + directory + "]");
        
        System.out.print("Enter an output directory >");
        String outDirectory = scan.nextLine();
        System.out.println("Out Directory: [" + outDirectory + "]");       
        
        File[] files = getDirContents(directory);
        
        MergeJSONFiles(files, outDirectory);
        
    }
    
    // adds completely unique elements to a json array if they exist
    public static void updateJSONArray(JSONArray storedNode, JSONArray curNode){
        Set<String> storedSet = new HashSet<>();

        // create a mapping from every element in our source array to its index
        for(int i = 0; i < storedNode.length(); i++){
            String srcObject = storedNode.get(i).toString();
            storedSet.add(srcObject);
        }
        
        // loop through the new array to find unique elements
        for(int i = 0; i < curNode.length(); i++){
            String curObject = curNode.get(i).toString();
            
            if(!storedSet.contains(curObject)){
                // a unique array element has been found
                // add it to the stored array
                storedNode.put(curObject);
            }
        }
    }
    
    // recursively adds unique elements to a jsonobject
    public static void updateJSONObject(JSONObject storedNode, JSONObject curNode){        
        // loop through all keys of the node
        for (String key : curNode.keySet()) {
            if (storedNode.has(key)) {
                // both nodes have the same key
                // check what type of value we map to
                
                // used to check for jsonobjects
                JSONObject srcObj = null;
                JSONObject curObj = null;
                
                // used to check for jsonarrays
                JSONArray srcArr = null;
                JSONArray curArr = null;
                
                if(((srcObj = getJSONObject(storedNode.get(key))) != null) && ((curObj = getJSONObject(curNode.get(key))) != null)){
                    // both values are a json object
                    updateJSONObject(srcObj, curObj);
                }else if(((srcArr = getJSONArray(storedNode.get(key))) != null) && ((curArr = getJSONArray(curNode.get(key))) != null)){
                    // both values are json array
                    updateJSONArray(srcArr, curArr);
                }else{
                    if(srcObj != null || curObj != null || srcArr != null || curArr != null){
                        // data mismatch between documents detected
                        System.err.println("ERROR: Document structure mismatch detected");
                    }
                    
                    // value is not json, therefore we do nothing as we already 
                    //have the most recent version
                    continue;
                }
                
            } else {
                // our stored node does not have the key, so we add it
                storedNode.put(key, curNode.get(key));
            }
        }
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
                List<JSONObject> cursorArray = getFileAsArray(curFile);
                
                // check to see if json read correctly
                if(cursorArray == null || cursorArray.size() == 0){
                    System.out.println("ERROR: Failed to read json from file " + curFile.getFile().getName());
                    // skip files with error
                    continue;
                }
                
                // loop through each cursor
                int nodeCount = 0;
                for (JSONObject fileWrapper : cursorArray) {

                    // get the JSON node array of the current cursor
                    JSONArray nodeArray = fileWrapper.getJSONArray("response");

                    // loop through every node in the current file
                    for (int i = 0; i < nodeArray.length(); i++) {
                        // get the current node
                        JSONObject curNode = nodeArray.getJSONObject(i);

                        // get the id of the node
                        String id = curNode.getString("id");

                        // check if the current node is unique
                        if (nodeMap.containsKey(id)) {
                            // duplicate ids detected
                            // get our stored duplicate node from map
                            FileNode storedNode = nodeMap.get(id);

                            // update the json object corresponding to the node
                            updateJSONObject(storedNode.node, curNode);
                        } else {
                            // add nodes with unique ids to our map
                            nodeMap.put(id, new FileNode(curNode, id));
                        }
                    }

                    System.out.println("\t\t" + nodeArray.length() + " node(s) in cursor.");
                    nodeCount += nodeArray.length();
                }
                System.out.println("\t\t\t" + nodeCount + " node(s) in file.");
            }
            
            // create a JSON array for output
            JSONArray articleContents = new JSONArray();
            
            System.out.println("\t\t\tTotal Nodes after merge: " + nodeMap.values().size());            
            // add all nodes to array
            for(FileNode node : nodeMap.values()){
                articleContents.put(node);
            }
            
            // store response
            JSONObject finalResponse = new JSONObject();
            finalResponse.put("response", articleContents);
            
            // open the output file
            String outFileName = outDirectory + articleId + ".txt";
            FileWriter outFile = null;
            try {
                // write the array to file
                outFile = new FileWriter(outFileName);
                outFile.write(finalResponse.toString());
            } catch (IOException ex) {
                Logger.getLogger(CloudComputingJSONMerger.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Error: Failed to write [" + articleId + "] to [" + outFileName + "]");
                System.exit(-4);
            }finally{
                try {
                    // close the file
                    outFile.close();
                } catch (IOException ex) {
                    Logger.getLogger(CloudComputingJSONMerger.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    // returns jsonObject or null on failure
    public static JSONObject getJSONObject(Object object){
        JSONObject jsonObject;
        try{
            jsonObject = new JSONObject(object);
        }catch(JSONException ex){
            jsonObject = null;
        }
        
        return jsonObject;
    }
    
    // returns jsonArray or null on failure
    public static JSONArray getJSONArray(Object object){
        JSONArray jsonArray;
        try{
            jsonArray = new JSONArray(object);
        }catch(JSONException ex){
            jsonArray = null;
        }
        
        return jsonArray;
    }
    
    // Used to parse a valid JSON file (this assumes only 1 cursor per file.
    // This is not used in this assignment since the large file > 500 KB has
    // more than 1 cursor in it. However, the cursors are not wrapper in an 
    // array and are therefore invalid JSON
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
    
    // used to parse JSON file with multiple cursors. This is bad JSON
    // and we should not need to handle it, but I never received a response
    // when I emailed Dr. Dragut letting him know of the error in his 5by3
    // folder that was supplied for testing.
    public static List<JSONObject> getFileAsArray(JSONFile jsonFile){
        List<JSONObject> jsonArray = new ArrayList<>();

        // This will reference one line at a time
        String line = null;

        try {
            // Open the file
            FileReader fileReader = new FileReader(jsonFile.getFile().getAbsolutePath());

            // read as a buffer
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            // assume each cursor is on a seperate line
            while ((line = bufferedReader.readLine()) != null) {
                // tokenize the line
                JSONObject cursor = new JSONObject(new JSONTokener(line));;
                
                // track the json object
                jsonArray.add(cursor);
            }
            
            // close the reader
            bufferedReader.close();
            fileReader.close();
        } catch (FileNotFoundException ex) {
            System.out.println("ERROR: Failed to open file [" + jsonFile.getFile().getAbsolutePath() + "]");
        } catch (IOException ex) {
            System.out.println("Error: Failed to read file [" + jsonFile.getFile().getAbsolutePath() + "]");
        }
        
        // return json contents
        return jsonArray;
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
