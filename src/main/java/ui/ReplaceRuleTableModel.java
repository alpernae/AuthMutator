package ui;

import model.ReplaceRule;

import javax.swing.table.AbstractTableModel;
import java.util.List;

class ReplaceRuleTableModel extends AbstractTableModel {
    private final List<ReplaceRule> rules;
    private static final String[] COLUMNS = {"Enabled", "Name", "Operations"};

    ReplaceRuleTableModel(List<ReplaceRule> rules) {
        this.rules = rules;
    }

    @Override
    public int getRowCount() {
        return rules.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ReplaceRule rule = rules.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> rule.isEnabled();
            case 1 -> rule.getName();
            case 2 -> rule.describeOperations();
            default -> null;
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
