package indexer;

import java.io.*;
import java.util.*;
import java.nio.file.Files;

public class Utils {
   public static Set<String> loadStopWords(String path) throws IOException {
    List<String> lines = Files.readAllLines(new File(path).toPath());
    Set<String> words = new HashSet<>();
    for (String line : lines) {
        words.addAll(Arrays.asList(line.split("\\s+")));
    }
    return words;
}
//convet files to lines to words as set

    public static List<String> tokenize(String text) {
        return Arrays.asList(text.toLowerCase().split("[^a-zA-Z0-9]+"));
    }

   public static String detectPosition(String word, String title, String body) {
        word = word.toLowerCase();

        if (title.toLowerCase().contains(word)) {
            return "head";
        }

        String[] lines = body.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String[] words = lines[i].split("\\s+");
            for (int j = 0; j < words.length; j++) {
                if (words[j].replaceAll("\\W", "").equalsIgnoreCase(word)) {
                    return "line:" + (i + 1) + ",word:" + (j + 1);
                }
            }
        }

        return "body";
    }
}
