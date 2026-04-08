package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesJoin
{
   DBServerHarness db;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      db.createMovieDatabase();
   }

   @Test
   void testBasicJoinQuery()
   {
      // First delete everyone apart from Toni (so we can make sure the join actually works)
      db.sendCommandAndIgnoreResponse("DELETE FROM actors WHERE name == 'Hugh';");
      db.sendCommandAndIgnoreResponse("DELETE FROM actors WHERE name == 'James';");
      db.sendCommandAndIgnoreResponse("DELETE FROM actors WHERE name == 'Emma';");
      db.sendCommandAndCheckResponse("JOIN actors AND roles ON id AND actorID;", new String[]{"Toni","Fiona"}, new String[]{"Edward","Frank","Elinor"}, "Testing the JOIN query on a sparsely populated actor table");
   }

   @Test
   void testComplexJoinQuery()
   {
      db.sendCommandAndCheckResponse("JOIN movies AND roles ON id AND movieID;", new String[]{"Sense","Edward","Elinor"}, new String[]{}, "Testing the JOIN query on full populated movie and roles tables");
   }

   @Test
   void testJoinedAttributeNames()
   {
      db.sendCommandAndCheckResponse("JOIN movies AND roles ON id AND movieID;", new String[]{"movies.genre","roles.actorID"}, new String[]{}, "Testing the creation of attribute names in a JOINed table");
   }

   @Test
   void testRemovedOldIDsOnJoin()
   {
      db.sendCommandAndCheckResponse("JOIN movies AND roles ON id AND movieID;", new String[]{}, new String[]{"movies.id","roles.id"}, "Testing the removal of old IDs in JOINed table");
   }

}
