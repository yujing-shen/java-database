package edu.uob;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The core engine of the database.
 * Handle all SQL command executions and maintains the current database state.
 */
public class DatabaseEngine {
    private final String storageFolderPath;
    private String currentDatabase;
    private StorageManager storageManager;
    private ConditionEvaluator evaluator;

    public StorageManager getStorageManager() {
        return this.storageManager;
    }

    public ConditionEvaluator getEvaluator() {
        return this.evaluator;
    }

    public String getCurrentDatabase() {
        return this.currentDatabase;
    }

    public void setCurrentDatabase(String dbName) {
        this.currentDatabase = dbName;
    }

    public DatabaseEngine(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
        this.currentDatabase = "";
        this.storageManager = new StorageManager(storageFolderPath);
        this.evaluator = new ConditionEvaluator();
    }

    public String executeCommand(List<String> tokens) {
        try {
            String firstWord = tokens.get(0).toUpperCase();

            switch (firstWord) {
                case "USE":
                    return handleUse(tokens);
                case "CREATE":
                    return handleCreate(tokens);
                case "INSERT":
                    return handleInsert(tokens);
                case "SELECT":
                    return handleSelect(tokens);
                case "DROP":
                    return handleDrop(tokens);
                case "ALTER":
                    return handleAlter(tokens);
                case "DELETE":
                    return handleDelete(tokens);
                case "UPDATE":
                    return handleUpdate(tokens);
                case "JOIN":
                    return handleJoin(tokens);
                default:
                    return "[ERROR] Unknown command: " + firstWord;
            }
        } catch (Exception e) {
            return "[ERROR] " + e.getMessage();
        }
    }

    /**
     * Switches the engine's active databse to an existing folder.
     * Does not read or write any table files; only updates {@code currentDatabase} in memory.
     * 
     * @param tokens token list from Tokenizer, e.g. ["USE", "university", ";"]
     * @return "[OK]" if the database directory exists, otherwise an [ERROR] message
     */
    private String handleUse(List<String> tokens) {
        if (tokens.size() < 2) {
            return "[ERROR] Invalid USE command";
        }

        String dbName = tokens.get(1);
        // Path to databases/<dbName>/ - File object only describes the path until exists is called
        File dbFolder = new File(storageFolderPath + File.separator + dbName);

        if (dbFolder.exists() && dbFolder.isDirectory()) {
            // Remember which database subsequent CREATE TABLE / INSERT / SELECT apply to
            this.currentDatabase = dbName;
            return "[OK]";
        } else {
            return "[ERROR] Database " + dbName + " does not exist or is not a directory";
        }
    }

    /**
     * Handles CREATE DATABASE and CREATE TABLE.
     *
     * <p>For CREATE DATABASE, this method creates a directory under the storage root.
     * For CREATE TABLE, it validates context and column definitions, prepends the
     * mandatory {@code id} column, then persists an empty table schema as a .tab file.
     *
     * @param tokens tokenized command (for example: CREATE TABLE students (name, age);)
     * @return {@code [OK]} on success, or {@code [ERROR] ...} when validation/execution fails
     */
    private String handleCreate(List<String> tokens) {
        if (tokens.size() < 3) return "[ERROR] Invalid CREATE syntax.";
        String createType = tokens.get(1).toUpperCase();
        String targetName = tokens.get(2).replace(";","");

        if (!isValidName(targetName)) return "[ERROR] Invalid or reserved name.";

        try {
            if (createType.equals("DATABASE")) {
                // CREATE DATABASE <name>: create a folder databases/<name>
                File dbFolder = new File(this.storageFolderPath + File.separator + targetName);
                if (dbFolder.exists()) return "[ERROR] Database " + targetName + " already exists.";

                dbFolder.mkdirs();
                return "[OK]\n";

            } else if (createType.equals("TABLE")) {
                if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
                    return "[ERROR] No database selected. Please USE a database first.";
                }

                // CREATE TABLE requires a selected DB and a non-existing target file
                File tableFile = new File(this.storageFolderPath + File.separator + this.currentDatabase + File.separator + targetName + ".tab");
                if (tableFile.exists()) return "[ERROR] Table " + targetName + " already exists.";

                Table newTable = new Table(targetName);

                // Internal schema rule: every table begins with an auto-managed id column.
                newTable.addColumn("id");

                // Parse user-defined columns inside (...) and validate alternating pattern:
                // columnName, comma, columnName, comma, ...
                int openBracket = tokens.indexOf("(");
                int closeBracket = tokens.indexOf(")");

                if (openBracket != -1 && closeBracket == -1) return "[ERROR] Missing closing bracket.";
                if (openBracket != -1 && closeBracket != -1 && openBracket < closeBracket) {
                    // false => expecting a column name, true => expecting a comma
                    boolean expectingComma = false;

                    for (int i = openBracket + 1; i < closeBracket; i++) {
                        String colName = tokens.get(i);
                        if (expectingComma) {
                            // After a valid column, the next token must be a comma.
                            if (!colName.equals(",")) return "[ERROR] Missing comma between columns";
                            expectingComma = false;
                        } else {
                            // Validate column token before adding into schema.
                            if (colName.equalsIgnoreCase("id")) {
                                return "[ERROR] Cannot explicitly create 'id' column";
                            }
                            if (!isValidName(colName)) return "[ERROR] Invalid column name: " + colName;
                            if (newTable.getColumnNames().contains(colName)) return "[ERROR] Duplicate column name: " + colName;

                            newTable.addColumn(colName);
                            expectingComma = true;
                        }
                    }
                }
                // Persist empty schema now; rows will be appended by INSERT later.
                storageManager.saveTable( this.currentDatabase,newTable);
                return "[OK]\n";
            }

            return "[ERROR] Unknown CREATE target. Expected DATABASE or TABLE.";

        } catch (Exception e) {
            return "[ERROR] Failed to execute CREATE: " + e.getMessage();
        }
    }

    /**
     * Handles INSERT INTO ... VALUES (...).
     *
     * <p>Workflow:
     * validate command/context, load target table, extract values from parentheses,
     * verify value count against schema (excluding auto-managed id), create a new row,
     * and persist the updated table back to disk.
     *
     * @param tokens tokenized command list
     * @return {@code [OK]} on success, or {@code [ERROR] ...} if validation/execution fails
     */
    private String handleInsert(List<String> tokens) {
        // 1) Guard checks: INSERT INTO ... VALUES ... and selected database context.
        boolean hasValues = false;
        for (String t : tokens) {
            if (t.equalsIgnoreCase("VALUES")) {
                hasValues = true;
                break;
            }
        }
        if (tokens.size() < 7 || !tokens.get(1).equalsIgnoreCase("INTO") || !hasValues) {
            return "[ERROR] Invalid INSERT command syntax.";
        }
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] No database selected. You must USE a database first.";
        }

        try {
            String tableName = tokens.get(2);
            Table myTable = this.storageManager.loadTable(this.currentDatabase,tableName);

            // 2) Extract user values strictly inside (...) after VALUES.
            int openBracket = tokens.indexOf("(");
            int closeBracket = tokens.indexOf(")");

            if (openBracket == -1 || closeBracket == -1 || openBracket >= closeBracket) {
                return "[ERROR] Missing or malformed parentheses in INSERT command.";
            }

            List<String> valuesToInsert = new ArrayList<>();
            // Parse only the bracket payload and ignore separators.
            for (int i = openBracket + 1; i < closeBracket; i++) {
                String token = tokens.get(i).trim();

                // Ignore delimiters and keep only value tokens.
                if (!token.equals(",") && !token.equals(")") && !token.equals(");")) {
                    // Normalize quoted string literals: 'Alice' -> Alice.
                    valuesToInsert.add(token.replace("'", ""));
                }
            }

            // 3) Schema check: user value count must match non-id columns.
            int expectedValueCount = myTable.getColumnNames().size();
            // id is system-managed, so INSERT should provide one fewer value.
            if (expectedValueCount > 0 && myTable.getColumnNames().get(0).equalsIgnoreCase("id")) {
                expectedValueCount -= 1;
            }

            if (valuesToInsert.size() != expectedValueCount) {
                return "[ERROR] Value count mismatch! Expected " + expectedValueCount +
                        " user-provided values, but got " + valuesToInsert.size() +
                        ". Values extracted: " + String.join(", ", valuesToInsert);
            }

            // 4) Build new row with auto-increment id in column 0.
            Row newRow = new Row();
            newRow.addValue(String.valueOf(myTable.getNextId()));

            for (String val : valuesToInsert) {
                newRow.addValue(val);
            }

            // 5) Update in-memory table and flush to .tab file.
            myTable.addRow(newRow);
            this.storageManager.saveTable(this.currentDatabase, myTable);

            return "[OK]\n";

        } catch (RuntimeException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "[ERROR] Failed to insert data: " + e.getMessage();
        }
    }

    /**
     * Handles the SELECT command to query and filter data from a table.
     * Optimized to cache column indices prior to row iteration, achieving O(N)
     * time complexity instead of O(N*M), and delegates validation to the Table entity.
     * Supports case-insensitive SQL keywords.
     *
     * @param tokens The tokenized SQL command list.
     * @return A formatted string of the queried data or an error message.
     */
    private String handleSelect(List<String> tokens) {
        if (tokens.size() < 4) return "[ERROR] Invalid SELECT command length";
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] You must USE a database first";
        }

        try {
            // 1. Locate FROM (Case-Insensitive) and extract table name
            int fromIndex = -1;
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i).equalsIgnoreCase("FROM")) {
                    fromIndex = i;
                    break;
                }
            }
            if (fromIndex == -1 || fromIndex + 1 >= tokens.size()) return "[ERROR] Missing FROM keyword or table name.";

            String tableName = tokens.get(fromIndex + 1).replace(";", "");
            Table myTable = storageManager.loadTable(this.currentDatabase,tableName);

            // 2. Extract target columns (Projection phase)
            List<String> targetColumns = new ArrayList<>();
            for (int i = 1; i < fromIndex; i++) {
                String token = tokens.get(i);
                if (!token.equals(",")) targetColumns.add(token);
            }

            // Handle wildcard '*' for all columns
            if (targetColumns.contains("*")) targetColumns = myTable.getColumnNames();

            // OOP Magic & Performance Optimization: Cache column indices!
            List<Integer> targetIndices = new ArrayList<>();
            for (String col : targetColumns) {
                targetIndices.add(myTable.getColumnIndexOrThrow(col));
            }

            // 3. Extract WHERE tokens (Selection phase - Case-Insensitive)
            int whereIndex = -1;
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i).equalsIgnoreCase("WHERE")) {
                    whereIndex = i;
                    break;
                }
            }

            boolean hasWhere = (whereIndex != -1);
            List<String> conditionTokens = new ArrayList<>();

            if (hasWhere) {
                for (int i = whereIndex + 1; i < tokens.size(); i++) {
                    String t = tokens.get(i).replace(";", "");
                    if (!t.isEmpty()) conditionTokens.add(t);
                }
            }
            if (!hasWhere) {
                if (fromIndex + 2 < tokens.size()) {
                    String trailingToken = tokens.get(fromIndex + 2);
                    if (!trailingToken.equals(";")) {
                        return "[ERROR] Missing WHERE keyword before conditions.";
                    }
                }
            }

            // 4. Build the Output Header
            StringBuilder result = new StringBuilder("[OK]\n");
            result.append(String.join("\t", targetColumns)).append("\n");

            // 5. Iterate through rows and evaluate conditions
            for (Row row : myTable.getRows()) {
                // Short-circuit logic: print if no WHERE clause, or if AST evaluates to true.
                boolean shouldPrint = !hasWhere || evaluator.evaluate(row, myTable, conditionTokens);

                if (shouldPrint) {
                    // Use the cached indices directly for O(1) instantaneous access
                    for (int index : targetIndices) {
                        result.append(row.getCleanValueAt(index)).append("\t");
                    }
                    result.append("\n");
                }
            }

            return result.toString();

        } catch (RuntimeException e) {
            // Catches getColumnIndexOrThrow and evaluateCondition errors
            return e.getMessage();
        } catch (Exception e) {
            return "[ERROR] Failed to query table: " + e.getMessage();
        }
    }

    private String handleDrop(List<String> tokens) {
        if (tokens.size() < 3) {
            return "[ERROR] Invalid drop command length";
        }
        String targetObjectName = tokens.get(1).toUpperCase();

        if (targetObjectName.equals("TABLE")) {
            if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
                return "[ERROR] You must USE a database first";
            }
            String tableName = tokens.get(2);
            String tablePath = this.storageFolderPath + File.separator + this.currentDatabase + File.separator + tableName + ".tab";
            File tableFile = new File(tablePath);
            if (!tableFile.exists()) {
                return "[ERROR] Table " + tableName + " does not exist";
            } else {
                tableFile.delete();
                return "[OK]";
            }
        } else if (targetObjectName.equals("DATABASE")) {
            String databaseName = tokens.get(2);
            String databasePath = this.storageFolderPath + File.separator + databaseName;
            File databaseFolder = new File(databasePath);
            if (!databaseFolder.exists()) {
                return "[ERROR] Database " + databaseName + " does not exist";
            }

            File[] allFiles = databaseFolder.listFiles();
            if (allFiles != null) {
                for (File file : allFiles) {
                    file.delete();
                }
            }
            databaseFolder.delete();
            if (databaseName.equals(this.currentDatabase)) {
                this.currentDatabase = "";
            }
            return "[OK]";
        }
        return "[ERROR] Invalid DROP target: " + tokens.get(1);

    }

    /**
     * Handles the DELETE command to remove specific rows from a table.
     * Enforces the presence of a WHERE clause to prevent accidental mass deletion.
     * Utilizes reverse iteration to safely remove elements from the ArrayList.
     *
     * @param tokens The tokenized SQL command list.
     * @return A success message [OK] or an error string.
     */
    private String handleDelete(List<String> tokens) {
        if (tokens.size() < 3 || !tokens.get(1).equalsIgnoreCase("FROM")) {
            return "[ERROR] Invalid DELETE command syntax.";
        }
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] No database selected. You must USE a database first.";
        }

        try {
            String tableName = tokens.get(2).replace(";", "");
            Table myTable = storageManager.loadTable(this.currentDatabase, tableName);
            int whereIndex = tokens.indexOf("WHERE");

            // Mandatory WHERE clause check for safety
            if (whereIndex == -1) {
                return "[ERROR] DELETE command must include a WHERE clause.";
            }

            List<String> conditionTokens = new ArrayList<>();
            for (int i = whereIndex + 1; i < tokens.size(); i++) {
                String t = tokens.get(i).replace(";", "");
                if (!t.isEmpty()) conditionTokens.add(t);
            }

            // Using Iterator for safe removal
            List<Row> rows = myTable.getRows();
            Iterator<Row> iterator = rows.iterator();

            while (iterator.hasNext()) {
                Row row = iterator.next();
                // Check the condition using our AST evaluator
                if (evaluator.evaluate(row, myTable, conditionTokens)) {
                    // The iterator's remove() method is the ONLY safe way
                    // to remove items while iterating forward.
                    iterator.remove();
                }
            }

            storageManager.saveTable(this.currentDatabase, myTable); //
            return "[OK]\n";

        } catch (RuntimeException e) {
            // Catches exceptions thrown by loadTableFromFile or evaluateCondition (e.g. column not found)
            return e.getMessage();
        } catch (Exception e) {
            return "[ERROR] Failed to execute DELETE: " + e.getMessage();
        }
    }

    private String handleAlter(List<String> tokens) {
        if (tokens.size() < 5) {
            return "[ERROR] Invalid ALTER command length";
        }
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] You must USE a database first";
        }

        String tableName = tokens.get(2);

        try {
            Table myTable = this.storageManager.loadTable(this.currentDatabase, tableName);
            String action = tokens.get(3).toUpperCase();
            String columnName = tokens.get(4);

            if (action.equals("ADD")) {
                if (myTable.getColumnNames().contains(columnName)) {
                    return "[ERROR] Column " + columnName + " already exists";
                }
                myTable.addColumnName(columnName);
                for (Row row : myTable.getRows()) {
                    row.addValue("NULL");
                }

            }else if (action.equals("DROP")) {
                if (!myTable.getColumnNames().contains(columnName)) {
                    return "[ERROR] Column " + columnName + " does not exist";
                } else if (columnName.equalsIgnoreCase("id")) {
                    return "[ERROR] Id cannot be deleted";
                }
                int index = myTable.getColumnNames().indexOf(columnName);
                myTable.getColumnNames().remove(index);
                for (Row row : myTable.getRows()) {
                    row.getValues().remove(index);
                }
            }else {
                return "[ERROR] Invalid ALTER command (ONLY ADD or DROP)";
            }
            String dbFolderPath = this.storageFolderPath + File.separator + this.currentDatabase;
            myTable.saveToFile(dbFolderPath);
            return "[OK]";
        } catch (IOException e) {
            return "[ERROR] Failed to alter table: " + e.getMessage();
        }
    }

    /**
     * Handles the UPDATE command to modify existing records in a table.
     * Includes security checks to prevent modification of the protected 'id' primary key.
     *
     * @param tokens The tokenized SQL command list.
     * @return A success message [OK] or an error string.
     */
    private String handleUpdate(List<String> tokens) {
        if (tokens.size() < 6 || !tokens.contains("SET")) {
            return "[ERROR] Invalid UPDATE command syntax.";
        }
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] No database selected. You must USE a database first.";
        }

        try {
            String tableName = tokens.get(1);
            Table myTable = this.storageManager.loadTable(this.currentDatabase, tableName);

            // Locate boundaries for SET and WHERE clauses
            int setIndex = tokens.indexOf("SET");
            int whereIndex = tokens.indexOf("WHERE");
            boolean hasWhere = (whereIndex != -1);

            // Extract the target column and new value (Format: SET column = 'value')
            String targetCol = tokens.get(setIndex + 1);

            // CRITICAL SECURITY FIX: Prevent users from manually updating the auto-generated ID!
            // The 'id' column is a protected primary key managed solely by the database engine.
            if (targetCol.equalsIgnoreCase("id")) {
                return "[ERROR] The 'id' column is a protected primary key and cannot be updated manually.";
            }

            String newValue = tokens.get(setIndex + 3).replace("'", "").trim();

            // Delegate index resolution to the Table class.
            // This will safely throw an exception if the column doesn't exist.
            int targetColIndex = myTable.getColumnIndexOrThrow(targetCol);

            // Extract condition tokens if a WHERE clause exists
            List<String> conditionTokens = new ArrayList<>();
            if (hasWhere) {
                for (int i = whereIndex + 1; i < tokens.size(); i++) {
                    String t = tokens.get(i).replace(";", "");
                    if (!t.isEmpty()) conditionTokens.add(t);
                }
            }

            // Iterate through rows and apply the update
            for (Row row : myTable.getRows()) {
                // Determine if the current row matches the WHERE condition (or if there is no WHERE)
                boolean shouldUpdate = !hasWhere || this.evaluator.evaluate(row, myTable, conditionTokens);

                if (shouldUpdate) {
                    // Delegate the actual data mutation to the Row class
                    row.updateValueAt(targetColIndex, newValue);
                }
            }

            // Persist the modified state back to the hard drive
            this.storageManager.saveTable(this.currentDatabase, myTable);
            return "[OK]\n";

        } catch (RuntimeException e) {
            // Catches getColumnIndexOrThrow and evaluateCondition errors
            return e.getMessage();
        } catch (Exception e) {
            return "[ERROR] Failed to execute UPDATE: " + e.getMessage();
        }
    }

    /**
     * Handles the JOIN command to combine rows from two tables bases on a related column.
     *
     * @param tokens The tokenized SQL command list.
     * @return A formatted string representation of the joined data.
     */
    private  String handleJoin(List<String> tokens)  {
        // Syntax expected: JOIN table1 AND table2 ON col1 AND col2
        if (tokens.size() < 8) {
            return "[ERROR] Invalid JOIN command length";
        }
        if (!tokens.get(2).equalsIgnoreCase("AND") ||
                !tokens.get(4).equalsIgnoreCase("ON") ||
                !tokens.get(6).equalsIgnoreCase("AND"))  {
            return "[ERROR] Invalid JOIN syntax";
        }
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] You must USE a database first";
        }

        try {
            // Extract table and column names
            String table1Name = tokens.get(1);
            String table2Name = tokens.get(3);
            String col1Name = tokens.get(5);
            String col2Name = tokens.get(7);

            // Load tables from disk
            Table table1 = this.storageManager.loadTable(this.currentDatabase, table1Name);
            Table table2 = this.storageManager.loadTable(this.currentDatabase, table2Name);

            // Resolve column indices using OOP magic
            int index1 = table1.getColumnIndexOrThrow(col1Name);
            int index2 = table2.getColumnIndexOrThrow(col2Name);
            if (index1 == -1 ) {
                return "[ERROR] Column " + col1Name + " does not exist";
            }
            if (index2 == -1 ) {
                return "[ERROR] Column " + col2Name + " does not exist";
            }

            // Initialize the result builder and build the unified header
            StringBuilder result = new StringBuilder();
            result.append("[OK]").append("\n");

            // Builder the header: id | table1.col | table2.col
            result.append("id\t");
            buildJoinHeader(result,table1);
            buildJoinHeader(result, table2);
            result.append("\n");

            // Nested Loop Join (The core engine of relational databases)
            int joinIdCounter = 1;

            for (Row r1: table1.getRows()) {
                String matchVal1 = r1.getCleanValueAt(index1);

                for (Row r2: table2.getRows()) {
                    String matchVal2 = r2.getCleanValueAt(index2);

                    // If the ON condition holds, merge and append the rows
                    if (matchVal1.equals(matchVal2)) {
                        result.append(joinIdCounter++).append("\t");
                        buildJoinDataRow(result, table1, r1);
                        buildJoinDataRow(result, table2, r2);
                        result.append("\n");
                    }
                }
            }
            return result.toString();

        } catch (Exception e) {
            return "[ERROR] " + e.getMessage();
        }

    }

    private boolean isValidName(String name) {
        String[] keywords = {"USE", "CREATE", "ALTER", "INSERT", "INTO",
                "VALUES", "SELECT", "FROM", "WHERE", "UPDATE",
                "SET", "DELETE", "JOIN", "AND", "ON",
                "TURE", "FALSE", "LIKE"};
        for (String keyword : keywords) {
            if (name.equalsIgnoreCase(keyword)) return false;
        }

        return name.matches("^[A-Za-z0-9_]+$");
    }

    /**
     * Helper method to append table headers with proper prefixes (e.g., table1.name)
     * to avoid naming collisions in the merged JOIN view.
     */
    private void buildJoinHeader(StringBuilder sb, Table table) {
        for (String colName : table.getColumnNames()) {
            // Usually, the original 'id' columns are omitted in the joined output
            // to avoid confusion with the newly generated join ID.
            if (!colName.equalsIgnoreCase("id")) {
                sb.append(table.getTableName()).append(".").append(colName).append("\t");
            }
        }
    }

    /**
     * Helper method to append row data for the JOIN view,
     * meticulously skipping the original ID columns to align with the header.
     */

    private void buildJoinDataRow(StringBuilder sb, Table table, Row row) {
        List<String> colNames = table.getColumnNames();
        List<String> values = row.getValues();

        for (int i = 0; i < colNames.size(); i++) {
            if (!colNames.get(i).equalsIgnoreCase("id")) {
                sb.append(values.get(i).replace("'", "")).append("\t");
            }
        }
    }
}



