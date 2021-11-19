package com.christopher.objects;

import java.util.HashMap;
import java.util.Properties;

public class HeapEntry implements Comparable<HeapEntry>{
    private Double impactScore;
    private int docId;
    private String term;
    // length of inverted list, frequency in document, length of page
    HashMap<String, int[]> termParametersMap;
    public void setImpactScore(Double impactScore){
        this.impactScore = impactScore;
    }
    public Double getImpaceScore(){
        return this.impactScore;
    }
    public void setDocId(int docId){
        this.docId = docId;
    }
    public int getDocId(){
        return this.docId;
    }
    public void setTerm(String term){
        this.term = term;
    }
    public String getTerm(){
        return this.term;
    }
    public void setTermParametersMap(HashMap<String, int[]> termParametersMap){
        this.termParametersMap = termParametersMap;
    }
    public HashMap<String, int[]> getTermParametersMap(){
        return this.termParametersMap;
    }
    @Override
    public int compareTo(HeapEntry o) {
        // TODO Auto-generated method stub
        
        return this.impactScore.compareTo(o.getImpaceScore());
    }
}
