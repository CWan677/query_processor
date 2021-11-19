package com.christopher.objects;

public class SnippetEntry implements Comparable<SnippetEntry>{
    private String snippet;
    private Integer score; // length of list
    private int lineNumber;
    public void setSnippet(String snippet){
        this.snippet = snippet;
    }
    public String getSnippet(){
        return this.snippet;
    }

    public void setScore(int score){
        this.score = score;
    }
    public Integer getScore(){
        return this.score;
    }

    public void setLineNumber(int lineNumber){
        this.lineNumber = lineNumber;
    }
    public Integer getLineNumber(){
        return this.lineNumber;
    }
    @Override
    public int compareTo(SnippetEntry o) {
        return this.getScore().compareTo(o.getScore());
    }
}
