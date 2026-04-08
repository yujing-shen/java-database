package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesIllegalNames
{
   DBServerHarness db;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      db.createMovieDatabase();
   }

   @Test
   void testPreventingSQLKeywordsAsTableNames()
   {
      db.sendCommandAndCheckResponse("CREATE TABLE SELECT (name, age);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that SQL reserved keywords are prevented as table names");
      db.sendCommandAndCheckResponse("CREATE TABLE insert (name, age);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that SQL reserved keywords are prevented as table names");
   }

   @Test
   void testPreventingSQLKeywordsAsAttributeNames()
   {
      db.sendCommandAndCheckResponse("CREATE TABLE stunts (table, UPDATE);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that SQL reserved keywords are prevented as attribute names");
      db.sendCommandAndCheckResponse("CREATE TABLE crew (table, insert);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that SQL reserved keywords are prevented as attribute names");
   }

   @Test
   void testPreventIDAsColumnName()
   {
      db.sendCommandAndCheckResponse("CREATE TABLE stunts (name, id);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that 'id' can't be used as a column name");
   }

   @Test
   void testDuplicateAttributeName()
   {
      db.sendCommandAndCheckResponse("ALTER TABLE actors ADD name;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that a table can't contain duplicate column names");
      db.sendCommandAndCheckResponse("CREATE TABLE stunts (name, name);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that a table can't contain duplicate column names");
   }

   @Test
   void testDatabaseNameAlreadyTaken()
   {
      db.sendCommandAndIgnoreResponse("CREATE DATABASE existing;");
      db.sendCommandAndCheckResponse("CREATE DATABASE existing;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that trying to create a database with an existing database name results in an [ERROR]");
   }

   @Test
   void testTableNameAlreadyTaken()
   {
      db.sendCommandAndCheckResponse("CREATE TABLE actors (name);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that trying to create a table with an existing table name results in an [ERROR]");
   }

   @Test
   void testIllegalTableName()
   {
      db.sendCommandAndCheckResponse("CREATE TABLE $tunts (name);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that trying to create a table with an illegal character causes an [ERROR]");
   }

   @Test
   void testIllegalColumnName()
   {
      db.sendCommandAndCheckResponse("CREATE TABLE stunts (person.name);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that trying to create a column with an illegal character causes an [ERROR]");
   }

}
