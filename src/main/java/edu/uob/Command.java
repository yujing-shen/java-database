package edu.uob;

import java.util.List;

/**
 * The standard interface for all database commands.
 */
public interface Command {
    String execute(List<String> tokens, DatabaseEngine engine);
}
