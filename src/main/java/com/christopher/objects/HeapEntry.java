package com.christopher.objects;

import java.util.Properties;

public class HeapEntry implements Comparable<HeapEntry>{
    private Double impactScore;
    private int docId;
    private String term;

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


    @Override
    public int compareTo(HeapEntry o) {
        // TODO Auto-generated method stub
        
        return this.impactScore.compareTo(o.getImpaceScore());
    }
}
