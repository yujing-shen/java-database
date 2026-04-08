package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesBasicSelect
{
   DBServerHarness db;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      db.createMovieDatabase();
   }

   @Test
   void testBasicWildstarSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors;", new String[]{"Toni","Emma","James","Hugh"}, new String[]{}, "Testing basic SELECT * query");
   }

   @Test
   void testStringEqualsConditionalSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name == 'Toni';", new String[]{"Toni","Australian"}, new String[]{"Emma","James","Hugh"}, "Testing string equals conditional SELECT query");
   }

   @Test
   void testIntegerEqualsConditionalSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards == 10;", new String[]{"Emma","10"}, new String[]{"Toni","James","Hugh"}, "Testing integer equals conditional SELECT query");
   }

   @Test
   void testBooleanConditionalSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM movies WHERE UK == TRUE;", new String[]{"Boy","Sense"}, new String[]{"Mickey"}, "Testing boolean conditional SELECT query");
   }

   @Test
   void testStringNotEqualConditionalSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name != 'James';", new String[]{"Emma","Toni","Hugh"}, new String[]{"James"}, "Testing string not equals conditional SELECT query");
   }

   @Test
   void testIntegerComparisons()
   {
      // Check for "12 < 5" error (due to alphabetical rather than numberical comparison)
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards < 5;", new String[]{"Hugh"}, new String[]{"Toni","James","Emma"}, "Testing integer comparisons use numerical rather than character comparisons");
   }

   @Test
   void testSelectionOfSpecifiedColumns()
   {
      db.sendCommandAndCheckResponse("SELECT nationality FROM actors WHERE name == 'Hugh';", new String[]{"British"}, new String[]{"awards"}, "Testing selecting some specific columns in SELECT query");
   }

   @Test
   void testLikeQuery()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name LIKE 'am';", new String[]{"James"}, new String[]{"Toni","Emma","Hugh"}, "Testing the LIKE query");
   }

}
