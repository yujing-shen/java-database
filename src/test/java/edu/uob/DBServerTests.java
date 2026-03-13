package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class DBServerTests {
    private DBServer server;

    // Task3 Persistent Storage
    @BeforeEach
    public void setup() {
        server = new DBServer();
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

        int firstId = testTable.getNextId();
        assertEquals(1,firstId, "First Id should be 1");
        assertEquals(2,testTable.getNextId(), "First Id should be 2");
        assertEquals(3,testTable.getNextId(), "First Id should be 3");

        testTable.updateNextAvailableId(8);

        int idAfterLoad = testTable.getNextId();
        assertEquals(9,idAfterLoad, "The next Id should be 9");

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

    @Test
    public void testCreateAndUseDatabase() {
        DBServer server = new DBServer();
        String testDbName = "test_cmd_db";

        String response1 = server.handleCommand("CREATE DATABASE " + testDbName + ";");
        assertTrue(response1.startsWith("[OK]"), "CREATE Ddatabase successfully should return [OK]");

        String response2 = server.handleCommand("USE " + testDbName + ";");
        assertTrue(response2.startsWith("[OK]"), "SWITCHED to existing database should return [OK]");

        String response3 = server.handleCommand("USE a_fake_database;");
        assertTrue(response3.startsWith("[ERROR]"), "SWITCHED to unexisting database should return [ERROR]");

        File dbFolder = new File("databases" +  File.separator + testDbName);
        if (!dbFolder.exists()) {
            dbFolder.delete();
        }
    }

    @Test
    public void testCreateTable() {
        DBServer server = new DBServer();
        String dbName = "test_create_table";
        String tableName = "test_students";

        server.handleCommand("CREATE DATABASE " + dbName + ";");
        server.handleCommand("USE " + dbName + ";");

        String response1 = server.handleCommand("CREATE TABLE " + tableName + " (name, age, email);");
        assertTrue(response1.startsWith("[OK]"), "Creating a table should return [OK]");

        File tableFile = new File("databases" + File.separator + dbName + File.separator + tableName + ".tab");
        assertTrue(tableFile.exists(), "The .tab file must be physically created on the disk");

        String response2 = server.handleCommand("CREATE TABLE " + tableName + " (score);");
        assertTrue(response2.startsWith("[ERROR]"), "Creating an already existing table must return [ERROR]");

        tableFile.delete();
        File dbFolder = new File("databases" + File.separator + dbName);
        dbFolder.delete();

    }

    // Task 7: Test INSERT INTO
    @Test
    public void testInsertInto() {
        DBServer server = new DBServer();
        String dbName = "test_insert_db";
        String tableName = "test_students";

        server.handleCommand("CREATE DATABASE " + dbName + ";");
        server.handleCommand("USE " + dbName + ";");
        server.handleCommand("CREATE TABLE " + tableName + " (name, age, email);");

        String insertQuery = "INSERT INTO " + tableName + " VALUES ('Bob', 21, 'bob@bristol.ac.uk');";
        String response1 = server.handleCommand(insertQuery);
        assertTrue(response1.startsWith("[OK]"), "Inserting into table should return [OK]");

        String wrongQuery = "INSERT INTO " + tableName + " ('Alice', 22, 'alice@bristol.ac.uk');";
        String response2 = server.handleCommand(wrongQuery);
        assertTrue(response2.startsWith("[ERROR]"), "Inserting into table should return [ERROR]");

        File tableFile = new File("databases" + File.separator + dbName + File.separator + tableName + ".tab");
        tableFile.delete();
        File dbFolder = new File("databases" + File.separator + dbName);
        dbFolder.delete();

    }

    // TASK 7: Test SELECT (without WHERE)
    @Test 
    public void testBasicSelect() {
        DBServer server = new DBServer();
        String dbName = "test_select_db";
        String tableName = "test_students";

        // 1. Setup: Create DB, create table, and insert TWO records
        server.handleCommand("CREATE DATAbASE " + dbName + ";");
        server.handleCommand("USE " + dbName + ";");
        server.handleCommand("CREATE TABLE " + tableName + " (name, age);");
        server.handleCommand("INSERT INTO " + tableName + " VALUES ('Alice', 20)");
        server.handleCommand("INSERT INTO " + tableName + " VALUES ('Bob', 22)");

        // 2. Test SELECT * (Should return all columns and all data)
        String response1 = server.handleCommand("SELECT * FROM " + tableName + ";");
        assertTrue(response1.startsWith("[OK]"), "Select * should return [OK]");
        assertTrue(response1.contains("Alice") && response1.contains("20"), "Result should return Alice's data." );
        assertTrue(response1.contains("Bob") && response1.contains("22"), "Result should return Alice's data." );
        assertTrue(response1.contains("id") && response1.contains("name") && response1.contains("age"), "Result should contain all headers." );

        // 3. Test SELECT specific columns (e.g., only 'name')
        String response2 = server.handleCommand("SELECT name FROM " + tableName + ";");
        assertTrue(response2.startsWith("[OK]"), "Select specific column should return [OK]");
        assertTrue(response2.contains("Alice") && response2.contains("Bob"), "Result should contain all names." );
        // System.out.println(response2);
        assertFalse(response2.contains("20") || response2.contains("22"), "Result should NOT contain ages because we only selected 'name." );

        // 4. Test Error: Select from a non-existent table
        String response3 = server.handleCommand("SELECT * FROM fake_table" + ";");
        assertTrue(response3.startsWith("[ERROR]"), "Selecting from a fake table should return [ERROR]");

        // 5. Teardown: Clean up test files
        File tableFile = new File("databases" + File.separator + dbName + File.separator + tableName + ".tab");
        tableFile.delete();
        File dbFolder = new File("databases" + File.separator + dbName);
        dbFolder.delete();
    }

    // Task 7: Test DROP TABLE and DROP DATABASE
    @Test
    public void testDropCommand() {
        DBServer server = new DBServer();
        String dbName = "test_drop_db";
        String tableName = "test_drop_table";

        server.handleCommand("CREATE DATABASE " + dbName + ";");
        server.handleCommand("USE " + dbName + ";");
        server.handleCommand("CREATE TABLE " + tableName + " (name, age);");

        String response1 = server.handleCommand("DROP TABLE " + tableName + ";");
        assertTrue(response1.startsWith("[OK]"), "DROP TABLE should return [OK]");

        // Check physical file deletion
        File tableFile = new File("databases" + File.separator + dbName + File.separator + tableName + ".tab");
        assertFalse(tableFile.exists(), "The .tab file must be physically deleted from the disk");

        String response2 = server.handleCommand("DROP DATABASE " + dbName + ";");
        System.out.println(response2);
        assertTrue(response2.startsWith("[OK]"), "DROP DATABASE should return [OK]");
        File dbFolder = new File("databases" + File.separator + dbName);
        assertFalse(dbFolder.exists(), "The database folder must be physically deleted from the disk");

        // Test "Amnesia" (Checking if currentDatabase was cleared)
        String response3 = server.handleCommand("CREATE TABLE new_table (id);");
        assertTrue(response3.startsWith("[ERROR]"), "Creating a new table right after dropping the database must return [ERROR]");

        // Teardown
        if (tableFile.exists()) {
            tableFile.delete();
        }
        if (dbFolder.exists()) {
            File[] files = dbFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dbFolder.delete();
        }
    }
    @Test
    public void testAlterTable() {
        DBServer server = new DBServer();
        String dbName = "test_alter_db";
        String tableName = "test_alter_table";
        server.handleCommand("CREATE DATABASE " + dbName + ";");
        server.handleCommand("USE " + dbName + ";");
        server.handleCommand("CREATE TABLE " + tableName + " (name, age);");
        server.handleCommand("INSERT INTO " + tableName + " VALUES ('Alice', 20);");

        String addResponse = server.handleCommand("ALTER DATABASE " + tableName + " ADD email;");
        assertTrue(addResponse.startsWith("[OK]"), "Add column should return [OK]");

        String selectAfterAdd = server.handleCommand("SELECT * FROM " + tableName + ";");
        assertTrue(selectAfterAdd.contains("email"), "The new column 'email' should appear in the header");

        String dropResponse = server.handleCommand("ALTER TABLE " + tableName + " DROP age;");
        assertTrue(dropResponse.startsWith("[OK]"), "DROP column should return [OK]");
        String selectAfterDrop = server.handleCommand("SELECT * FROM " + tableName + ";");
        assertFalse(selectAfterDrop.contains("age"), "The new column 'age' should be gone");
        assertFalse(selectAfterDrop.contains("20"), "Alice's age data (20) should also be deleted");

        String dropIdResponse = server.handleCommand("ALTER TABLE " + tableName + " DROP id;");
        assertTrue(dropIdResponse.startsWith("[ERROR]"), "Dropping 'id' should return [ERROR]");

        String dropFakeResponse = server.handleCommand("ALTER TABLE " + tableName + " DROP fake_column;");
        assertTrue(dropFakeResponse.startsWith("[ERROR]"), "Adding an existing column must return [ERROR]");

        File tableFile = new File("databases" + File.separator + dbName + File.separator + tableName + ".tab");
        if (tableFile.exists()) {
            tableFile.delete();
        }
        File dbFolder = new File("databases" + File.separator + dbName);
        if (dbFolder.exists()) {
            File[] files = dbFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dbFolder.delete();
        }

    }

    @Test
    public void testDeleteTable() {
        DBServer server = new DBServer();
        String dbName = "test_delete_db";
        String tableName = "test_delete_table";

        server.handleCommand("CREATE DATABASE " + dbName + ";");
        server.handleCommand("USE " + dbName + ";");
        server.handleCommand("CREATE TABLE " + tableName + " (name, age);");
        server.handleCommand("INSERT INTO " + tableName + " VALUES ('Alice', 20);");
        server.handleCommand("INSERT INTO " + tableName + " VALUES ('Bob', 25);");
        server.handleCommand("INSERT INTO " + tableName + " VALUES ('Charlie', 20);");

        String deleteResponse = server.handleCommand("DELETE FROM " + tableName + " WHERE name == 'Bob';");
        assertTrue(deleteResponse.startsWith("[OK]"), "DELETE command should return [OK]");

        String selectAfterDelete = server.handleCommand("SELECT * FROM " + tableName + ";");
        assertFalse(selectAfterDelete.contains("Bob"), "Bob should be deleted");
        assertTrue(selectAfterDelete.contains("Alice"), "Alice should still exist");
        assertTrue(selectAfterDelete.contains("Charlie"), "Charlie should still exist");

        String noWhereResponse = server.handleCommand("DELETE FROM " + tableName + ";");
        assertTrue(noWhereResponse.startsWith("[ERROR]"), "DELETE without WHERE must return [ERROR]");

        String wrongColResponse = server.handleCommand("DELETE FROM " + tableName + " WHERE height == '180';");
        assertTrue(wrongColResponse.startsWith("[ERROR]"), "DELETE with non-existent column must return [ERROR]");

        File tableFile = new File("databases" + File.separator + dbName + File.separator + tableName + ".tab");
        if (tableFile.exists()) {
            tableFile.delete();
        }
        File dbFolder = new File("databases" + File.separator + dbName);
        if (dbFolder.exists()) {
            File[] files = dbFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dbFolder.delete();
        }

    }


}

