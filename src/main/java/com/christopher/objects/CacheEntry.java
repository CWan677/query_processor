package com.christopher.objects;

import java.util.PriorityQueue;

public class CacheEntry implements Comparable<CacheEntry>{
    private Integer frequency;
    private String query;
    private PriorityQueue<HeapEntry> heapEntry;

    public void setFrequency(Integer frequency){
        this.frequency = frequency;
    }
    public Integer getFrequency(){
        return this.frequency;
    }
    public void setHeapEntry(PriorityQueue<HeapEntry> heapEntry){
        this.heapEntry = heapEntry;
    }
    public PriorityQueue<HeapEntry> getHeapEntry(){
        return this.heapEntry;
    }
    public void setQuery(String query){
        this.query = query;
    }
    public String getQuery(){
        return this.query;
    }
    @Override
    public int compareTo(CacheEntry o) {
        // TODO Auto-generated method stub
        return this.getFrequency().compareTo(o.getFrequency());
    }


}
