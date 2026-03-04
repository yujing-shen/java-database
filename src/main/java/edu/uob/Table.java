package edu.uob;

import java.util.ArrayList;
import java.util.List;

public class Table {
    private String tableName;
    private List<String> columnNames;
    private List<Row> rows;
    private int nextAvailableId;

    public Table(String tableName) {
        this.tableName = tableName;
        this.columnNames = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.nextAvailableId = 1;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getColumnName() {
        return columnNames;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void addColumnName(String columnName) {
        this.columnNames.add(columnName);
    }

    public void addRow(Row row) {
        this.rows.add(row);
    }

    public int getNextNextId() {
        int idToGive = nextAvailableId;
        nextAvailableId++;
        return idToGive;
    }

    public void updateNextAvailableId(int maxIdInFile) {
        if (maxIdInFile >= this.nextAvailableId) {
            nextAvailableId = maxIdInFile + 1;
        }
    }
}
