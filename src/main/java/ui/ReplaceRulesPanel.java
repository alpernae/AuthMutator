package ui;

import burp.api.montoya.MontoyaApi;
import model.ReplaceRule;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ReplaceRulesPanel extends JPanel {
    private final MontoyaApi api;
    private final List<ReplaceRule> rules;
    private final ReplaceRuleTableModel tableModel;
    private final JTable rulesTable;
    private final Consumer<List<ReplaceRule>> onRulesChanged;
    private final Consumer<java.io.File> importHandler;
    private final Consumer<java.io.File> exportHandler;

    public ReplaceRulesPanel(MontoyaApi api,
                             List<ReplaceRule> initialRules,
                             Consumer<List<ReplaceRule>> onRulesChanged,
                             Consumer<java.io.File> importHandler,
                             Consumer<java.io.File> exportHandler) {
        this.api = api;
        this.rules = new ArrayList<>(initialRules);
        this.tableModel = new ReplaceRuleTableModel(rules);
        this.onRulesChanged = onRulesChanged;
        this.importHandler = importHandler;
        this.exportHandler = exportHandler;
        
        setLayout(new BorderLayout());

        // Create table
        rulesTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(deriveRowBackground(this));
                    c.setForeground(getForeground());
                }
                return c;
            }
        };
        rulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(rulesTable);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Control panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addButton = new PrimaryButton("Add Rule");
        addButton.addActionListener(e -> showAddRuleDialog());

        JButton editButton = new PrimaryButton("Edit");
        editButton.addActionListener(e -> editSelectedRule());

        JButton deleteButton = new PrimaryButton("Delete");
        deleteButton.addActionListener(e -> deleteSelectedRule());

        JButton enableButton = new PrimaryButton("Enable/Disable");
        enableButton.addActionListener(e -> toggleSelectedRule());

        JButton importButton = new PrimaryButton("Import JSON");
        importButton.addActionListener(e -> importRules());

        JButton exportButton = new PrimaryButton("Export JSON");
        exportButton.addActionListener(e -> exportRules());

        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);
        panel.add(enableButton);
        panel.add(importButton);
        panel.add(exportButton);

        return panel;
    }

    private void importRules() {
        if (importHandler == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();
            importHandler.accept(file);
        }
    }

    private void exportRules() {
        if (exportHandler == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();
            exportHandler.accept(file);
        }
    }

    private void showAddRuleDialog() {
        ReplaceRuleDialog dialog = new ReplaceRuleDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        
        ReplaceRule newRule = dialog.getRule();
        if (newRule != null) {
            rules.add(newRule);
            tableModel.fireTableRowsInserted(rules.size() - 1, rules.size() - 1);
            notifyRulesChanged();
            api.logging().logToOutput("Added rule: " + newRule.getName());
        }
    }

    private void editSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow >= 0) {
            ReplaceRule rule = rules.get(selectedRow);
            ReplaceRuleDialog dialog = new ReplaceRuleDialog((Frame) SwingUtilities.getWindowAncestor(this), rule);
            dialog.setVisible(true);
            
            if (dialog.getRule() != null) {
                tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
                notifyRulesChanged();
                api.logging().logToOutput("Updated rule: " + rule.getName());
            }
        }
    }

    private void deleteSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow >= 0) {
            ReplaceRule rule = rules.get(selectedRow);
            rules.remove(selectedRow);
            tableModel.fireTableRowsDeleted(selectedRow, selectedRow);
            notifyRulesChanged();
            api.logging().logToOutput("Deleted rule: " + rule.getName());
        }
    }

    private void toggleSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow >= 0) {
            ReplaceRule rule = rules.get(selectedRow);
            rule.setEnabled(!rule.isEnabled());
            tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
            notifyRulesChanged();
            api.logging().logToOutput("Toggled rule: " + rule.getName() + " - Enabled: " + rule.isEnabled());
        }
    }

    public List<ReplaceRule> getRules() {
        return new ArrayList<>(rules);
    }

    public void setRules(List<ReplaceRule> newRules) {
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

    private static Color deriveRowBackground(JTable table) {
        Color base = table.getBackground();
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
