package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.sql.Array;
import java.util.*;

/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;

    private DatabaseEngine engine;

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
        this.engine = new DatabaseEngine(storageFolderPath);
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
        // check if the quotes are double
        int quoteCount = trimmedCommand.length() - trimmedCommand.replace("'","").length();
        if (quoteCount % 2 != 0) return "[ERROR] Unclosed single quote.";

        // check if the brackets are double
        int openBrackets = trimmedCommand.length() - trimmedCommand.replace("(", "").length();
        int closeBrackets = trimmedCommand.length() - trimmedCommand.replace(")", "").length();
        if (openBrackets != closeBrackets) return "[ERROR] Unbalanced brackets.";

        try {
            Tokenizer tokenizer = new Tokenizer();
            List<String> tokens = tokenizer.parseTokens(trimmedCommand);

            if (tokens.isEmpty()) {
                return "[ERROR] Empty command";
            }

            return this.engine.executeCommand(tokens);
        } catch (Exception e) {
            return "[ERROR] " + e.getMessage();
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


