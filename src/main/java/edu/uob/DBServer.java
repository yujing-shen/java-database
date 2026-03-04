package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;

/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;

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
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming DB commands and carries out the required actions.
    */
    public String handleCommand(String command) {
        // TODO implement your server logic here
        StringBuilder content = new StringBuilder();
        try {
            String name = this.storageFolderPath + File.separator + "people.tab";
            File fileToOpen = new File(name);
            FileReader reader = new FileReader(fileToOpen);
            BufferedReader buffReader = new BufferedReader(reader);

            Table myTable = new Table("people");
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
            System.out.println(myTable.getRows().size() + " rows updated");
            System.out.println("The next Id will be " + myTable.getNextNextId());

            // Task 5: Java Data Structures
            int ageColumnIndex = myTable.getColumnNames().indexOf("Age");
            if (ageColumnIndex != -1) {
                for (Row row : myTable.getRows()) {
                    String oldAgeStr = row.getValueAt(ageColumnIndex);
                    try {
                        int newAge = Integer.parseInt(oldAgeStr) + 1;
                        row.setValueAt(ageColumnIndex, String.valueOf(newAge));
                    }
                    catch (NumberFormatException nfe) {

                    }
                }
            }
            myTable.saveToFIle(this.storageFolderPath);
            return "[OK] Table loaded, modified, and saved";
        } catch (IOException ioe) {
            System.out.println("THE FILE DOES NOT EXIST");
            return "[ERROR] " + ioe.getMessage();
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
}
