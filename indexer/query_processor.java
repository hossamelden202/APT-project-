import indexer.Stemmer;

public Map<String, Double> processQuery(String query) {
    Map<String, Double> results = new HashMap<>();
    Map<String, Double> phraseResults = new HashMap<>();
    Stemmer stemmer = new Stemmer(); // Create an instance of Stemmer

    // Preprocess query
    query = query.toLowerCase();

    // Check if the query contains quotation marks
    if (query.startsWith("\"") && query.endsWith("\"")) {
        // Extract the phrase
        String phrase = query.substring(1, query.length() - 1);

        // Perform phrase search
        phraseResults = performPhraseSearch(phrase);

        // Add phrase results to the final results
        results.putAll(phraseResults);
    }

    // Perform normal word-based search
    String[] words = query.replaceAll("\"", "").split("\\s+");
    for (String word : words) {
        // Add characters to the Stemmer
        stemmer.add(word.toCharArray(), word.length());
        stemmer.stem(); // Perform stemming
        word = stemmer.toString(); // Get the stemmed word

        if (index.index.containsKey(word)) {
            for (Posting posting : index.index.get(word)) {
                results.put(posting.documentId, results.getOrDefault(posting.documentId, 0.0) + 1.0);
            }
        }
    }

    // Ensure phrase results are a subset of normal results
    if (!phraseResults.isEmpty()) {
        results.keySet().retainAll(phraseResults.keySet());
    }

    return results;
}

private Map<String, Double> performPhraseSearch(String phrase) {
    Map<String, Double> phraseResults = new HashMap<>();
    String[] words = phrase.split("\\s+");

    for (Map.Entry<String, List<Posting>> entry : index.index.entrySet()) {
        String word = entry.getKey();
        List<Posting> postings = entry.getValue();

        if (word.equals(words[0])) { // Check if the first word matches
            for (Posting posting : postings) {
                List<String> positions = posting.positions;

                // Check if the phrase exists in the same order
                for (String position : positions) {
                    int startPos = Integer.parseInt(position);
                    boolean match = true;

                    for (int j = 1; j < words.length; j++) {
                        int nextPos = startPos + j;
                        if (!index.index.containsKey(words[j]) ||
                                index.index.get(words[j]).stream()
                                        .noneMatch(p -> p.documentId.equals(posting.documentId) &&
                                                p.positions.contains(String.valueOf(nextPos)))) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        phraseResults.put(posting.documentId, phraseResults.getOrDefault(posting.documentId, 0.0) + 1.0);
                    }
                }
            }
        }
    }

    return phraseResults;
}