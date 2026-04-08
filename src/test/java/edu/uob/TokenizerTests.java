package edu.uob;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TokenizerTests {
    @Test
    public void testBasicQuery() {
        Tokenizer tokenizer = new Tokenizer();
        String query = "SELECT * FROM people;";
        List<String> tokens = tokenizer.parseTokens(query);

        List<String> expectedTokens = Arrays.asList("SELECT", "*", "FROM", "people",";");
        assertEquals(expectedTokens, tokens, "Failed Query");
    }

    // Multiple spaces
    @Test
    public void testExtraSpaces() {
        Tokenizer tokenizer = new Tokenizer();
        String query = "         SELECT   *   FROM       people  ;   ";
        List<String> tokens = tokenizer.parseTokens(query);
        List<String> expectedTokens = Arrays.asList("SELECT", "*", "FROM", "people",";");
        assertEquals(expectedTokens, tokens, "Extra spaces should be ignored");
    }

    @Test
    public void testComplexQueryQuotesAndSymbols() {
        Tokenizer tokenizer = new Tokenizer();
        String query = "INSERT INTO marks (name, mark) VALUES ('Steve Jobs', 65);";
        List<String> tokens = tokenizer.parseTokens(query);

        List<String> expected = Arrays.asList(
                "INSERT", "INTO", "marks", "(", "name", ",", "mark", ")",
                "VALUES", "(", "'Steve Jobs'", ",", "65", ")", ";"
        );

        assertEquals(15, tokens.size(), "Wrong number of tokens");
        assertTrue(tokens.contains("'Steve Jobs'"), "The string containing spaces was split incorrectly.");
    }
}
