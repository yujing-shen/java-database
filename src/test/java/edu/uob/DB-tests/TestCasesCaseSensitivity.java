package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesCaseSensitivity
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
   void testLowerCaseBoolean()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM movies WHERE UK == true;", new String[]{"Boy","Sense"}, new String[]{"Mickey"}, "Testing lower case booleans");
      db.sendCommandAndCheckResponse("SELECT * FROM movies WHERE UK == False;", new String[]{"Mickey"}, new String[]{"Sense","Boy"}, "Testing mixed case booleans");
   }

   @Test
   void testCaseInsensitiveDatabaseName()
   {
      db.sendCommandAndCheckResponse("USE NonExistantDatabase;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that the server doesn't just reply with [OK] for any and all DB names");
      db.sendCommandAndCheckResponse("USE " + dbName.toLowerCase() + ";", new String[]{"[OK]"}, new String[]{"[ERROR]"}, "Testing that the server can cope with case insensitive DB names");
      db.sendCommandAndCheckResponse("USE " + dbName.toUpperCase() + ";", new String[]{"[OK]"}, new String[]{"[ERROR]"}, "Testing that the server can cope with case insensitive DB names");
   }

   @Test
   void testCaseInsensitiveTableName()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM AcToRs WHERE name == 'Emma';", new String[]{"[OK]","Emma"}, new String[]{"[ERROR]"}, "Testing that the server can cope with case insensitive table names");
   }

   @Test
   void testCaseInsensitiveSelectCommand()
   {
      db.sendCommandAndCheckResponse("select * from actors where name == 'Emma';", new String[]{"[OK]","Emma"}, new String[]{"[ERROR]"}, "Testing that the server can cope with lower case SELECT queries");
   }

   @Test
   void testCaseInsensitiveInsertCommand()
   {
      db.sendCommandAndCheckResponse("insert into actors values ('Stephen', 'British', 14);", new String[]{"[OK]"}, new String[]{"[ERROR]"}, "Testing that the server can cope with lower case INSERT queries");
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name == 'Stephen';", new String[]{"[OK]","Stephen","British"}, new String[]{"[ERROR]"}, "Testing that the server can cope with lower case INSERT queries");
   }

   @Test
   void testCaseInsensitiveLikeCommand()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name Like 'am';", new String[]{"James"}, new String[]{"Toni","Emma","Hugh"}, "Testing that the server can cope with case insensitive LIKE queries");
   }

}
