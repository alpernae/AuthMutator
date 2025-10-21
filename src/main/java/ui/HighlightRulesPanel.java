package ui;

import burp.api.montoya.MontoyaApi;
import model.HighlightRule;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.ArrayList;

public class HighlightRulesPanel extends JPanel {
    private final MontoyaApi api;
    private final List<HighlightRule> rules;
    private final HighlightRuleTableModel tableModel;
    private final JTable rulesTable;
    private final Consumer<List<HighlightRule>> onRulesChanged;

    public HighlightRulesPanel(MontoyaApi api,
                               List<HighlightRule> initialRules,
                               Consumer<List<HighlightRule>> onRulesChanged) {
        this.api = api;
        this.rules = new ArrayList<>(initialRules);
        this.tableModel = new HighlightRuleTableModel(rules);
        this.onRulesChanged = onRulesChanged;
        
        setLayout(new BorderLayout());
        
        // Create table
        rulesTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    boolean colorColumn = column == 3 && getValueAt(row, 3) instanceof Color;
                    if (!colorColumn) {
                        c.setBackground(deriveRowBackground(this));
                        c.setForeground(getForeground());
                    }
                }
                return c;
            }
        };
        rulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rulesTable.getColumnModel().getColumn(3).setCellRenderer(new HighlightColorRenderer());
        JScrollPane scrollPane = new JScrollPane(rulesTable);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Control panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
    JButton addButton = new PrimaryButton("Add Highlight Rule");
        addButton.addActionListener(e -> showAddRuleDialog());
        
    JButton editButton = new PrimaryButton("Edit");
        editButton.addActionListener(e -> editSelectedRule());
        
    JButton deleteButton = new PrimaryButton("Delete");
        deleteButton.addActionListener(e -> deleteSelectedRule());
        
    JButton enableButton = new PrimaryButton("Enable/Disable");
        enableButton.addActionListener(e -> toggleSelectedRule());
        
        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);
        panel.add(enableButton);
        
        return panel;
    }

    private void showAddRuleDialog() {
        HighlightRuleDialog dialog = new HighlightRuleDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        
        HighlightRule newRule = dialog.getRule();
        if (newRule != null) {
            rules.add(newRule);
            tableModel.fireTableRowsInserted(rules.size() - 1, rules.size() - 1);
            notifyRulesChanged();
            api.logging().logToOutput("Added highlight rule: " + newRule.getName());
        }
    }

    private void editSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow >= 0) {
            HighlightRule rule = rules.get(selectedRow);
            HighlightRuleDialog dialog = new HighlightRuleDialog((Frame) SwingUtilities.getWindowAncestor(this), rule);
            dialog.setVisible(true);
            
            if (dialog.getRule() != null) {
                tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
                notifyRulesChanged();
                api.logging().logToOutput("Updated highlight rule: " + rule.getName());
            }
        }
    }

    private void deleteSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow >= 0) {
            HighlightRule rule = rules.get(selectedRow);
            rules.remove(selectedRow);
            tableModel.fireTableRowsDeleted(selectedRow, selectedRow);
            notifyRulesChanged();
            api.logging().logToOutput("Deleted highlight rule: " + rule.getName());
        }
    }

    private void toggleSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow >= 0) {
            HighlightRule rule = rules.get(selectedRow);
            rule.setEnabled(!rule.isEnabled());
            tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
            notifyRulesChanged();
            api.logging().logToOutput("Toggled highlight rule: " + rule.getName() + " - Enabled: " + rule.isEnabled());
        }
    }

    public List<HighlightRule> getRules() {
        return new ArrayList<>(rules);
    }

    public void setRules(List<HighlightRule> newRules) {
        rules.clear();
        rules.addAll(newRules);
        tableModel.fireTableDataChanged();
        notifyRulesChanged();
    }

    private void notifyRulesChanged() {
        if (onRulesChanged != null) {
            onRulesChanged.accept(getRules());
        }
    }

    // Inner class for table model
    private static class HighlightRuleTableModel extends AbstractTableModel {
        private final List<HighlightRule> rules;
        private final String[] columnNames = {"Enabled", "Name", "Criteria", "Color"};

        public HighlightRuleTableModel(List<HighlightRule> rules) {
            this.rules = rules;
        }

        @Override
        public int getRowCount() {
            return rules.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            HighlightRule rule = rules.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> rule.isEnabled();
                case 1 -> rule.getName();
                case 2 -> rule.describeCriteria();
                case 3 -> rule.getColor();
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> Boolean.class;
                case 3 -> Color.class;
                default -> String.class;
            };
        }
    }

    private static class HighlightColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column == 3 && value instanceof Color color) {
                setText(String.format("#%08X", color.getRGB()).toUpperCase());
                if (!isSelected) {
                    c.setBackground(color);
                    c.setForeground(color.getRed() + color.getGreen() + color.getBlue() < 382 ? Color.WHITE : Color.BLACK);
                }
            } else if (!isSelected) {
                c.setBackground(deriveRowBackground(table));
                c.setForeground(table.getForeground());
            }
            return c;
        }
    }

    private static Color deriveRowBackground(JComponent component) {
        Color base = component.getBackground();
        if (base == null) {
            base = UIManager.getColor("Table.background");
        }
        if (base == null) {
            base = UIManager.getColor("Panel.background");
        }
        if (base == null) {
            return new Color(0xF5F5F5);
        }

        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        double luminance = ((0.2126 * base.getRed()) + (0.7152 * base.getGreen()) + (0.0722 * base.getBlue())) / 255.0;
        if (luminance < 0.5) {
            hsb[2] = Math.min(1f, hsb[2] + 0.18f);
        } else {
            hsb[2] = Math.max(0f, hsb[2] - 0.08f);
        }

        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }
}
