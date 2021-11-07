package com.christopher;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.TreeMap;

import com.christopher.helper.PageRetriever;
import com.christopher.objects.CacheEntry;
import com.christopher.objects.HeapEntry;
import com.christopher.objects.LexiconEntry;
import com.christopher.objects.PageTableEntry;

public class App 
{
    public static void main( String[] args )
    {
        TreeMap<String, LexiconEntry> lexicon = new TreeMap<>();
        TreeMap<Integer, PageTableEntry> pageTable = new TreeMap<>();
        double[] arrAveragePage = new double[1];
        loadLexiconAndPageTable(lexicon, pageTable, arrAveragePage);
        PriorityQueue<HeapEntry> selectedDocuments = new PriorityQueue<>();
        HashMap<String, PriorityQueue<HeapEntry>> cache =  new HashMap<>();
        PriorityQueue<CacheEntry> queryFrequencyHeap = new PriorityQueue<>();
        HashMap<String, Integer> queryFrequencyMap = new HashMap<>();
        // TODO put exit condition
        System.out.println("Please input 'and' or 'or' to set type of query or 'exit' to exit the application");
        // TODO implement this to set conjunctive or disjonctive
        // Scanner in = new Scanner(System.in);
        // TODO maybe if term has already been returned, no need to do it again.
        String query = "grasshopper armadillo";
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        // TODO check if disjunctive or conjunctive, check in cache
        if (cache.containsKey(query)){
            selectedDocuments = cache.get(query);
        }else{
            Date dateStart = new Date(System.currentTimeMillis());
            System.out.println(formatter.format(dateStart));
            processConjunctiveQuery(query, lexicon, pageTable.size(), selectedDocuments, arrAveragePage[0], pageTable);

            // TODO change to query + or or and
            int prevFreq = 0;
            if (queryFrequencyMap.containsKey(query)){
                prevFreq = queryFrequencyMap.get(query);
            }
            queryFrequencyMap.put(query, prevFreq + 1);
            if (queryFrequencyHeap.size() >= 100){
                // to compare with last entry
                CacheEntry currentEntry = queryFrequencyHeap.peek();
                if (prevFreq + 1 > currentEntry.getFrequency()){
                    CacheEntry toBeRemovedEntry = queryFrequencyHeap.poll();
                    cache.remove(toBeRemovedEntry.getQuery());
                }
            }
            CacheEntry newEntry = new CacheEntry();
            newEntry.setFrequency(prevFreq + 1);
            newEntry.setQuery(query);
            queryFrequencyHeap.add(newEntry);
            cache.put(query, selectedDocuments);
        }
        Date dateEnd = new Date(System.currentTimeMillis());
        System.out.println(formatter.format(dateEnd));
        HeapEntry entry = selectedDocuments.poll();
        PageRetriever pageRetriever = new PageRetriever();
        pageRetriever.getPage(entry.getDocId());
    }
    public static void loadLexiconAndPageTable(TreeMap<String, LexiconEntry> lexicon, TreeMap<Integer, PageTableEntry> pageTable, double[] arrAveragePage){
        String input = new String();
        long total = 0;
        try{
            DataInputStream dataInputLexicon = new DataInputStream(new BufferedInputStream(new FileInputStream("Lexicon.bin")));
            while(dataInputLexicon.available() > 0){
                input = dataInputLexicon.readUTF().trim();
                String[] split = input.split(",");
                if (split.length > 5){
                    continue;
                }
                LexiconEntry newLexiconEntry = new LexiconEntry(split[0], Long.parseLong(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]));
                lexicon.put(split[0], newLexiconEntry);
            }
            dataInputLexicon.close();
            DataInputStream dataInputPageTable = new DataInputStream(new BufferedInputStream(new FileInputStream("PageTable.bin")));
            while(dataInputPageTable.available() > 0){
                input = dataInputPageTable.readUTF();
                String[] split = input.split(" ");
                int docId = Integer.parseInt(split[0]);
                String url = split[1];
                int length = Integer.parseInt(split[2].trim());
                total += length;
                PageTableEntry newPageTableEntry = new PageTableEntry(docId, url, length);
                pageTable.put(docId, newPageTableEntry);
            }
            dataInputPageTable.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        double average = total/pageTable.size();
        arrAveragePage[0] = average;
    }
    /**
     * Method to find next posting in the list with docID > k and return the docId
     * @param list
     * @param k
     */
    public static boolean nextGEQ(int[] resultArray, int k, int lengthOfList, int[] docIdFreq){
        int numberOfList = lengthOfList / 64;
        int remainder = lengthOfList % 64;
        // int[] docIdFreq = new int[2];
            // iterate through maximums. if there is one greater than k, search through that list. else search in remainder.
        for (int i = 1; i < numberOfList + 1; i++){
            // if maximum greater than k, go to that list
            if (resultArray[i] >= k){
                // 1 + length of list + ( (i - 1) * lengthOfList * 2)
                int startingIndexOfList = 1 + numberOfList + ((i - 1) * (64 * 2));
                for (int j = startingIndexOfList; j < startingIndexOfList + 64; j++){
                    if(resultArray[j] >= k){
                        docIdFreq[0] = resultArray[j];
                        docIdFreq[1] = resultArray[j + 64];
                        return true;
                    }
                }
            }
        }
        // else nothing has been found in the 64 blocks, check the remainder
        int startingIndexOfList = 1 + numberOfList + (numberOfList * (64 * 2));
        for (int i = startingIndexOfList; i < startingIndexOfList + remainder; i++){
            if (resultArray[i] >= k){
                // doc id
                docIdFreq[0] = resultArray[i];
                // frequency
                docIdFreq[1] = resultArray[i + remainder];
                return true;
            }
        }
        return false;
    }
    /**
     * Compute score for a document using BM25
     */
    public static double computeScore(int docCount, int lengthOfList, int frequencyInDocument, double averageLengthOfDocuments, int numberOfWordsInDoc){
        double k1 = 1.2;
        double b = 0.75;
        double k = k1 * ((1-b) + (b * (numberOfWordsInDoc/averageLengthOfDocuments)));
        double inverseDocumentFrequency = Math.log((docCount - lengthOfList + 0.5)/ (lengthOfList + 0.5));
        double rightPart = ((k1 + 1) * frequencyInDocument)/ (k + frequencyInDocument);
        return inverseDocumentFrequency * rightPart;
    }
    /**
     * Open inverted list for a term
     * @param term
     */
    public static int[] openList(String term, TreeMap<String, LexiconEntry> lexicon){
        // if inverted list has less than 64, there will be no max for that chunk
        LexiconEntry lexiconEntry = lexicon.get(term);
        File file = new File("InvertedIndex.bin");
        // List<Integer> result = new ArrayList<>();
        int numberOfList = lexiconEntry.getLength() / 64;
        int lengthInvertedList = 1 + numberOfList + (2 * lexiconEntry.getLength());
        int[] resultArray = new int[lengthInvertedList];
        try(FileInputStream fin = new FileInputStream(file);){
            // TODO check if makes sense to do like that
            fin.skip(lexiconEntry.getStartBlock() + lexiconEntry.getBlockNumber());
            // read in inverted list based on length
            // metadata--- number in list, max1, max2,...maxn,post1, post2, ...postn,freq1,freq2,...freqn
            
            int i = 0;
            int remainder = lexiconEntry.getLength() % 64;
            int count = 0;
            int offset = lexiconEntry.getOffset();
            
            // read in length of list
            i  = varDecode(i, 0, fin, resultArray);
            // read max number of each list
            for(int j = 0; j < numberOfList; j++){
                i = varDecode(i, offset, fin, resultArray);
            }
            // while (result.size() < lengthInvertedList){
            while(i < lengthInvertedList){
                // read posting
                if (numberOfList > 0){
                    while(count < 64){
                        i = varDecode(i, offset, fin, resultArray);
                        count++;
                    }
                    count = 0;
                    // numberOfList--;
                }else if(remainder > 0){
                    while(count < remainder){
                        i = varDecode(i, offset, fin, resultArray);
                        count++;
                    }
                    count = 0;
                }
                // read frequency
                if (numberOfList > 0){
                    while(count < 64){
                        i = varDecode(i, 0, fin, resultArray);
                        count++;
                    }
                    count = 0;
                    numberOfList--;
                }else if(remainder > 0){
                    while(remainder > 0){
                        i = varDecode(i, 0, fin, resultArray);
                        remainder--;
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return resultArray;
    }

    public void closeList(){

    }
    /**
     * Return frequency for that term
     * @param term
     * @return
     */
    public int getFrequency(String term){
        return 0;
    }

    public static void processConjunctiveQuery(String query, TreeMap<String, LexiconEntry> lexicon, int docCount, PriorityQueue<HeapEntry> selectedDocuments, double averagePageLength, TreeMap<Integer, PageTableEntry> pageTable){
        String[] terms = query.split(" ");
        HashMap<String, int[]> map =  new HashMap<>();
        List<int[]> invertedListList = new ArrayList<>();
        for (String term : terms){
            if (map.containsKey(term)){
                continue;
            }
            // List<Integer> list = openList(term, lexicon);
            int[] resultArray = openList(term, lexicon);
            map.put(term, resultArray);
            invertedListList.add(resultArray);
        }
        // sort list in ascending order of size
        Collections.sort(invertedListList, new Comparator<int[]>(){
            public int compare(int[] a1, int[] a2) {
                return a1.length - a2.length; // assumes you want biggest to smallest
            }
        });
        boolean isFinished = false;
        boolean isfirst = true;
        int currentDocId = 0;
        int newDocId = 0;
        int listNumber = 0;
        while (!isFinished){
            List<List<Integer>> scoreParameterList = new ArrayList<>();
            // for(int[] currentList : invertedListList){
            for(String term : terms){
                int[] currentList = map.get(term);
                int[] docIdFreq = new int[2];
                List<Integer> scoreParameter = new ArrayList<>();
                if (!nextGEQ(currentList, currentDocId, currentList[0], docIdFreq)){
                    isFinished = true;
                    break;
                }
                // if first list, set currentDocId to the first docId found in list and go to next list.
                if (isfirst){
                    currentDocId = docIdFreq[0];
                    isfirst = false;
                    listNumber++;
                    scoreParameter.add(currentList[0]);
                    scoreParameter.add(docIdFreq[1]);
                    scoreParameter.add(pageTable.get(docIdFreq[0]).getLength());
                    scoreParameterList.add(scoreParameter);
                    continue;
                }
                newDocId = docIdFreq[0];
                if (newDocId > currentDocId){
                    currentDocId = newDocId;
                    listNumber = 0;
                    break;
                }else if (newDocId == currentDocId){
                    scoreParameter.add(currentList[0]);
                    scoreParameter.add(docIdFreq[1]);
                    scoreParameter.add(pageTable.get(docIdFreq[0]).getLength());
                    scoreParameterList.add(scoreParameter);
                    listNumber++;
                    // if present in all documents, compute BM25 score
                    if (listNumber == invertedListList.size()){
                        double score = 0;
                        for (List<Integer> currentParameter : scoreParameterList){
                            score += computeScore(docCount, currentParameter.get(0), currentParameter.get(1), averagePageLength, currentParameter.get(2));
                        }
                        HeapEntry newEntry = new HeapEntry();
                        newEntry.setDocId(currentDocId);
                        newEntry.setImpactScore(score);
                        
                        if (selectedDocuments.size() >= 10){
                            HeapEntry lowestEntry = selectedDocuments.peek();
                            if (lowestEntry.getImpaceScore() < score){
                                selectedDocuments.poll();
                                selectedDocuments.add(newEntry);
                            }
                        }else{
                            selectedDocuments.add(newEntry);
                        }
                        listNumber = 0;
                        currentDocId++;
                    }
                }
            }
        }
    }
    public static void processDisjunctiveQuery(String query, TreeMap<String, LexiconEntry> lexicon, int docCount, PriorityQueue<HeapEntry> selectedDocuments){

    }
    public static int varDecode(int i, int offset, FileInputStream fin, int[] resultArray){
        int val = 0;
        int shift = 0;
        int b = -1;
        try{
            while((0x80 & (b = fin.read()))==0){
                val |= (b & 127) << shift;
                shift = shift + 7;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        val |= (b - 128) << shift;
        resultArray[i] = val + offset;
        i++;
        return i;
    }
}
