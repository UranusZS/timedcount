package org.timedcount.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: StringUtil
 * @Description: Operations of Strings
 * @author ZS
 * @date Feb 3, 2016 4:13:52 PM
 *
 */
public class StringUtil {
    
    /**
     * 
     * @Title: splitSentences
     * @Description: Split sentences into words by space
     * @param @param sentence
     * @param @return  
     * @return List<String> 
     * @throws
     */
    public static List<String> splitSentences(String sentence) {
        return splitSentences(sentence, " ");
    }
    
    /**
     * 
     * @Title: splitSentences
     * @Description: Split sentences into words by spliter
     * @param @param sentence
     * @param @param spliter
     * @param @return  
     * @return List<String> 
     * @throws
     */
    public static List<String> splitSentences(String sentence, String spliter) {
        String[] words = sentence.split(spliter);
        List<String> list=new ArrayList<String>();
        for(String word : words){
            word = word.trim();
            if(!word.isEmpty()){
                word = word.toLowerCase();
                list.add(word);
            }
        }
        return list;
    }
}
