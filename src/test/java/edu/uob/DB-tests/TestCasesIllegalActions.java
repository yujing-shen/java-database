package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesIllegalActions
{
   DBServerHarness db;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      db.createMovieDatabase();
   }

   @Test
   void testTooManyValues()
   {
      db.sendCommandAndCheckResponse("INSERT INTO actors VALUES ('Stephen Fry', 'British', 10, TRUE);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing trying to insert too many values into a table");
   }

   @Test
   void testTooFewValues()
   {
      db.sendCommandAndCheckResponse("INSERT INTO actors VALUES ('Stephen Fry', 'British');", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing trying to insert too few values into a table");
   }

   @Test
   void testPreventingRemovalOfIdColumn()
   {
      db.sendCommandAndCheckResponse("ALTER TABLE actors DROP id", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that the server returns an error on attempt to remove ID column");
      db.sendCommandAndCheckResponse("SELECT * FROM actors;", new String[]{"id"}, new String[]{}, "Testing that the server prevents removal of ID column");
   }

   @Test
   void testPreventingUpdateOfIdColumn()
   {
      db.sendCommandAndCheckResponse("UPDATE actors SET id=999 WHERE name=='Hugh';", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that the server returns an error on attempt to change record IDs");
      db.sendCommandAndCheckResponse("SELECT * FROM actors;", new String[]{}, new String[]{"999"}, "Testing that the server prevents changing of record IDs (which we just tried to change to 999)");
   }

}
