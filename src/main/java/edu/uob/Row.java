package edu.uob;

import java.util.ArrayList;
import java.util.List;

public class Row {
    private List<String> values;

    public Row() {
        this.values = new ArrayList<>();
    }

    public void addValue(String value) {
        this.values.add(value);
    }

    public List<String> getValues() {
        return this.values;
    }

    public String getValueAt(int index) {
        if (index >= 0 && index < this.values.size()) {
            return this.values.get(index);
        }
        return "";
    }

    public void setValueAt(int index, String newValue) {
        if  (index >= 0 && index < this.values.size()) {
            this.values.set(index, newValue);
        }
    }
}
