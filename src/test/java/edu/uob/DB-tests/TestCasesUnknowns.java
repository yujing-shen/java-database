package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesUnknowns
{
   DBServerHarness db;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      db.createMovieDatabase();
   }

   @Test
   void testUnknownTable()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM crew;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that an unknown table is detected");
   }

   @Test
   void testUnknownColumn()
   {
      db.sendCommandAndCheckResponse("SELECT spouse FROM actors;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that the attempted use of the unknown attribute 'spouse' is detected");
   }

   @Test
   void testUnknownDatabase()
   {
      db.sendCommandAndCheckResponse("USE ebay;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that server detects attempted use of unknown database");
   }

}
