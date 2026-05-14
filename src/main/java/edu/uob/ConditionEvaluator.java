package edu.uob;

import java.util.List;

/**
 * Parses and evaluates SQL WHERE clause conditions (AST Evaluator)
 */
public class ConditionEvaluator {
    public boolean evaluate(Row row, Table table, List<String> condTokens) {
        if (condTokens == null || condTokens.isEmpty()) {
            throw new RuntimeException("[ERROR] Empty condition provided.");
        }

        int bracketDepth = 0;
        int mainOpIndex = -1;
        String mainOp = "";

        // Locate the top-level logical operator (AND/ OR) outside of any brackets
        for (int i = 0; i < condTokens.size(); i++) {
            String token = condTokens.get(i);
            if (token.equals("(")) {
                bracketDepth++;
            } else if (token.equals(")")) {
                bracketDepth--;
            } else if (bracketDepth == 0 &&
                    (token.equalsIgnoreCase("AND") || token.equalsIgnoreCase("OR"))) {
                mainOpIndex = i;
                mainOp = token.toUpperCase();
                break;
            }
        }

        // Recursive splitting if a top-level operator is found
        if (mainOpIndex != -1) {
            List<String> leftTokens = condTokens.subList(0, mainOpIndex);
            List<String> rightTokens = condTokens.subList(mainOpIndex + 1, condTokens.size());

            boolean leftResult = evaluate(row, table, leftTokens);
            boolean rightResult = evaluate(row, table, rightTokens);

            return mainOp.equals("AND") ? (leftResult && rightResult) : (leftResult || rightResult);
        }

        // Strip wrapping parentheses if the whole expression is enclosed
        if (condTokens.get(0).equals("(") && condTokens.get(condTokens.size() - 1).equals(")")) {
            return evaluate(row, table, condTokens.subList(1, condTokens.size()-1));
        }

        // Base Case: Evaluate simple condition triplet (Column, Operator, Value)
        if (condTokens.size() == 3) {
            String col =  condTokens.get(0);
            String op = condTokens.get(1);
            String val = condTokens.get(2); // String iteral cleaning is delegated to checkCondtion
            return checkCondition(row, table, col, op, val);
        }

        throw new RuntimeException("[ERROR] Invalid condition syntax: " + String.join(" ", condTokens));
    }

    /**
     * Evaluates a base condition (e.g., age > 20) against a specific row.
     * Utilizes encapsulated Table and Row methods to prevent tight coupling.
     *
     * @param row The current row being checked.
     * @param table The table object for schema reference.
     * @param columnName The column to check against.
     * @param operator The comparative operator (==, !=, > LIKE, etc.)
     * @param targetValue The value to compare with.
     * @return true if the condition holds, false otherwise,
     */
    private boolean checkCondition(Row row, Table table, String columnName, String operator, String targetValue) {
        // Delegate index resolution to the Table class (Decoupling)
        int colIndex = table.getColumnIndexOrThrow(columnName);

        // Delegate data extraction to the Row class, but clean the target here
        String cellValue = row.getCleanValueAt(colIndex);
        String cleanTarget = targetValue.replace("'","").trim();

        switch (operator.toUpperCase()) {
            case "==":
                if (cleanTarget.equalsIgnoreCase("TRUE") || cleanTarget.equalsIgnoreCase("FALSE")) {
                    return cellValue.equalsIgnoreCase(cleanTarget);
                }
                return cellValue.equals(cleanTarget);
            case "!=":
                return !cellValue.equals(cleanTarget);
            case "LIKE":
                return cellValue.contains(cleanTarget);
            case ">":
            case ">=":
            case "<":
            case "<=":
                try {
                    float cellNum = Float.parseFloat(cellValue);
                    float targetNum = Float.parseFloat(targetValue);
                    switch (operator) {
                        case ">": return cellNum > targetNum;
                        case "<": return cellNum < targetNum;
                        case "<=": return cellNum <= targetNum;
                        case ">=": return cellNum >= targetNum;
                        default: return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                throw new RuntimeException("[ERROR] Unknown operator: " + operator);
        }
    }

}
