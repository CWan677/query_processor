package com.christopher.objects;

import java.util.HashMap;

public class ResponseObject {
    private String snippet;
    private double score;
    private int frequency;   // frequency in document
    HashMap<String, int[]> termParametersMap;

    public void setSnippet(String snippet){
        this.snippet = snippet;
    }
    public String getSnippet(){
        return this.snippet;
    }
    public void setFrequency(int frequency){
        this.frequency = frequency;
    }
    public int getFrequency(){
        return this.frequency;
    }
    public void setScore(double score){
        this.score = score;
    }
    public double getScore(){
        return this.score;
    }
    public void setTermParametersMap(HashMap<String, int[]> termParametersMap){
        this.termParametersMap = termParametersMap;
    }
    public HashMap<String, int[]> getTermParametersMap(){
        return this.termParametersMap;
    }
}
