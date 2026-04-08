package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesUpdate
{
   DBServerHarness db;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      db.createMovieDatabase();
   }

   @Test
   void testRowUpdate()
   {
      db.sendCommandAndIgnoreResponse("UPDATE actors SET awards = 11 WHERE name == 'Emma';");
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name == 'Emma';", new String[]{"11"}, new String[]{"10"}, "Testing that Emma's awards were successfully updated to 11");
   }

   @Test
   void testAddingColumnToTable()
   {
      db.sendCommandAndIgnoreResponse("ALTER TABLE actors ADD age;");
      db.sendCommandAndCheckResponse("SELECT * FROM actors;", new String[]{"age"}, new String[]{}, "Testing altering table to add a new column called 'age'");
   }

   @Test
   void testInsertingIntoAlteredTable()
   {
      db.sendCommandAndIgnoreResponse("ALTER TABLE actors ADD age;");
      db.sendCommandAndIgnoreResponse("UPDATE actors SET age = 45 WHERE name == 'Hugh';");
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name == 'Hugh';", new String[]{"45"}, new String[]{}, "Testing updating an entry in a table after adding a new column and setting the age of Hugh to be 45");
   }

}
