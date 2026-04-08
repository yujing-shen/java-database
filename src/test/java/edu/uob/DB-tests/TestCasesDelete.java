package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesDelete
{
   DBServerHarness db;
   String dbName;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      dbName = db.createMovieDatabase();
   }

   @Test
   void testStringConditionalDelete()
   {
      db.sendCommandAndIgnoreResponse("DELETE FROM actors WHERE name == 'Hugh';");
      db.sendCommandAndCheckResponse("SELECT * FROM actors;", new String[]{"Toni","James","Emma"}, new String[]{"Hugh"}, "Testing to make sure that Hugh was deleted from the table");
   }

   @Test
   void testIntegerConditionalDelete()
   {
      db.sendCommandAndIgnoreResponse("DELETE FROM actors WHERE awards < 10;");
      db.sendCommandAndCheckResponse("SELECT * FROM actors;", new String[]{"Toni","Emma"}, new String[]{"Hugh","James"}, "Testing to make sure we successfully deleting anyone who has less that 10 awards");
   }

   @Test
   void testDroppingTable()
   {
      db.sendCommandAndIgnoreResponse("DROP TABLE actors;");
      db.sendCommandAndCheckResponse("SELECT * FROM actors;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that dropped table can no longer be queried");
   }

   @Test
   void testDroppingDatabase()
   {
      db.sendCommandAndIgnoreResponse("DROP DATABASE " + dbName + ";");
      db.sendCommandAndCheckResponse("USE " + dbName + ";", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that dropped databases can no longer be used");
   }

   @Test
   void testDroppingColumnFromTable()
   {
      db.sendCommandAndIgnoreResponse("ALTER TABLE actors DROP awards;");
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name == 'Hugh';", new String[]{"British"}, new String[]{"3","awards"}, "Testing altering table by dropping dropping the 'awards' column");
   }


}
