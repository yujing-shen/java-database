package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;

class TestCasesRobust
{
   DBServerHarness db;
   String databaseName;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      databaseName = db.createMovieDatabase();
   }

   @Test
   void testEmptyResultsTable()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name == 'Zardoz';", new String[]{"[OK]","name","nationality"}, new String[]{"Toni","James","Emma","Hugh"}, "Testing that a query with no results returns an empty table");
   }

   @Test
   void testIncompatibleTypesComparison()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name > 10;", new String[]{"[OK]","name","nationality","awards"}, new String[]{"Emma","Toni","James","Hugh"}, "Testing that query returns an empty table for incompatible types comparison");
   }

   @Test
   void testMixedTypesSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards > '9';", new String[]{"[","]"}, new String[]{}, "Testing querying with mixed types doesn't cause server to crash");
   }

   @Test
   void testNull()
   {
      db.sendCommandAndIgnoreResponse("ALTER TABLE actors ADD age;");
      db.sendCommandAndIgnoreResponse("UPDATE actors SET age = 45 WHERE name == 'Hugh';");
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE age == NULL;", new String[]{"Toni","James","Emma"}, new String[]{"Hugh"}, "Testing that we can use NULL in queries");
   }

}
