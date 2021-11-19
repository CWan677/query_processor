package com.christopher.web.controller;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeMap;

import javax.management.QueryEval;

import com.christopher.helper.PageRetriever;
import com.christopher.objects.CacheEntry;
import com.christopher.objects.HeapEntry;
import com.christopher.objects.LexiconEntry;
import com.christopher.objects.PageTableEntry;
import com.christopher.objects.ResponseObject;
import com.christopher.objects.SnippetEntry;
import com.christopher.objects.TermEntry;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class QueryProcessorController {
    private TreeMap<String, LexiconEntry> lexicon = new TreeMap<>();
    private TreeMap<Integer, PageTableEntry> pageTable = new TreeMap<>();
    // Variables used to keep cache
    HashMap<String, List<ResponseObject>> cache =  new HashMap<>();
    PriorityQueue<CacheEntry> queryFrequencyHeap = new PriorityQueue<>();
    HashMap<String, Integer> queryFrequencyMap = new HashMap<>();
    PriorityQueue<HeapEntry> selectedDocuments = new PriorityQueue<>();
    private double[] arrAveragePage = new double[1];
    @GetMapping("/")
    public ModelAndView index(Model model){
        
        loadLexiconAndPageTable(lexicon, pageTable, arrAveragePage);
        List<ResponseObject> responseList = new ArrayList<>();
        ModelAndView newModelAndView = new ModelAndView();
        newModelAndView.setViewName("queryPage");
        newModelAndView.addObject("responseList", responseList);
        return newModelAndView;
    }

    @GetMapping("/query")
    public ModelAndView processQuery(@RequestParam("query") String query, @RequestParam("type") String type){
        // if disjunctive
        query = query.toLowerCase();
        String queryAndType = query;
        if (type.equals("0")){
            queryAndType += " or";
        }else{
            queryAndType += " and";
        }
        List<ResponseObject> responseList = new ArrayList<>();
        if (cache.containsKey(queryAndType)){
            responseList = new ArrayList<>(cache.get(queryAndType));
        }else{
            selectedDocuments = new PriorityQueue<>();
            if (type.equals("0")){
                // process disjunctive
                processDisjunctiveQuery(query, lexicon, pageTable.size(), selectedDocuments, arrAveragePage[0], pageTable);
            }else{
                // process conjunctive
                processConjunctiveQuery(query, lexicon, pageTable.size(), selectedDocuments, arrAveragePage[0], pageTable);
            }
            
            responseList = new ArrayList<>();
            PageRetriever pageRetriever = new PageRetriever();
            // fetch page from database and generate snippet
            while(selectedDocuments.size() > 0){
                HeapEntry entry = selectedDocuments.poll();
                String text = pageRetriever.getPage(entry.getDocId());
                ResponseObject newResponseObject = new ResponseObject();
                String snippet = generateSnippet(query.split(" "), text);
                newResponseObject.setSnippet(snippet);
                newResponseObject.setTermParametersMap(entry.getTermParametersMap());
                newResponseObject.setScore(entry.getImpaceScore());
                responseList.add(newResponseObject);
            }
            Collections.reverse(responseList);
            // check if to be added to cache
            int prevFreq = 0;
            if (queryFrequencyMap.containsKey(queryAndType)){
                prevFreq = queryFrequencyMap.get(queryAndType);
            }
            queryFrequencyMap.put(queryAndType, prevFreq + 1);
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
            newEntry.setQuery(queryAndType);
            queryFrequencyHeap.add(newEntry);
            
            cache.put(queryAndType, new ArrayList<>(responseList));
        }

        ModelAndView newModelAndView = new ModelAndView();
        newModelAndView.setViewName("queryPage");
        newModelAndView.addObject("responseList", responseList);
        return newModelAndView;
    }
    public void loadLexiconAndPageTable(TreeMap<String, LexiconEntry> lexicon, TreeMap<Integer, PageTableEntry> pageTable, double[] arrAveragePage){
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
    public void processConjunctiveQuery(String query, TreeMap<String, LexiconEntry> lexicon, int docCount, PriorityQueue<HeapEntry> selectedDocuments, double averagePageLength, TreeMap<Integer, PageTableEntry> pageTable){
        String[] terms = query.split(" ");
        HashMap<String, int[]> map =  new HashMap<>();
        PriorityQueue<TermEntry> minHeap = new PriorityQueue<>();   // used to sort terms based on size
        for (String term : terms){
            if (map.containsKey(term)){
                continue;
            }
            int[] resultArray = openList(term, lexicon);
            if (resultArray.length == 1){
                return;
            }
            map.put(term, resultArray);
            TermEntry newTermEntry = new TermEntry();
            newTermEntry.setTerm(term);
            newTermEntry.setSize(resultArray.length);
            minHeap.add(newTermEntry);
        }
        List<String> sortedTerms = new ArrayList<>();
        while(minHeap.size() > 0){
            TermEntry entry = minHeap.poll();
            sortedTerms.add(entry.getTerm());
        }        
        boolean isFinished = false;
        boolean isfirst = true;
        int currentDocId = 0;
        int newDocId = 0;
        int listNumber = 0;
        while (!isFinished){
            List<int[]> scoreParameterList = new ArrayList<>();
            HashMap<String, int[]> termParametersMap = new HashMap<>();
            // for(int[] currentList : invertedListList){
            for(String term : sortedTerms){
                int[] currentList = map.get(term);
                int[] docIdFreq = new int[2];
                // List<Integer> scoreParameter = new ArrayList<>();
                int[] scoreParameter = new int[3];
                if (!nextGEQ(currentList, currentDocId, currentList[0], docIdFreq)){
                    isFinished = true;
                    break;
                }
                // if first list, set currentDocId to the first docId found in list and go to next list.
                if (isfirst){
                    currentDocId = docIdFreq[0];
                    isfirst = false;
                    listNumber++;
                    
                    // length of list
                    scoreParameter[0] = currentList[0];
                    // frequency of term in list
                    scoreParameter[1] = docIdFreq[1];
                    // length of page
                    scoreParameter[2] = pageTable.get(docIdFreq[0]).getLength();
                    scoreParameterList.add(scoreParameter);
                    termParametersMap.put(term, scoreParameter);
                    // check if only 1 term
                    if (listNumber == sortedTerms.size()){
                        double score = 0;
                        for (int[] currentParameter : scoreParameterList){
                            score += computeScore(docCount, currentParameter[0], currentParameter[1], averagePageLength, currentParameter[2]);
                        }
                        HeapEntry newEntry = new HeapEntry();
                        newEntry.setDocId(currentDocId);
                        newEntry.setImpactScore(score);
                        newEntry.setTermParametersMap(termParametersMap);
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
                    continue;
                }
                newDocId = docIdFreq[0];
                if (newDocId > currentDocId){
                    currentDocId = newDocId;
                    listNumber = 0;
                    break;
                }else if (newDocId == currentDocId){
                    scoreParameter[0] = currentList[0];
                    scoreParameter[1] = docIdFreq[1];
                    scoreParameter[2] = pageTable.get(docIdFreq[0]).getLength();
                    scoreParameterList.add(scoreParameter);
                    termParametersMap.put(term, scoreParameter);
                    listNumber++;
                    // if present in all documents, compute BM25 score
                    if (listNumber == sortedTerms.size()){
                        double score = 0;
                        for (int[] currentParameter : scoreParameterList){
                            score += computeScore(docCount, currentParameter[0], currentParameter[1], averagePageLength, currentParameter[2]);
                        }
                        HeapEntry newEntry = new HeapEntry();
                        newEntry.setDocId(currentDocId);
                        newEntry.setImpactScore(score);
                        newEntry.setTermParametersMap(termParametersMap);
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
    public void processDisjunctiveQuery(String query, TreeMap<String, LexiconEntry> lexicon, int docCount, PriorityQueue<HeapEntry> selectedDocuments, double averagePageLength,  TreeMap<Integer, PageTableEntry> pageTable){
        double[] impactScoreArray = new double[pageTable.size()];
        String[] terms = query.split(" ");
        HashMap<String, int[]> map =  new HashMap<>();
        PriorityQueue<TermEntry> minHeap = new PriorityQueue<>();
        for (String term : terms){
            if (map.containsKey(term)){
                continue;
            }
            int[] resultArray = openList(term, lexicon);
            if (resultArray.length == 1){
                continue;
            }
            map.put(term, resultArray);
            TermEntry newTermEntry = new TermEntry();
            newTermEntry.setTerm(term);
            newTermEntry.setSize(resultArray.length);
            minHeap.add(newTermEntry);
        }
        List<String> sortedTerms = new ArrayList<>();
        while(minHeap.size() > 0){
            TermEntry entry = minHeap.poll();
            sortedTerms.add(entry.getTerm());
        }
        for(String term : sortedTerms){
            int[] currentList = map.get(term);
            int[] docIdFreq = new int[2];
            int currentDocId = 0;
            while(nextGEQ(currentList, currentDocId, currentList[0], docIdFreq)){
                double score = computeScore(docCount, currentList[0], docIdFreq[1], averagePageLength, pageTable.get(docIdFreq[0]).getLength());
                impactScoreArray[docIdFreq[0]] += score;
                currentDocId = docIdFreq[0] + 1;
            }
        }
        PriorityQueue<HeapEntry> pQueue = new PriorityQueue<>();
        // iterate through the array to find top 10;
        for(int i = 0; i < impactScoreArray.length; i++){
            double currentScore = impactScoreArray[i];
            if (currentScore == 0.0){
                continue;
            }
            HeapEntry newEntry = new HeapEntry();
            newEntry.setDocId(i);
            newEntry.setImpactScore(currentScore);
            if (pQueue.size() >= 10){
                // compare with lowest one
                HeapEntry lowestEntry = pQueue.peek();
                if (lowestEntry.getImpaceScore() < currentScore){
                    pQueue.poll();
                    pQueue.add(newEntry);
                }
            }else{
                pQueue.add(newEntry);
            }
        }
        while(pQueue.size() > 0){
            HeapEntry currentEntry = pQueue.poll();
            HeapEntry newEntry = new HeapEntry();
            newEntry.setDocId(currentEntry.getDocId());
            newEntry.setImpactScore(currentEntry.getImpaceScore());
            HashMap<String, int[]> termParametersMap = new HashMap<>();

            for(String term : sortedTerms){
                int[] currentList = map.get(term);
                int[] docIdFreq = new int[2];
                int[] scoreParameter = new int[3];
                nextGEQ(currentList, currentEntry.getDocId(), currentList[0], docIdFreq);

                if (currentEntry.getDocId() != docIdFreq[0]){
                    scoreParameter[0] = currentList[0];
                    scoreParameter[1] = 0;
                }else{
                    scoreParameter[0] = currentList[0];
                    scoreParameter[1] = docIdFreq[1];
                }
                scoreParameter[2] = pageTable.get(currentEntry.getDocId()).getLength();
                termParametersMap.put(term, scoreParameter);
            }
            newEntry.setTermParametersMap(termParametersMap);
            selectedDocuments.add(newEntry);
        }
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
    private String generateSnippet(String[] query, String text){
        PriorityQueue<SnippetEntry> minHeapSnippet = new PriorityQueue<>();
        HashMap<String, Integer> queryValueMap = new HashMap<>();
        for (String currentQuery : query){
            queryValueMap.put(currentQuery.toLowerCase(), 5);
        }

        String[] sentences = text.replaceAll("<TEXT>", "").split("\\.|\\!|\\?|\\n|\\r\\r?");
        for(int i = 0; i < sentences.length; i++){
            int score = 0;
            // find term in sentence
            boolean isFound = false;
            String termFound = "";
            for (int j = 0; j < query.length; j++){
                int index = sentences[i].toLowerCase().indexOf(query[j].toLowerCase());
                if ( index != -1){
                    isFound = true;
                    termFound = query[j].toLowerCase();
                    break;
                }
            }
            if (isFound){
                // get all words in the sentence
                String[] split = sentences[i].toLowerCase().split("[\\W]");
                for (int j = 0; j < split.length; j++){
                    // count the number of words in between
                    // the more in the middle, the more points
                    if (split[j].toLowerCase().equals(termFound)){
                        int termScore = queryValueMap.get(termFound);
                        score += termScore;
                        // compare the number of words before and after the term and select the minimum
                        score += Math.min( j - 0, split.length - j);
                        if (termScore == 5){
                            queryValueMap.put(termFound, 2);
                        }else if (termScore == 2){
                            queryValueMap.put(termFound, 1);
                        }
                        // check if there are any query terms left
                        for (int k = j + 1; k < split.length; k++){
                            if (queryValueMap.containsKey(split[k].toLowerCase())){
                                termScore = queryValueMap.get(split[k].toLowerCase());
                                score += termScore;
                                if (termScore == 5){
                                    queryValueMap.put(split[k].toLowerCase(), 2);
                                }else if (termScore == 2){
                                    queryValueMap.put(split[k].toLowerCase(), 1);
                                }
                            }
                        }
                        SnippetEntry snippetEntry = new SnippetEntry();
                        snippetEntry.setScore(score);
                        snippetEntry.setSnippet(sentences[i]);
                        snippetEntry.setLineNumber(i);
                        if (minHeapSnippet.size() >= query.length){
                            SnippetEntry curEntry = minHeapSnippet.peek();
                            if (curEntry.getScore() < score){
                                minHeapSnippet.poll();
                                minHeapSnippet.add(snippetEntry);
                            }
                        }else{
                            minHeapSnippet.add(snippetEntry);
                        }
                        break;
                    }
                    
                }
            }

            sentences[i] = "";
        }
        TreeMap<Integer, String> sortedMap = new TreeMap<>();
        while(minHeapSnippet.size() > 0){
            SnippetEntry entry = minHeapSnippet.poll();
            sortedMap.put(entry.getLineNumber(), entry.getSnippet());
        }
        String answer = "";
        for(String snippet : sortedMap.values()){
            answer += snippet + "...";
        }
        return answer;
    }
    /**
     * Open inverted list for a term
     * @param term
     */
    public int[] openList(String term, TreeMap<String, LexiconEntry> lexicon){
        // if inverted list has less than 64, there will be no max for that chunk
        LexiconEntry lexiconEntry = lexicon.get(term.toLowerCase());
        if (lexiconEntry == null) {
            return new int[]{0};
        }
        File file = new File("InvertedIndex.bin");
        int numberOfList = lexiconEntry.getLength() / 64;
        int lengthInvertedList = 1 + numberOfList + (2 * lexiconEntry.getLength());
        int[] resultArray = new int[lengthInvertedList];
        try(FileInputStream fin = new FileInputStream(file);){
            fin.skip(lexiconEntry.getStartBlock() + lexiconEntry.getBlockNumber());
            
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
    /**
     * Method to find next posting in the list with docID > k and return the docId
     * @param list
     * @param k
     */
    public boolean nextGEQ(int[] resultArray, int k, int lengthOfList, int[] docIdFreq){
        int numberOfList = lengthOfList / 64;
        int remainder = lengthOfList % 64;
            // iterate through maximums. if there is one greater than k, search through that list. else search in remainder.
        for (int i = 1; i < numberOfList + 1; i++){
            // if maximum greater than k, go to that list
            if (resultArray[i] >= k){
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
    public int varDecode(int i, int offset, FileInputStream fin, int[] resultArray){
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
