package edu.uob;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.separator;

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

    public List<String> getColumnNames() {
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

    public void saveToFIle(String storageFolderPath) throws IOException {
        java.io.File file = new java.io.File(storageFolderPath + separator +this.tableName + ".tab");

        BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file));

        String headerLine = String.join("\t", this.columnNames);
        writer.write(headerLine);
        writer.write("\n");
        for (Row row : this.rows) {
            String dataLine = String.join("\t", row.getValues());
            writer.write(dataLine);
            writer.write("\n");
        }
        writer.flush();
        writer.close();
    }
}
