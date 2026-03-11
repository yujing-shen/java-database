package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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
        try {
            Tokenizer tokenizer = new Tokenizer();
            List<String> tokens = tokenizer.tokenize(command);

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
            return "[OK] Switched to databse " + dbName;
        } else {
            return "[ERROR] Database " + dbName + " does not exist or is not a directory";
        }
    }
    private String handleCreate(List<String> tokens) {
        if (tokens.size() < 3) {
            return "[ERROR] Invalid CREATE command";
        }

        String secondWord = tokens.get(1).toUpperCase();
        if (secondWord.equals("DATABASE")) {
            String dbName = tokens.get(2);
            java.io.File newDbFolder = new java.io.File(storageFolderPath + File.separator + dbName);

            if (newDbFolder.exists()) {
                return "[ERROR] Database already exists";
            } else {
                newDbFolder.mkdirs();
                return "[OK] DATABASE " + dbName + " created successfully";
            }
        }
        else if (secondWord.equals("TABLE")) {
            if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
                return "[ERROR] You must USE a database first";
            }
            String tableName = tokens.get(2);

            String tablePath = this.storageFolderPath + File.separator + this.currentDatabase + File.separator + tableName + ".tab";
            File tableFile = new File(tablePath);
            if (tableFile.exists()) {
                return "[ERROR] Table " + tableName + "already exists";
            }
            Table newTable = new Table(tableName);
            newTable.addColumnName("id");

            boolean insideBrackets = false;
            for (int i = 3; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if (token.equals("(")) {
                    insideBrackets = true;
                    continue;
                }
                if (token.equals(")")) {
                    break;
                }
                if (insideBrackets) {
                    if (!token.equals(",")) {
                        newTable.addColumnName(token);
                    }
                }
            }
            try {
                String dbFolderPath = this.storageFolderPath + File.separator + this.currentDatabase;
                newTable.saveToFIle(dbFolderPath);
                return "[OK] Table " + tableName + " created successfully";
            } catch (IOException e) {
                return "[ERROR] Failed to create table file: " + e.getMessage();
            }
        }
        else {
            return "[ERROR] Invalid CREATE command";
        }

    }
    private String handleInsert(List<String> tokens) {
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] You must USE a database first";
        }

        if (tokens.size() < 6 ||
                !tokens.get(0).equals("INSERT") ||
                !tokens.get(1).equals("INTO") ||
                !tokens.get(3).equals("VALUES")
        ) {
            return "[ERROR] Invalid INSERT command";
        }


        String tableName = tokens.get(2);
        String tablePath = this.storageFolderPath + File.separator + this.currentDatabase + File.separator + tableName + ".tab";
        File tableFile = new File(tablePath);
        if (!tableFile.exists()) {
            return "[ERROR] Database " + tableName + " does not exist";
        }

        try {
            Table myTable = loadTableFromFile(tableName);
            Row row = new Row();

            int id = myTable.getNextId();
            row.addValue(String.valueOf(id));

            boolean insideBrackets = false;
            for(int i = 4; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if (token.equals("(")) {
                    insideBrackets = true;
                    continue;
                }
                if (token.equals(")")) {
                    break;
                }
                if (insideBrackets) {
                    if (!token.equals(",")) {
                        row.addValue(token);
                    }
                }
            }
            myTable.addRow(row);

            String dbFolderPath = this.storageFolderPath + File.separator + this.currentDatabase;
            myTable.saveToFIle(dbFolderPath);
            return "[OK] Table " + tableName + " inserted successfully";

        } catch (Exception e) {
            return "[ERROR] Failed to insert table: " + e.getMessage();
        }



    }
    private String handleSelect(List<String> tokens) {
        if (this.currentDatabase == null || this.currentDatabase.isEmpty()) {
            return "[ERROR] You must USE a database first";
        }
        // Find fromIndex
        int fromIndex = -1;
        for (int i = 1; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equalsIgnoreCase("FROM")) {
                fromIndex = i;
                break;
            }
        }
        if (fromIndex == -1 || fromIndex + 1 >= tokens.size()) {
            return "[ERROR] Invalid SELECT command, missing FROM or table name";
        }

        String tableName = tokens.get(fromIndex + 1);
        try {
            Table myTable = loadTableFromFile(tableName);
            List<String> targetColumns = new ArrayList<>();

            for (int i = 1; i < fromIndex; i++) {
                String token = tokens.get(i);
                if (!token.equals(",")) {
                    targetColumns.add(token);
                }
            }

            if (targetColumns.contains("*")) {
                targetColumns = new ArrayList<>(myTable.getColumnNames());
            }

            StringBuilder result = new StringBuilder();
            result.append("[OK]\n");


            for (String column : targetColumns) {
                if (!myTable.getColumnNames().contains(column)) {
                    return "[ERROR] Column " + column + " does not exist";
                }
                result.append(column).append("\t");
                }
                result.append("\n");

            List<String> columnNames = myTable.getColumnNames();
            for (Row row : myTable.getRows()) {
                for (String col : targetColumns) {
                    int index = columnNames.indexOf(col);
                    result.append(row.getValueAt(index)).append("\t");
                }
                result.append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            return "[ERROR] Failed to select table: " + e.getMessage();
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
                return "[OK] Table " + tableName + " dropped successfully";
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
            return "[OK] Database " + databaseName + " dropped successfully";
        }
        return "[ERROR] Invalid DROP target: " + tokens.get(1);

    }

    private Table loadTableFromFile(String tableName) throws IOException {
        String name = this.storageFolderPath + File.separator + this.currentDatabase + File.separator + tableName + ".tab";
        File fileToOpen = new File(name);

        if (!fileToOpen.exists()) {
            throw new IOException("Table " + tableName + " does not exist");
        }

        FileReader reader = new FileReader(fileToOpen);
        BufferedReader buffReader = new BufferedReader(reader);

        Table myTable = new Table(tableName);
        int maxId = 0;

        String headerLine = buffReader.readLine();
        if (headerLine != null) {
            String[] headers = headerLine.split("\t");
            for (String header : headers) {
                myTable.addColumnName(header);
            }
        }

        String dataline = buffReader.readLine();
        while (dataline != null) {
            String[] values = dataline.split("\t");
            Row newRow = new Row();
            for (String value : values) {
                newRow.addValue(value);
            }
            myTable.addRow(newRow);

            try {
                int currentId = Integer.parseInt(values[0]);
                if (currentId > maxId) {
                    maxId = currentId;
                }
            } catch (NumberFormatException nfe) {

            }
            dataline = buffReader.readLine();
        }
        buffReader.close();
        myTable.updateNextAvailableId(maxId);

        return myTable;
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
}
