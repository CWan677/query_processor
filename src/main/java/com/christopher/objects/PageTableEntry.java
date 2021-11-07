package com.christopher.objects;

public class PageTableEntry {
    private int docId;
    private String url;
    private int length;

    public PageTableEntry(int docId, String url, int length){
        this.docId = docId;
        this.url = url;
        this.length = length;
    }
    public void setDocId(int docId){
        this.docId = docId;
    }

    public int getDocId(){
        return this.docId;
    }
    public void setUrl(String url){
        this.url = url;
    }
    public String getUrl(){
        return this.url;
    }
    public void setLength(int length){
        this.length = length;
    }
    public int getLength(){
        return this.length;
    }
}
