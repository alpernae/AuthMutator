package ui;

import model.HighlightCondition;
import model.HighlightRule;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HighlightRuleDialog extends JDialog {
    private final java.util.List<String> availableRoles;
    private JTextField nameField;
    private JComboBox<HighlightRule.LogicalOperator> logicCombo;
    private HighlightConditionTableModel conditionTableModel;
    private JTable conditionTable;
    private JButton editConditionButton;
    private JButton removeConditionButton;
    private JButton colorButton;
    private Color selectedColor;
    private HighlightRule rule;
    private boolean confirmed;

    public HighlightRuleDialog(Frame parent, HighlightRule existingRule, java.util.List<String> availableRoles) {
        super(parent, existingRule == null ? "Add Highlight Rule" : "Edit Highlight Rule", true);
        this.availableRoles = availableRoles != null ? availableRoles : java.util.Collections.emptyList();
        this.rule = existingRule;
        this.selectedColor = existingRule != null ? existingRule.getColor() : Color.YELLOW;
        initializeUI();

        if (existingRule != null) {
            populateFields(existingRule);
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        panel.add(nameField, gbc);

        // Advanced conditions panel
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JPanel advancedPanel = createAdvancedConditionsPanel();
        panel.add(advancedPanel, gbc);

        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.weighty = 0;

        // Color picker
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Highlight Color:"), gbc);
        gbc.gridx = 1;
        colorButton = new JButton("Choose Color");
        colorButton.setBackground(selectedColor);
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Highlight Color", selectedColor);
            if (newColor != null) {
                selectedColor = newColor;
                colorButton.setBackground(selectedColor);
            }
        });
        panel.add(colorButton, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (validateAndSave()) {
                confirmed = true;
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void populateFields(HighlightRule rule) {
        nameField.setText(rule.getName());
        selectedColor = rule.getColor();
        colorButton.setBackground(selectedColor);
        logicCombo.setSelectedItem(rule.getLogicalOperator());
        conditionTableModel.setConditions(rule.getConditions());
        conditionTable.clearSelection();
        updateConditionButtons();
    }

    private boolean validateAndSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (conditionTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Add at least one condition", "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (rule == null) {
            rule = new HighlightRule(
                    name,
                    selectedColor);
        } else {
            rule.setName(name);
            rule.setColor(selectedColor);
        }

        rule.setLogicalOperator((HighlightRule.LogicalOperator) logicCombo.getSelectedItem());
        rule.setConditions(conditionTableModel.getConditions());
        return true;
    }

    public HighlightRule getRule() {
        return confirmed ? rule : null;
    }

    private JPanel createAdvancedConditionsPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createTitledBorder("Advanced Conditions"));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        header.add(new JLabel("Match when:"));
        logicCombo = new JComboBox<>(HighlightRule.LogicalOperator.values());
        logicCombo.setSelectedItem(HighlightRule.LogicalOperator.ALL);
        header.add(logicCombo);
        header.add(new JLabel("(Only applied when at least one condition is defined)"));
        container.add(header, BorderLayout.NORTH);

        conditionTableModel = new HighlightConditionTableModel();
        conditionTable = new JTable(conditionTableModel);
        conditionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(conditionTable);
        scrollPane.setPreferredSize(new Dimension(420, 140));
        container.add(scrollPane, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addCondition());
        editConditionButton = new JButton("Edit");
        editConditionButton.addActionListener(e -> editCondition());
        removeConditionButton = new JButton("Remove");
        removeConditionButton.addActionListener(e -> removeCondition());
        controls.add(addButton);
        controls.add(editConditionButton);
        controls.add(removeConditionButton);
        container.add(controls, BorderLayout.SOUTH);

        conditionTable.getSelectionModel().addListSelectionListener(e -> updateConditionButtons());
        updateConditionButtons();

        return container;
    }

    private void addCondition() {
        HighlightConditionDialog dialog = new HighlightConditionDialog(this, null, availableRoles);
        dialog.setVisible(true);
        HighlightCondition condition = dialog.getCondition();
        if (condition != null) {
            conditionTableModel.addCondition(condition);
            int lastRow = conditionTableModel.getRowCount() - 1;
            if (lastRow >= 0) {
                conditionTable.setRowSelectionInterval(lastRow, lastRow);
                conditionTable.scrollRectToVisible(conditionTable.getCellRect(lastRow, 0, true));
            }
            updateConditionButtons();
        }
    }

    private void editCondition() {
        int row = conditionTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        HighlightCondition existing = conditionTableModel.getConditionAt(row);
        if (existing == null) {
            return;
        }
        HighlightConditionDialog dialog = new HighlightConditionDialog(this, existing, availableRoles);
        dialog.setVisible(true);
        HighlightCondition updated = dialog.getCondition();
        if (updated != null) {
            conditionTableModel.updateCondition(row, updated);
            updateConditionButtons();
        }
    }

    private void removeCondition() {
        int row = conditionTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        conditionTableModel.removeCondition(row);
        int newRow = Math.min(row, conditionTableModel.getRowCount() - 1);
        if (newRow >= 0) {
            conditionTable.setRowSelectionInterval(newRow, newRow);
        } else {
            conditionTable.clearSelection();
        }
        updateConditionButtons();
    }

    private void updateConditionButtons() {
        boolean hasSelection = conditionTable.getSelectedRow() >= 0;
        editConditionButton.setEnabled(hasSelection);
        removeConditionButton.setEnabled(hasSelection);
    }

    private static class HighlightConditionTableModel extends AbstractTableModel {
        private final List<HighlightCondition> conditions = new ArrayList<>();
        private final String[] columnNames = { "Version", "Part", "Relationship", "Value" };

        @Override
        public int getRowCount() {
            return conditions.size();
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
            HighlightCondition condition = conditions.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> condition.getMessageVersion().getLabel();
                case 1 -> condition.getMatchPart().getLabel();
                case 2 -> condition.getRelationship().getLabel();
                case 3 -> condition.getMatchValue();
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public void setConditions(List<HighlightCondition> newConditions) {
            conditions.clear();
            if (newConditions != null) {
                newConditions.stream()
                        .filter(Objects::nonNull)
                        .map(HighlightCondition::copy)
                        .forEach(conditions::add);
            }
            fireTableDataChanged();
        }

        public List<HighlightCondition> getConditions() {
            return conditions.stream().map(HighlightCondition::copy).toList();
        }

        public void addCondition(HighlightCondition condition) {
            conditions.add(condition.copy());
            int index = conditions.size() - 1;
            fireTableRowsInserted(index, index);
        }

        public HighlightCondition getConditionAt(int row) {
            if (row < 0 || row >= conditions.size()) {
                return null;
            }
            return conditions.get(row).copy();
        }

        public void updateCondition(int row, HighlightCondition updated) {
            if (row < 0 || row >= conditions.size()) {
                return;
            }
            conditions.set(row, updated.copy());
            fireTableRowsUpdated(row, row);
        }

        public void removeCondition(int row) {
            if (row < 0 || row >= conditions.size()) {
                return;
            }
            conditions.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }
}
