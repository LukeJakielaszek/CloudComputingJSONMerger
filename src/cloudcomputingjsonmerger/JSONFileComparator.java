package cloudcomputingjsonmerger;


import java.util.Comparator;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jakie
 */
public class JSONFileComparator implements Comparator<JSONFile>{
    @Override
    public int compare(JSONFile o1, JSONFile o2) {
        // sort in ascending order by date
        if(o1.date.before(o2.date)){
            return -1;
        }else if(o1.date.after(o2.date)){
            return 1;
        }else{
            return 0;
        }
    }    
}
