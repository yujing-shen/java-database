package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MyTests {
    private DBServer server;

    // Task3 Persistent Storage
    @BeforeEach
    public void setup() {
        server = new DBServer();
    }

    @Test
    public void testReadingPeopleFile() {
        String response = server.handleCommand("hello");
        assertTrue(response.contains("Bob"), "Response should contain content from the file");
        assertTrue(response.contains("id"), "Response should contain table headers");
    }
    @Test
    public void testEmptyCommand() {
        String response = server.handleCommand("");
        assertTrue(response.length() > 0, "Server should return something even with empty command");
    }

    // Task4 Maintaining Relationships
    @Test
    public void testTableGeneration() {
        Table testTable = new Table("test_people");

        int firstId = testTable.getNextNextId();
        assertEquals(1,firstId, "First id should be 1");
        assertEquals(2,testTable.getNextNextId(), "First id should be 2");
        assertEquals(3,testTable.getNextNextId(), "First id should be 3");

        testTable.updateNextAvailableId(8);

        int idAfterLoad = testTable.getNextNextId();
        assertEquals(9,idAfterLoad, "The next ID should be 9");

    }
}
