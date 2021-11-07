package com.christopher.objects;

public class LexiconEntry implements Comparable<LexiconEntry>{
    private String term;
    private long startBlock; // block at which it starts
    private int length; // length of list
    private int blockNumber; // data block at which the list starts
    private int offset; // difference substracted from each docids to make the list smaller

    public LexiconEntry(String term, long startBlock, int length, int blockNumber, int offset){
        this.term = term;
        this.startBlock = startBlock;
        this.length = length;
        this.blockNumber = blockNumber;
        this.offset = offset;
    }
    public void setTerm(String term){
        this.term = term;
    }
    public String getTerm(){
        return this.term;
    }
    public void setStartBlock(long startBlock){
        this.startBlock = startBlock;
    }
    public long getStartBlock(){
        return this.startBlock;
    }
    public void setLength(int length){
        this.length = length;
    }
    public int getLength(){
        return this.length;
    }
    public void setBlockNumber(int blockNumber){
        this.blockNumber = blockNumber;
    }
    public int getBlockNumber(){
        return this.blockNumber;
    }
    public void setOffset(int offset){
        this.offset = offset;
    }
    public int getOffset(){
        return this.offset;
    }
    @Override
    public int compareTo(LexiconEntry o) {
        return this.getTerm().compareTo(o.getTerm());
    }
    public String toString(){
        return this.term + "," + this.startBlock + "," + this.length + "," + this.blockNumber + ","+ this.offset + "\n";
    }
}
