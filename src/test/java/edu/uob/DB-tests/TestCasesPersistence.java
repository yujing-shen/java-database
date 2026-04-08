package edu.uob;

import org.junit.jupiter.api.Test;

class TestCasesPersistence
{

   @Test
   void testPersistentStorage()
   {
      DBServerHarness db = new DBServerHarness();
      String dbname = db.createMovieDatabase();
      db = new DBServerHarness();
      db.sendCommandAndIgnoreResponse("USE " + dbname + ";");
      db.sendCommandAndCheckResponse("SELECT * FROM actors;", new String[]{"Toni","James","Hugh","Emma"}, new String[]{}, "Testing that data persists after a server restart");
   }

   @Test
   void testForUniqueIDs()
   {
      DBServerHarness db = new DBServerHarness();
      String dbname = db.createMovieDatabase();
      String[] previousIDs = new String[3];
      previousIDs[0] = db.lookupEntityID("movies","name","'Mickey'");
      previousIDs[1] = db.lookupEntityID("movies","name","'Boy'");
      previousIDs[2] = db.lookupEntityID("movies","name","'Sense'");
      db.sendCommandAndIgnoreResponse("DELETE FROM movies WHERE name == 'Mickey';");
      db.sendCommandAndIgnoreResponse("DELETE FROM movies WHERE name == 'Sense';");
      // Restart server to make sure that the ID tracking isn't just in memory
      db = new DBServerHarness();
      db.sendCommandAndIgnoreResponse("USE " + dbname + ";");
      db.sendCommandAndIgnoreResponse("INSERT INTO movies VALUES ('Weddings', 'Comedy');");
      db.sendCommandAndCheckResponse("SELECT id FROM movies WHERE name == 'Weddings';", new String[]{"[OK]"}, previousIDs, "Testing that server does not recycle IDs event after deletion of rows and server restart");
   }

}
