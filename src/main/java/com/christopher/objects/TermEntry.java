package com.christopher.objects;

public class TermEntry implements Comparable<TermEntry>{
    private String term;
    private Integer size; // length of list

    public void setTerm(String term){
        this.term = term;
    }
    public String getTerm(){
        return this.term;
    }

    public void setSize(int size){
        this.size = size;
    }
    public Integer getSize(){
        return this.size;
    }
    @Override
    public int compareTo(TermEntry o) {
        return this.getSize().compareTo(o.getSize());
    }
}
