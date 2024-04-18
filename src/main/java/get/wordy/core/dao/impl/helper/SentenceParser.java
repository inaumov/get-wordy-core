package get.wordy.core.dao.impl.helper;

import get.wordy.core.api.bean.Sentence;

public class SentenceParser {

    public Sentence parseSentence(String rowData) {

        String[] columns = rowData.split("\\s*;\\s*"); // Split by semicolon with optional whitespace

        String fullSentence = null;
        String matchedWords = null;

        for (String column : columns) {
            String[] keyValue = column.split(":");
            if (keyValue.length == 2) {
                String propertyName = keyValue[0].trim();
                String propertyValue = keyValue[1].trim();
                if ("example".equals(propertyName)) {
                    fullSentence = propertyValue;
                } else if ("matchedWords".equals(propertyName)) {
                    matchedWords = propertyValue;
                }
            }
        }

        return Sentence.of(fullSentence).withMatchedWords(matchedWords);
    }

}
