package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesWhitespace
{
   DBServerHarness db;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      db.createMovieDatabase();
   }

   @Test
   void testWhitespaceAtStart()
   {
      db.sendCommandAndCheckResponse("      SELECT * FROM actors WHERE awards > 10;", new String[]{"Toni"}, new String[]{}, "Testing server can cope with additional whitespace");
   }

   @Test
   void testWhitespaceAtEnd()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards > 10 ; ", new String[]{"Toni"}, new String[]{}, "Testing server can cope with additional whitespace");
   }

   @Test
   void testWhitespaceInMiddle()
   {
      db.sendCommandAndCheckResponse("SELECT  *  FROM   actors   WHERE   awards   >10;", new String[]{"Toni"}, new String[]{}, "Testing server can cope with additional whitespace");
   }

   @Test
   void testReducedWhitespaceSelect()
   {
      // No spaces between tokens
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name=='Hugh';", new String[]{"[OK]","Hugh"}, new String[]{"[ERROR]","Toni","Emma"}, "Testing that the server can cope with minimal whitespace");
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards>5;", new String[]{"[OK]","Toni","Emma"}, new String[]{"[ERROR]","Hugh"}, "Testing that the server can cope with minimal whitespace");
   }

   @Test
   void testReducedWhitespaceUpdate()
   {
      db.sendCommandAndCheckResponse("UPDATE actors SET awards=10 WHERE name=='Hugh';", new String[]{"[OK]"}, new String[]{"[ERROR]"}, "Testing that the server can cope with minimal whitespace");
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards==10;", new String[]{"[OK]","Hugh"}, new String[]{"[ERROR]"}, "Testing that the server can cope with minimal whitespace");
   }

}
