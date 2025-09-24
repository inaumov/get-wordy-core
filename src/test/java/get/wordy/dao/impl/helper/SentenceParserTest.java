package get.wordy.dao.impl.helper;

import get.wordy.core.api.bean.Sentence;
import get.wordy.core.dao.impl.helper.SentenceParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SentenceParserTest {

    private final SentenceParser parser = new SentenceParser();

    @ParameterizedTest
    @CsvSource({
            "example:This range has not been tested on animals.;matchedWords:tested",
            "example:Vocabulary size is generally measured by extrapolating a test score.;matchedWords:test"
    })
    public void testParseSentence(String rowData) {
        Sentence parsedSentence = parser.parseSentence(rowData);

        assertNotNull(parsedSentence);

        String expectedFullSentence = getFullSentenceFromRowData(rowData);
        String expectedMatchedWords = getMatchedWordsFromRowData(rowData);

        assertEquals(expectedFullSentence, parsedSentence.example());
        assertEquals(expectedMatchedWords, parsedSentence.matchedWords());
    }

    private String getFullSentenceFromRowData(String rowData) {
        return rowData.split(";")[0].split(":")[1].trim();
    }

    private String getMatchedWordsFromRowData(String rowData) {
        return rowData.split(";")[1].split(":")[1].trim();
    }

}
