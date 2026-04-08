package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesMalformed
{
   DBServerHarness db;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      db.createMovieDatabase();
   }

   @Test
   void testMissingSemiColon()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that missing semi-colon is detected");
   }

   @Test
   void testExtraBracket()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that imbalanced brackets are detected");
   }

   @Test
   void testIncorrectQueryCommand()
   {
      db.sendCommandAndCheckResponse("SELECTION * FROM actors);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that 'SELECTION' is not accepted as a valid query");
   }

   @Test
   void testInvalidComparitors()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name === 'Hugh';", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that === is not accepted as a valid comparitor");
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards >> 10;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that >> is not accepted as a valid comparitor");
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards > = 10;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that > = is not accepted as a valid comparitor");
   }

   @Test
   void testMissingQuote()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name == 'Hugh;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that missing quotes are detected");
   }

   @Test
   void testMissingCommaInSelect()
   {
      db.sendCommandAndCheckResponse("SELECT name age FROM actors;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that missing comma is detected");
   }

   @Test
   void testMissingWhere()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors awards > 10;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that missing WHERE is detected");
   }

   @Test
   void testBlankQuery()
   {
      db.sendCommandAndCheckResponse("", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that a blank query results in an error");
   }

   @Test
   void testJustSemiColon()
   {
      db.sendCommandAndCheckResponse(";", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that an empty query results in an error");
   }

   @Test
   void testMissingCommaInCreate()
   {
      db.sendCommandAndCheckResponse("CREATE TABLE crew (name job);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that a missing comma is detected");
   }

   @Test
   void testMissingCloseBracket()
   {
      db.sendCommandAndCheckResponse("CREATE TABLE crew (name, job;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that a missing closing bracket is detected");
   }

}
