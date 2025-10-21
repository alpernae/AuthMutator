package model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class RequestLogModel extends AbstractTableModel {
    private final List<RequestLogEntry> entries;
    private final String[] columnNames = {"ID", "Method", "URL", "Status", "Modified Status", "Cookies", "Parameters"};
    private boolean showUnauthColumn;
    private int maxEntries = 1000;

    public RequestLogModel() {
        this.entries = new ArrayList<>();
    }

    public void addEntry(RequestLogEntry entry) {
        int row = entries.size();
        entries.add(entry);
        fireTableRowsInserted(row, row);
        enforceLimit();
    }

    public void clearEntries() {
        int size = entries.size();
        if (size > 0) {
            entries.clear();
            fireTableRowsDeleted(0, size - 1);
        }
    }

    public RequestLogEntry getEntry(int row) {
        if (row >= 0 && row < entries.size()) {
            return entries.get(row);
        }
        return null;
    }

    public List<RequestLogEntry> getAllEntries() {
        return new ArrayList<>(entries);
    }

    public void replaceById(int id, RequestLogEntry updated) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId() == id) {
                entries.set(i, updated);
                fireTableRowsUpdated(i, i);
                return;
            }
        }
        // If not found, append
        addEntry(updated);
    }

    public RequestLogEntry findById(int id) {
        for (RequestLogEntry entry : entries) {
            if (entry.getId() == id) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return entries.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length + (showUnauthColumn ? 1 : 0);
    }

    @Override
    public String getColumnName(int column) {
        if (column < columnNames.length) {
            return columnNames[column];
        }
        return "Unauth";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RequestLogEntry entry = entries.get(rowIndex);
        if (columnIndex < columnNames.length) {
            return switch (columnIndex) {
                case 0 -> entry.getId();
                case 1 -> entry.getMethod();
                case 2 -> entry.getUrl();
                case 3 -> entry.getOriginalStatusCode() != null ? entry.getOriginalStatusCode() : entry.getStatusCode();
                case 4 -> entry.getModifiedStatusCode();
                case 5 -> entry.getCookieSummary();
                case 6 -> entry.getParameterSummary();
                default -> null;
            };
        }
        return entry.isUnauthenticatedTesting();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex < columnNames.length) {
            return switch (columnIndex) {
                case 0, 3, 4 -> Integer.class;
                default -> String.class;
            };
        }
        return Boolean.class;
    }

    public void setMaxEntries(int maxEntries) {
        int normalized = Math.max(100, maxEntries);
        if (this.maxEntries == normalized) {
            return;
        }
        this.maxEntries = normalized;
        enforceLimit();
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setShowUnauthColumn(boolean show) {
        if (this.showUnauthColumn == show) {
            return;
        }
        this.showUnauthColumn = show;
        fireTableStructureChanged();
    }

    public boolean isShowUnauthColumn() {
        return showUnauthColumn;
    }

    private void enforceLimit() {
        while (entries.size() > maxEntries) {
            entries.remove(0);
            fireTableRowsDeleted(0, 0);
        }
    }
}
