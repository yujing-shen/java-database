package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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

    // Task5 Java Data Structures
    @Test
    public void testTableSaveToFile() throws IOException {
        String testFolderPath = "databases";
        String testTableName = "test_persistence";
        Table testTable = new Table(testTableName);
        testTable.addColumnName("id");
        testTable.addColumnName("Name");
        testTable.addColumnName("Age");

        Row r1 = new Row();
        r1.addValue("1");
        r1.addValue("Alice");
        r1.addValue("20");
        testTable.addRow(r1);

        testTable.saveToFIle(testFolderPath);
        File savedFile = new File(testFolderPath + File.separator + testTableName + ".tab");
        assertTrue(savedFile.exists(), "The file should exist");

        BufferedReader reader = new BufferedReader(new FileReader(savedFile));
        String header = reader.readLine();
        String firstDataLine = reader.readLine();
        reader.close();
        assertEquals("id\tName\tAge", header, "The header format is incorrect. It should be a tab-separated string.");
        assertEquals("1\tAlice\t20", firstDataLine, "The first data line is incorrect.");
        savedFile.delete();


    }


}
