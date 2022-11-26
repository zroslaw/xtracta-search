package nz.xtracts.search;

import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Throwable{

        // read suppliers file
        List<List<String>> suppliernamesFile = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("suppliernames.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                suppliernamesFile.add(Arrays.asList(values));
            }
        }
        suppliernamesFile.remove(0); // remove headers line

        // read invoice.txt
        Map<Integer, Map<Integer, TreeMap<Integer, String>>> invoiceWords = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("invoice.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                JSONObject json = new JSONObject(line);
                int page_id = (Integer)json.get("page_id");
                int line_id = (Integer)json.get("line_id");
                int pos_id = (Integer)json.get("pos_id");
                String word = (String)json.get("word");
                Map<Integer, TreeMap<Integer, String>> pageLines = invoiceWords.get(page_id);
                if (pageLines==null) {pageLines = new HashMap<>();invoiceWords.put(page_id, pageLines);}
                TreeMap<Integer, String> lineWords = pageLines.get(line_id);
                if (lineWords==null) {lineWords = new TreeMap<>();pageLines.put(line_id, lineWords);}
                lineWords.put(pos_id, word);
            }
        }
//        System.out.println(suppliernamesFile);
//        System.out.println(invoiceWords);

        // create search index
        Map<String, List<WordData>> index = new TreeMap<>();
        for (Integer page_id:invoiceWords.keySet()){;
            Map<Integer, TreeMap<Integer, String>> pageLines = invoiceWords.get(page_id);
            for (Integer line_id:pageLines.keySet()){
                TreeMap<Integer, String> lineWords = pageLines.get(line_id);
                List<String> nextWords = new LinkedList<>();
                for (Integer pos_id:lineWords.descendingKeySet()) {
                    String word = lineWords.get(pos_id);
                    WordData wordData = new WordData();
                    wordData.word = word;
                    wordData.page_id = page_id;
                    wordData.line_id = line_id;
                    wordData.pos_id = pos_id;
                    wordData.nextWords = new LinkedList<>(nextWords);
                    nextWords.add(0, word);
                    // add wordData to the index
                    List<WordData> wordEncounters = index.get(word);
                    if (wordEncounters==null) {wordEncounters = new LinkedList<>();index.put(word, wordEncounters);}
                    wordEncounters.add(wordData);
                }
            }
        }
//        System.out.println(index);

        // search through the suppliers list
        for (List<String> supplierString:suppliernamesFile){
            String id = supplierString.get(0);
            String[] nameWords = supplierString.get(1).split(" ");
            String firstWord = nameWords[0];
            List<WordData> wordEncounters = index.get(firstWord);
            if (wordEncounters==null) continue;
            for (WordData wordData:wordEncounters){
                boolean supplierNameFound = true;
                if (wordData.nextWords.size()<nameWords.length-1) continue;
                for (int i=1;i<nameWords.length;i++){
                    if (wordData.nextWords.get(i-1).equals(nameWords[i])) continue;
                    supplierNameFound = false;
                    break;
                }
                if (supplierNameFound){
                    System.out.println("Supplier "+supplierString+" found at position [page_id="+wordData.page_id+", line_id="+wordData.line_id+", pos_id="+wordData.pos_id+"]");
                }
            }
        }

    }

}

class WordData {
    int page_id, line_id, pos_id;
    String word;
    List<String> nextWords;

    public String toString(){
        return word+"=("+nextWords+")";
    }

}

