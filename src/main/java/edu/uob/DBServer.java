package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;
    private String currentDatabase;

    public static void main(String args[]) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    /**
    * KEEP this signature otherwise we won't be able to mark your submission correctly.
    */
    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
        this.currentDatabase = "";
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming DB commands and carries out the required actions.
    */
    public String handleCommand(String command) {
        // TODO implement your server logic here
        String trimmedCommand = command.trim();
        if (trimmedCommand.isEmpty()) {
            return "[ERROR] Empty command";
        }
        if (!trimmedCommand.endsWith(";")) {
            return "[ERROR] Invalid syntax: Command must end with a semicolon (;)";
        }
        try {
            Tokenizer tokenizer = new Tokenizer();
            List<String> tokens = tokenizer.parseTokens(trimmedCommand);

            if (tokens.isEmpty()) {
                return "[ERROR] Empty command";
            }


            String firstWord = tokens.get(0).toUpperCase();

            switch (firstWord) {
                case "USE": return handleUse(tokens);
                case "CREATE": return handleCreate(tokens);
                case "INSERT": return handleInsert(tokens);
                case "SELECT": return handleSelect(tokens);
                case "DROP"  : return handleDrop(tokens);
                case "ALTER" : return handleAlter(tokens);
                case "DELETE": return handleDelete(tokens);
                case "UPDATE": return handleUpdate(tokens);
                case "JOIN"  : return handleJoin(tokens);
                default:
                    return "[ERROR] Unknown command: " + firstWord;
            }
        } catch (Exception e){
            return "[ERROR] " + e.getMessage();
        }
    }

    private String handleUse(List<String> tokens) {
        if (tokens.size() < 2) {
            return "[ERROR] Invalid USE command";
        }

        String dbName = tokens.get(1);
        File dbFolder = new File(storageFolderPath + File.separator + dbName);

        if (dbFolder.exists() && dbFolder.isDirectory()) {
            this.currentDatabase = dbName;
            return "[OK]";
        } else {
            return "[ERROR] Database " + dbName + " does not exist or is not a directory";
        }
    }

    /**
     * Handles the CREATE command (DATABASE or TABLE).
     * Automatically prepends the mandatory 'id' column to all new tables
     * to ensure compatibility with standard relational operations (e.g., JOIN, INSERT).
     *
     * @param tokens The tokenized SQL command list.
     * @return A success message [OK] or an error string.
     */
    private String handleCreate(List<String> tokens) {
        if (tokens.size() < 3) return "[ERROR] Invalid CREATE syntax.";
        String createType = tokens.get(1).toUpperCase();
        String targetName = tokens.get(2);

        try {
            if (createType.equals("DATABASE")) {
                // Ensure the target database directory exists
                File dbFolder = new File(this.storageFolderPath + File.separator + targetName);
                if (!dbFolder.exists()) {
                    dbFolder.mkdirs();
                }
                return "[OK]\n";

            } else if (createType.equals("TABLE")) {
                if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
                    return "[ERROR] No database selected. Please USE a database first.";
                }

                // Check for table existence to prevent accidental overwrites
                File tableFile = new File(this.storageFolderPath + File.separator + this.currentDatabase + File.separator + targetName + ".tab");
                if (tableFile.exists()) return "[ERROR] Table " + targetName + " already exists.";

                Table newTable = new Table(targetName);

                // CRITICAL FIX: Automatically prepend the mandatory 'id' column!
                // This ensures all inserted rows have a primary key to match against.
                newTable.addColumn("id");

                // Parse custom column names enclosed in parentheses, e.g., (name, age, email)
                int openBracket = tokens.indexOf("(");
                int closeBracket = tokens.indexOf(")");

                if (openBracket != -1 && closeBracket != -1 && openBracket < closeBracket) {
                    for (int i = openBracket + 1; i < closeBracket; i++) {
                        String colName = tokens.get(i);
                        if (!colName.equals(",")) {
                            newTable.addColumn(colName);
                        }
                    }
                }

                // Persist the new table schema to disk
                saveTableToFile(newTable);
                return "[OK]\n";
            }

            return "[ERROR] Unknown CREATE target. Expected DATABASE or TABLE.";

        } catch (Exception e) {
            return "[ERROR] Failed to execute CREATE: " + e.getMessage();
        }
    }

    /**
     * Handles the INSERT INTO command to add a new record to a table.
     * Utilizes the Table's internal auto-increment ID generator to ensure
     * primary key uniqueness and delegates I/O to the persistence helper.
     *
     * @param tokens The tokenized SQL command list.
     * @return A success message [OK] or an error string.
     */
    private String handleInsert(List<String> tokens) {
        // 1. Guard Clauses for syntax and context validation
        if (tokens.size() < 7 || !tokens.get(1).equalsIgnoreCase("INTO") || !tokens.contains("VALUES")) {
            return "[ERROR] Invalid INSERT command syntax.";
        }
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] No database selected. You must USE a database first.";
        }

        try {
            String tableName = tokens.get(2);
            Table myTable = loadTableFromFile(tableName);

            // 2. Safely extract values between parentheses: ( val1 , val2 )
            int openBracket = tokens.indexOf("(");
            int closeBracket = tokens.indexOf(")");

            if (openBracket == -1 || closeBracket == -1 || openBracket >= closeBracket) {
                return "[ERROR] Missing or malformed parentheses in INSERT command.";
            }

            List<String> valuesToInsert = new ArrayList<>();
            // Only loop STRICTLY between the brackets to prevent Tokenizer artifacts
            for (int i = openBracket + 1; i < closeBracket; i++) {
                String token = tokens.get(i).trim();

                // Skip commas AND any stray closing brackets or semicolons
                // that might have snuck in due to the Tokenizer implementation
                if (!token.equals(",") && !token.equals(")") && !token.equals(");")) {
                    // Remove string literal quotes
                    valuesToInsert.add(token.replace("'", ""));
                }
            }

            // 3. Schema Validation: Dynamic calculation of expected columns
            int expectedValueCount = myTable.getColumnNames().size();
            // Since we mandate 'id' as the first column, we expect one less value from the user
            if (expectedValueCount > 0 && myTable.getColumnNames().get(0).equalsIgnoreCase("id")) {
                expectedValueCount -= 1;
            }

            if (valuesToInsert.size() != expectedValueCount) {
                return "[ERROR] Value count mismatch! Expected " + expectedValueCount +
                        " user-provided values, but got " + valuesToInsert.size() +
                        ". Values extracted: " + String.join(", ", valuesToInsert);
            }

            // 4. Create new Row and use OOP Magic to generate ID!
            Row newRow = new Row();
            newRow.addValue(String.valueOf(myTable.getNextId())); // The magic auto-increment ID

            for (String val : valuesToInsert) {
                newRow.addValue(val);
            }

            // 5. Update domain model and persist to disk
            myTable.addRow(newRow);
            saveTableToFile(myTable);

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
     * time complexity instad of O(N*M), and delegates validation to the Table entity.
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
            // 1. Locate FROM and extract table name
            int fromIndex = tokens.indexOf("FROM");
            if (fromIndex == -1 || fromIndex + 1 >= tokens.size()) return "[ERROR] Missing FROM keyword or table name.";

            String tableName = tokens.get(fromIndex+1).replace(";","");
            Table myTable = loadTableFromFile(tableName);

            // 2. Extract target columns (Projection phase)
            List<String> targetColumns = new ArrayList<>();
            for (int i = 1; i < fromIndex; i++) {
                String token = tokens.get(i);
                if (!token.equals(",")) targetColumns.add(token);
            }

            // Handle wildcard '*' for all columns
            if (targetColumns.contains("*")) targetColumns = myTable.getColumnNames();

            // OOP Magic & Performance Optimization: Cache column indices!
            // This prevents searching for the column name in every single row iteration.
            List<Integer> targetIndices = new ArrayList<>();
            for (String col : targetColumns) {
                // This will automatically throw an exception if the column does not exist
                targetIndices.add(myTable.getColumnIndexOrThrow(col));
            }

            // 3. Extract WHERE tokens (Selection phase)
            int whereIndex = tokens.indexOf("WHERE");
            boolean hasWhere = (whereIndex != -1);
            List<String> conditionTokens = new ArrayList<>();

            if (hasWhere) {
                for (int i = whereIndex + 1; i < tokens.size(); i++) {
                    String t = tokens.get(i).replace(";","");
                    if (!t.isEmpty()) conditionTokens.add(t);
                }
            }

            // 4. Build the Output Header
            StringBuilder result = new StringBuilder("[OK]\n");
            result.append(String.join("\t", targetColumns)).append("\n");

            // 5. Iterate through rows and evaluate conditions
            for (Row row : myTable.getRows()) {
                // Utilize the elegant short-circuit logic we buil earlier.
                boolean shouldPrint = !hasWhere || evaluateCondition(row, myTable, conditionTokens);

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
            // Fixed the copy-paste bug: changed "insert" to "query"
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
     * Handles the DELETE FROM command to remove records matching specific conditions.
     * Implements a reverse-iteration strategy (i--) to safely remove elements from
     * the dynamic ArrayList without triggering index-shifting anomalies.
     *
     * @param tokens The tokenized SQL command list.
     * @return A success message [OK] or an error string.
     */
    private String handleDelete(List<String> tokens) throws IOException {
        if (tokens.size() < 3 || !tokens.get(1).equalsIgnoreCase("FROM")) {
            return "[ERROR] Invalid DELETE command syntax";
        }
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] You must USE a database first";
        }
        try {
            String tableName = tokens.get(2);
            Table myTable = loadTableFromFile(tableName);

            // Locate and extract WHERE conditions
            int whereIndex = tokens.indexOf("WHERE");
            boolean hasWhere = (whereIndex != -1);
            List<String> conditionTokens = new ArrayList<>();

            if (hasWhere) {
                for (int i = whereIndex + 1; i < tokens.size(); i++) {
                    String t = tokens.get(i).replace(";","");
                    if (!t.isEmpty()) conditionTokens.add(t);
                }
            }

            // Reverse Iteration Deletion (Crucial for ArrayList stability)
            List<Row> rows = myTable.getRows();
            for (int i = rows.size() - 1; i >= 0; i--) {
                Row row = rows.get(i);
                boolean shouldDelete = !hasWhere || evaluateCondition(row, myTable, conditionTokens);

                if (shouldDelete) rows.remove(i);
            }

            // Persist changes
            saveTableToFile(myTable);
            return "[OK]\n";
        } catch (RuntimeException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "[ERROR] Failed to delete: " + e.getMessage();
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
            Table myTable = loadTableFromFile(tableName);
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

    // UPDATE student SET age = 26 WHERE name == 'Alice' ;
    private String handleUpdate(List<String> tokens) throws IOException {
        if (tokens.size() < 6 || !tokens.contains("SET")) return "[ERROR] Invalid UPDATE syntax";
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) return "[ERROR] You must USE a database first";

        try {
            String tableName = tokens.get(1);
            Table myTable = loadTableFromFile(tableName);

            // Locate boundaries for SET and WHERE clauses
            int setIndex = tokens.indexOf("SET");
            int whereIndex = tokens.indexOf("WHERE");
            boolean hasWhere = (whereIndex != -1);
            int endOfSet = hasWhere ? whereIndex : tokens.size();

            // Extract the target column and new value (Assuming standard format: SET column = 'value')
            String targetCol = tokens.get(setIndex + 1);
            String newValue = tokens.get(setIndex + 3).replace("'", "").trim();

            // Delegate index resolution to the Table class
            int targetColIndex = myTable.getColumnIndexOrThrow(targetCol);

            // Extract condition tokens if a WHERE clause exists
            List<String> conditionTokens = new ArrayList<>();
            if (hasWhere) {
                for (int i = whereIndex + 1; i < tokens.size(); i++) {
                    String t = tokens.get(i).replace(";","");
                    if (!t.isEmpty()) conditionTokens.add(t);
                }
            }

            // Iterate through rows and apply the update
            for (Row row : myTable.getRows()) {
                // If no WHERE clause, update all. Otherwise, ask the AST parser.
                boolean shouldUpdate = !hasWhere || evaluateCondition(row, myTable, conditionTokens);

                if (shouldUpdate) {
                    // Delegate the data mutation to the Row class
                    row.updateValueAt(targetColIndex, newValue);
                }
            }
            saveTableToFile(myTable);
            return "[OK]\n";
        } catch (Exception e) {
            return "[ERROR] Failed to update table: " + e.getMessage();
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
            Table table1 = loadTableFromFile(table1Name);
            Table table2 = loadTableFromFile(table2Name);

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



    //  === Methods below handle networking aspects of the project - you will not need to change these ! ===

    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }

    /**
     * Recursively evaluates complex boolean conditions (AST parsing).
     * Handles nested parentheses, AND/OR logical operators, and base conditions.
     *
     * @param row The current database row being evaluated.
     * @param table The table metadata used to resolve column indices.
     * @param condTokens The tokenized WHERE clause condition.
     * @return true if the row satisfies the condition, false otherwise.
     * @throws RuntimeException if the condition syntax is invalid.
     */
    private boolean evaluateCondition(Row row, Table table, List<String> condTokens) {
        if (condTokens == null || condTokens.isEmpty()) {
            throw new RuntimeException("[ERROR] Empty condition provided.");
        }

        int bracketDepth = 0;
        int mainOpIndex = -1;
        String mainOp = "";

        // Locate the top-level logical operator (AND/ OR) outside of any brackets
        for (int i = 0; i < condTokens.size(); i++) {
            String token = condTokens.get(i);
            if (token.equals("(")) {
                bracketDepth++;
            } else if (token.equals(")")) {
                bracketDepth--;
            } else if (bracketDepth == 0 &&
                    (token.equalsIgnoreCase("AND") || token.equalsIgnoreCase("OR"))) {
                mainOpIndex = i;
                mainOp = token.toUpperCase();
                break;
            }
        }

        // Recursive splitting if a top-level operator is found
        if (mainOpIndex != -1) {
            List<String> leftTokens = condTokens.subList(0, mainOpIndex);
            List<String> rightTokens = condTokens.subList(mainOpIndex + 1, condTokens.size());

            boolean leftResult = evaluateCondition(row, table, leftTokens);
            boolean rightResult = evaluateCondition(row, table, rightTokens);

            return mainOp.equals("AND") ? (leftResult && rightResult) : (leftResult || rightResult);
        }

        // Strip wrapping parentheses if the whole expression is enclosed
        if (condTokens.get(0).equals("(") && condTokens.get(condTokens.size() - 1).equals(")")) {
            return evaluateCondition(row, table, condTokens.subList(1, condTokens.size()-1));
        }

        // Base Case: Evaluate simple condition triplet (Column, Operator, Value)
        if (condTokens.size() == 3) {
            String col =  condTokens.get(0);
            String op = condTokens.get(1);
            String val = condTokens.get(2); // String iteral cleaning is delegated to checkCondtion
            return checkCondition(row, table, col, op, val);
        }

        throw new RuntimeException("[ERROR] Invalid condition syntax: " + String.join(" ", condTokens));
    }

    /**
     * Evaluates a base condition (e.g., age > 20) against a specific row.
     * Utilizes encapsulated Table and Row methods to prevent tight coupling.
     *
     * @param row The current row being checked.
     * @param table The table object for schema reference.
     * @param columnName The column to check against.
     * @param operator The comparative operator (==, !=, > LIKE, etc.)
     * @param targetValue The value to compare with.
     * @return true if the condition holds, false otherwise,
     */
    private boolean checkCondition(Row row, Table table, String columnName, String operator, String targetValue) {
        // Delegate index resolution to the Table class (Decoupling)
        int colIndex = table.getColumnIndexOrThrow(columnName);

        // Delegate data extraction to the Row class, but clean the target here
        String cellValue = row.getCleanValueAt(colIndex);
        String cleanTarget = targetValue.replace("'","").trim();

        switch (operator.toUpperCase()) {
            case "==":
                return cellValue.equals(cleanTarget);
            case "!=":
                return !cellValue.equals(cleanTarget);
            case "LIKE":
                return cellValue.contains(cleanTarget);
            case ">":
            case ">=":
            case "<":
            case "<=":
                try {
                    float cellNum = Float.parseFloat(cellValue);
                    float targetNum = Float.parseFloat(targetValue);
                    switch (operator) {
                        case ">": return cellNum > targetNum;
                        case "<": return cellNum < targetNum;
                        case "<=": return cellNum <= targetNum;
                        case ">=": return cellNum >= targetNum;
                        default: return false;
                    }
                } catch (NumberFormatException e) {
                    throw new RuntimeException("[ERROR] Cannot use math operators on non-number values");
                }
            default:
                throw new RuntimeException("[ERROR] Unknown operator: " + operator);
        }
    }

    private void saveTableToFile(Table tableToSave) {
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            throw new RuntimeException("[ERROR] No database selected. Cannot save table.");
        }

        try {
            // Construct the database directory path
            String dbFolderPath = this.storageFolderPath + File.separator + this.currentDatabase;
            // Delegate the actual file writing to the Table object
            tableToSave.saveToFile(dbFolderPath);
        } catch (IOException e) {
            throw new RuntimeException("[ERROR] Failed to save table to disk: " + e.getMessage());
        }
    }

    private Table loadTableFromFile(String tableName) {
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            throw new RuntimeException("[ERROR] No database selected. Cannot load table.");
        }

        String tablePath = this.storageFolderPath + File.separator + this.currentDatabase + File.separator + tableName + ".tab";
        File file = new File(tablePath);

        if (!file.exists()) {
            throw new RuntimeException("[ERROR] Table " + tableName + " does not exist.");
        }

        try {
            Table loadedTable = new Table(tableName);
            Scanner scanner = new Scanner(file);

            // Phase 1: Parse Header
            if (scanner.hasNextLine()) {
                String headerLine = scanner.nextLine();
                String[] headers = headerLine.split("\t", -1);
                loadedTable.setColumnNames(new ArrayList<>(Arrays.asList(headers)));
            }

            int maxIdFound = 0;
            // Phase 2: Parse Data Rows
            while (scanner.hasNextLine()) {
                String dataLine = scanner.nextLine();
                if (dataLine.trim().isEmpty()) continue; // Skip empty lines

                String[] values = dataLine.split("\t", -1);
                Row newRow = new Row();
                for (String val : values) {
                    newRow.addValue(val);
                }
                loadedTable.addRow(newRow);

                // Phase 3 : ID Recalibration tracking
                if (values.length > 0) {
                    try {
                        int currentId = Integer.parseInt(values[0]);
                         if (currentId > maxIdFound) {
                             maxIdFound = currentId;
                         }
                    } catch (NumberFormatException e) {
                        // Ignore if the first column is not a numeric ID
                    }
                }
                scanner.close();

                // Calibrate the ID generator to avoid collisions with existing data
                loadedTable.updateNextAvailableId(maxIdFound);
            }
            return loadedTable;
        }catch (IOException e) {
            throw new RuntimeException("[ERROR] Failed to read table file: " + e.getMessage());
        }
    }
}


