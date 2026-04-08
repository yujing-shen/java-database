package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesCompoundSelect
{
   DBServerHarness db;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      db.createMovieDatabase();
   }

   @Test
   void testBasicCompoundConditionWithoutBrackets()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards > 5 AND nationality == 'British';", new String[]{"Emma"}, new String[]{"Toni","James","Hugh"}, "Testing a compound condition without brackets");
   }

   @Test
   void testBasicCompoundConditionWithBrackets()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE (awards > 5) AND (nationality == 'British');", new String[]{"Emma"}, new String[]{"Toni","James","Hugh"}, "Testing a compound condition with brackets");
   }

   @Test
   void testComplexCompoundConditionWithBrackets()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE (awards > 5) AND ((nationality == 'British') OR (nationality == 'Australian'));", new String[]{"Emma","Toni"}, new String[]{"James","Hugh"}, "Testing a complex compound condition with brackets");
   }

}
