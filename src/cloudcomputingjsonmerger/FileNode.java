/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cloudcomputingjsonmerger;

import org.json.JSONObject;

/**
 *
 * @author jakie
 */
public class FileNode {
    public JSONObject node;
    public String nodeID;

    public FileNode(JSONObject node, String nodeID) {
        this.node = node;
        this.nodeID = nodeID;
    }
}
