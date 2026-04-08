package edu.uob;

import java.io.IOException;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class DBServerHarness
{
   String reportTag = "\033[0m[\033[0;33mREPORT\033[0m]\033[0;33m ";
   String bar = "\n" + reportTag + "---------------------------------------------------------------------------\033[0m\n";

   DBServer server;

   public DBServerHarness()
   {
      server = new DBServer();
   }

   String createMovieDatabase()
   {
      String randomName = "";
      for(int i=0; i<6 ;i++) randomName += (char)( 97 + (Math.random() * 25.0));
      sendCommandAndIgnoreResponse("CREATE DATABASE " + randomName + ";");
      sendCommandAndIgnoreResponse("USE " + randomName + ";");

      sendCommandAndIgnoreResponse("CREATE TABLE actors (name, nationality, awards);");
      sendCommandAndIgnoreResponse("INSERT INTO actors VALUES ('Hugh', 'British', 3);");
      sendCommandAndIgnoreResponse("INSERT INTO actors VALUES ('Toni', 'Australian', 12);");
      sendCommandAndIgnoreResponse("INSERT INTO actors VALUES ('James', 'American', 8);");
      sendCommandAndIgnoreResponse("INSERT INTO actors VALUES ('Emma', 'British', 10);");

      sendCommandAndIgnoreResponse("CREATE TABLE movies (name, genre, UK);");
      sendCommandAndIgnoreResponse("INSERT INTO movies VALUES ('Mickey', 'Comedy', FALSE);");
      sendCommandAndIgnoreResponse("INSERT INTO movies VALUES ('Boy', 'Comedy', TRUE);");
      sendCommandAndIgnoreResponse("INSERT INTO movies VALUES ('Sense', 'Period', TRUE);");

      String mickeyID = lookupEntityID("movies", "name", "'Mickey'");
      String boyID = lookupEntityID("movies", "name", "'Boy'");
      String senseID = lookupEntityID("movies", "name", "'Sense'");

      String hughID = lookupEntityID("actors", "name", "'Hugh'");
      String toniID = lookupEntityID("actors", "name", "'Toni'");
      String jamesID = lookupEntityID("actors", "name", "'James'");
      String emmaID = lookupEntityID("actors", "name", "'Emma'");

      sendCommandAndIgnoreResponse("CREATE TABLE roles (name, movieID, actorID);");
      sendCommandAndIgnoreResponse("INSERT INTO roles VALUES ('Edward', " + senseID + ", " + hughID + ");");
      sendCommandAndIgnoreResponse("INSERT INTO roles VALUES ('Frank', " + mickeyID + ", " + jamesID + ");");
      sendCommandAndIgnoreResponse("INSERT INTO roles VALUES ('Fiona', " + boyID + ", " + toniID + ");");
      sendCommandAndIgnoreResponse("INSERT INTO roles VALUES ('Elinor', " + senseID + ", " + emmaID + ");");

      return randomName;
   }

   String lookupEntityID(String tableName, String attributeName, String attributeValue)
   {
      try {
         String query = "SELECT id FROM " + tableName + " WHERE " + attributeName + " == " + attributeValue + ";";
         String response = safeSendWithTimeout(query,"");
         String[] tokens = response.replace("\n"," ").trim().split(" ");
         return tokens[tokens.length-1];
      } catch(Exception e) {
         System.out.println("Exception encountered:\033[0m\n" + e);
         return "";
      }
   }

   void sendCommandAndIgnoreResponse(String command)
   {
      try {
         String sectionHeader = bar + reportTag + command + "...\n";
         safeSendWithTimeout(command, sectionHeader);
      } catch(Exception e) {
         System.out.println("Exception encountered:\033[0m\n" + e);
      }
   }

   void sendCommandAndCheckResponse(String command, String[] expectedWords, String[] unexpectedWords, String comment)
   {
      String sectionHeader = bar + reportTag + comment + "...\n";
      try {
         String response = safeSendWithTimeout(command, sectionHeader);
         String reportableResponse = reportTag + "  " + String.join(("\n" + reportTag + "  "), response.split("\n")) + "\033[0m";
         for(String word: expectedWords) {
            assertTrue(response.contains(word), sectionHeader + reportTag + "Couldn't find the phrase \"" + word + "\" in response to \"" + command + "\":\033[0m\n" + reportableResponse);
         }
         for(String word: unexpectedWords) {
            assertFalse(response.contains(word), sectionHeader + reportTag + "Found the phrase \"" + word + "\" when we shouldn't have in response to \"" + command + "\":\033[0m\n" + reportableResponse);
         }
      } catch(Exception e) {
         fail(sectionHeader + reportTag + "Exception encountered:\033[0m\n" + e);
      }
   }

   String safeSendWithTimeout(String command, String sectionHeader) {
      return assertTimeoutPreemptively(Duration.ofMillis(5000), () -> {
         return server.handleCommand(command);
      }, sectionHeader + reportTag + "Server took too long to respond (probably stuck in an infinite loop)\033[0m\n");
   }

}
