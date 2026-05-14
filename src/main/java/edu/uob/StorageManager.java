package edu.uob;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Handles all file system operations (I/O) for the database engine.
 */
public class StorageManager {
    private final String storageFolderPath;

    public StorageManager(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
    }

    public Table loadTable(String currentDatabase, String tableName) {
        // Guard clause: Ensure context is valid before executing file I/O
        if (currentDatabase == null || currentDatabase.isEmpty()) {
            throw new RuntimeException("[ERROR] No database selected. Cannot load table.");
        }

        String tablePath = this.storageFolderPath + File.separator + currentDatabase + File.separator + tableName + ".tab";
        File file = new File(tablePath);

        if (!file.exists()) {
            throw new RuntimeException("[ERROR] Table " + tableName + " does not exist.");
        }

        // OOP Best Practice: try-with-resources ensures the Scanner is automatically closed
        // after the try block executes completely, avoiding the infamous "Scanner closed" bug.
        try (Scanner scanner = new Scanner(file)) {
            Table loadedTable = new Table(tableName);

            // Phase 1: Parse the Header Row
            if (scanner.hasNextLine()) {
                String headerLine = scanner.nextLine();
                // Use a limit of -1 to preserve trailing empty cells if any exist
                String[] headers = headerLine.split("\t", -1);
                loadedTable.setColumnNames(new ArrayList<>(Arrays.asList(headers)));
            }

            int maxIdFound = 0;

            // Phase 2: Parse Data Rows
            while (scanner.hasNextLine()) {
                String dataLine = scanner.nextLine();

                // Skip completely empty lines to prevent blank ghost rows
                if (dataLine.trim().isEmpty()) continue;

                String[] values = dataLine.split("\t", -1);
                Row newRow = new Row();
                for (String val : values) {
                    newRow.addValue(val);
                }
                loadedTable.addRow(newRow);

                // Phase 3: ID Recalibration Tracking
                // Dynamically track the highest ID currently present in the database.
                // This prevents primary key collisions during future INSERT operations.
                if (values.length > 0) {
                    try {
                        int currentId = Integer.parseInt(values[0]);
                        if (currentId > maxIdFound) {
                            maxIdFound = currentId;
                        }
                    } catch (NumberFormatException e) {
                        // Safely ignore if the first column happens to be non-numeric
                    }
                }
            } // End of file scanning loop

            // CRITICAL: Calibrate the ID generator ONLY ONCE after the entire file is parsed.
            loadedTable.updateNextAvailableId(maxIdFound);

            return loadedTable;

        } catch (IOException e) {
            throw new RuntimeException("[ERROR] Failed to read table file: " + e.getMessage());
        }
    }

    public void saveTable(String currentDatabase,  Table tableToSave) {
        if (currentDatabase == null || currentDatabase.isEmpty()) {
            throw new RuntimeException("[ERROR] No database selected. Cannot save table.");
        }

        try {
            // Construct the database directory path
            String dbFolderPath = this.storageFolderPath + File.separator + currentDatabase;
            // Delegate the actual file writing to the Table object
            tableToSave.saveToFile(dbFolderPath);
        } catch (IOException e) {
            throw new RuntimeException("[ERROR] Failed to save table to disk: " + e.getMessage());
        }
    }
}
