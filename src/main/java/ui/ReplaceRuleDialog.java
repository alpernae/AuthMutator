package ui;

import model.ReplaceRule;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ReplaceRuleDialog extends JDialog {
    private ReplaceRule rule;
    private JTextField nameField;
    private ReplaceOperationTableModel operationTableModel;
    private JTable operationTable;
    private JButton editButton;
    private JButton removeButton;
    private boolean confirmed;

    public ReplaceRuleDialog(Frame owner, ReplaceRule existingRule) {
        super(owner, existingRule == null ? "Add Replace Rule" : "Edit Replace Rule", true);
        this.rule = existingRule;
        initializeUI();
        if (existingRule != null) {
            populateFields(existingRule);
        }
        pack();
        setLocationRelativeTo(owner);
    }

    private void initializeUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Rule Name:"), gbc);

        gbc.gridx = 1;
        nameField = new JTextField(30);
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
    gbc.weighty = 1.0;
        JPanel operationsPanel = createOperationsPanel();
        panel.add(operationsPanel, gbc);

    gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.weighty = 0;
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
        panel.add(buttonPanel, gbc);

    add(panel);
    }

    private JPanel createOperationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Replacement Operations"));

        operationTableModel = new ReplaceOperationTableModel();
        operationTable = new JTable(operationTableModel);
        operationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        operationTable.getSelectionModel().addListSelectionListener(e -> updateButtons());
        panel.add(new JScrollPane(operationTable), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addOperation());
        editButton = new JButton("Edit");
        editButton.addActionListener(e -> editOperation());
        removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeOperation());
        controls.add(addButton);
        controls.add(editButton);
        controls.add(removeButton);
        panel.add(controls, BorderLayout.SOUTH);

        updateButtons();
        return panel;
    }

    private void populateFields(ReplaceRule existing) {
        nameField.setText(existing.getName());
        operationTableModel.setOperations(existing.getOperations());
        updateButtons();
    }

    private boolean validateAndSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (operationTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Add at least one replacement operation", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        List<ReplaceRule.ReplaceOperation> operations = operationTableModel.getOperations();
        if (rule == null) {
            rule = new ReplaceRule(name);
        } else {
            rule.setName(name);
        }
        rule.setOperations(operations);
        return true;
    }

    public ReplaceRule getRule() {
        return confirmed ? rule : null;
    }

    private void addOperation() {
        ReplaceOperationEditor dialog = new ReplaceOperationEditor(this, null);
        dialog.setVisible(true);
        ReplaceRule.ReplaceOperation operation = dialog.getOperation();
        if (operation != null) {
            operationTableModel.addOperation(operation);
            int lastRow = operationTableModel.getRowCount() - 1;
            if (lastRow >= 0) {
                operationTable.setRowSelectionInterval(lastRow, lastRow);
                operationTable.scrollRectToVisible(operationTable.getCellRect(lastRow, 0, true));
            }
            updateButtons();
        }
    }

    private void editOperation() {
        int selectedRow = operationTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        ReplaceRule.ReplaceOperation existing = operationTableModel.getOperationAt(selectedRow);
        if (existing == null) {
            return;
        }
        ReplaceOperationEditor dialog = new ReplaceOperationEditor(this, existing);
        dialog.setVisible(true);
        ReplaceRule.ReplaceOperation updated = dialog.getOperation();
        if (updated != null) {
            operationTableModel.updateOperation(selectedRow, updated);
            updateButtons();
        }
    }

    private void removeOperation() {
        int selectedRow = operationTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        operationTableModel.removeOperation(selectedRow);
        int newRow = Math.min(selectedRow, operationTableModel.getRowCount() - 1);
        if (newRow >= 0) {
            operationTable.setRowSelectionInterval(newRow, newRow);
        } else {
            operationTable.clearSelection();
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean hasSelection = operationTable != null && operationTable.getSelectedRow() >= 0;
        if (editButton != null) {
            editButton.setEnabled(hasSelection);
        }
        if (removeButton != null) {
            removeButton.setEnabled(hasSelection);
        }
    }

    private static class ReplaceOperationTableModel extends AbstractTableModel {
    private final List<ReplaceRule.ReplaceOperation> operations = new ArrayList<>();
    private final String[] columns = {"Type", "Match Pattern", "Replace Value", "Regex"};

        @Override
        public int getRowCount() {
            return operations.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ReplaceRule.ReplaceOperation operation = operations.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> operation.getType().getLabel();
                case 1 -> operation.getMatchPattern();
                case 2 -> operation.getType().supportsReplace() ? operation.getReplaceValue() : "";
                case 3 -> operation.isUseRegex();
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 3 ? Boolean.class : String.class;
        }

        public void setOperations(List<ReplaceRule.ReplaceOperation> newOperations) {
            operations.clear();
            if (newOperations != null) {
                newOperations.stream()
                        .map(ReplaceRule.ReplaceOperation::copy)
                        .forEach(operations::add);
            }
            fireTableDataChanged();
        }

        public void addOperation(ReplaceRule.ReplaceOperation operation) {
            operations.add(operation.copy());
            int index = operations.size() - 1;
            fireTableRowsInserted(index, index);
        }

        public ReplaceRule.ReplaceOperation getOperationAt(int row) {
            if (row < 0 || row >= operations.size()) {
                return null;
            }
            return operations.get(row).copy();
        }

        public void updateOperation(int row, ReplaceRule.ReplaceOperation updated) {
            if (row < 0 || row >= operations.size()) {
                return;
            }
            operations.set(row, updated.copy());
            fireTableRowsUpdated(row, row);
        }

        public void removeOperation(int row) {
            if (row < 0 || row >= operations.size()) {
                return;
            }
            operations.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public List<ReplaceRule.ReplaceOperation> getOperations() {
            return operations.stream().map(ReplaceRule.ReplaceOperation::copy).toList();
        }
    }

    private static class ReplaceOperationEditor extends JDialog {
        private ReplaceRule.ReplaceOperation operation;
        private JComboBox<ReplaceRule.OperationType> typeCombo;
        private JLabel matchLabel;
        private JTextField matchField;
        private JLabel replaceLabel;
        private JTextField replaceField;
        private JCheckBox regexCheckbox;
        private JLabel hintLabel;
        private boolean confirmed;

        ReplaceOperationEditor(Window owner, ReplaceRule.ReplaceOperation existing) {
            super(owner, existing == null ? "Add Operation" : "Edit Operation", ModalityType.APPLICATION_MODAL);
            this.operation = existing;
            initializeUI();
            if (existing != null) {
                populate(existing);
            }
            updateFieldState();
            pack();
            setLocationRelativeTo(owner);
        }

        private void initializeUI() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Operation Type:"), gbc);
            gbc.gridx = 1;
            typeCombo = new JComboBox<>(ReplaceRule.OperationType.values());
            typeCombo.addActionListener(e -> updateFieldState());
            panel.add(typeCombo, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            matchLabel = new JLabel("Match Pattern:");
            panel.add(matchLabel, gbc);
            gbc.gridx = 1;
            matchField = new JTextField(25);
            panel.add(matchField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            replaceLabel = new JLabel("Replace Value:");
            panel.add(replaceLabel, gbc);
            gbc.gridx = 1;
            replaceField = new JTextField(25);
            panel.add(replaceField, gbc);

            gbc.gridx = 1;
            gbc.gridy = 3;
            regexCheckbox = new JCheckBox("Use Regular Expression");
            panel.add(regexCheckbox, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.gridwidth = 2;
            hintLabel = new JLabel(" ");
            hintLabel.setFont(hintLabel.getFont().deriveFont(Font.ITALIC, hintLabel.getFont().getSize() - 1f));
            panel.add(hintLabel, gbc);

            gbc.gridy = 5;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = new JButton("OK");
            okButton.addActionListener(e -> {
                if (validateAndSave()) {
                    confirmed = true;
                    dispose();
                }
            });
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> dispose());
            buttons.add(okButton);
            buttons.add(cancelButton);
            panel.add(buttons, gbc);

            add(panel);
        }

        private ReplaceRule.OperationType selectedType() {
            ReplaceRule.OperationType type = (ReplaceRule.OperationType) typeCombo.getSelectedItem();
            return type != null ? type : ReplaceRule.OperationType.REQUEST_STRING;
        }

        private void updateFieldState() {
            ReplaceRule.OperationType type = selectedType();

            boolean supportsMatch = type.supportsMatch();
            boolean requiresMatch = type.requiresMatch();
            matchLabel.setEnabled(supportsMatch);
            matchField.setEnabled(supportsMatch);
            if (!supportsMatch) {
                matchField.setText("");
            }

            boolean supportsReplace = type.supportsReplace();
            boolean requiresReplace = type.requiresReplace();
            replaceLabel.setEnabled(supportsReplace);
            replaceField.setEnabled(supportsReplace);
            if (!supportsReplace) {
                replaceField.setText("");
            }

            regexCheckbox.setEnabled(type.supportsRegex());
            if (!type.supportsRegex()) {
                regexCheckbox.setSelected(false);
            }

            if (supportsMatch) {
                matchField.setToolTipText(requiresMatch && !type.allowsBlankMatch()
                        ? "Match pattern is required"
                        : "Match pattern (optional)"
                );
            } else {
                matchField.setToolTipText(null);
            }

            if (supportsReplace) {
                replaceField.setToolTipText(requiresReplace ? "Replacement value is required" : "Replacement value (optional)");
            } else {
                replaceField.setToolTipText(null);
            }

            hintLabel.setText(switch (type) {
                case REQUEST_HEADER -> "Leave Match empty to add a header using 'Header-Name: value' in Replace.";
                case REMOVE_PARAMETER_BY_NAME, REMOVE_PARAMETER_BY_VALUE,
                        REMOVE_COOKIE_BY_NAME, REMOVE_COOKIE_BY_VALUE,
                        REMOVE_HEADER_BY_NAME, REMOVE_HEADER_BY_VALUE -> "Removes items that match the pattern.";
                case MATCH_PARAM_NAME_REPLACE_VALUE -> "Matches parameter names and sets their value to Replace.";
                case MATCH_COOKIE_NAME_REPLACE_VALUE -> "Matches cookie names and sets their value to Replace.";
                case MATCH_HEADER_NAME_REPLACE_VALUE -> "Matches header names and sets their value to Replace.";
                default -> "";
            });
        }

        private void populate(ReplaceRule.ReplaceOperation existing) {
            typeCombo.setSelectedItem(existing.getType());
            updateFieldState();

            ReplaceRule.OperationType type = existing.getType();
            if (type.supportsMatch()) {
                matchField.setText(existing.getMatchPattern());
            }
            if (type.supportsReplace()) {
                replaceField.setText(existing.getReplaceValue());
            }
            regexCheckbox.setSelected(type.supportsRegex() && existing.isUseRegex());
        }

        private boolean validateAndSave() {
            ReplaceRule.OperationType type = selectedType();

            String match = matchField.getText().trim();
            if (type.supportsMatch()) {
                if (type.requiresMatch() && match.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Match pattern is required", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                if (!type.requiresMatch() && !type.allowsBlankMatch() && match.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Match pattern cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else {
                match = "";
            }

            String replaceValue = replaceField.getText();
            if (type.supportsReplace()) {
                if (type.requiresReplace() && replaceValue.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Replacement value is required", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else {
                replaceValue = "";
            }

            boolean useRegex = type.supportsRegex() && regexCheckbox.isSelected();

            if (operation == null) {
                operation = new ReplaceRule.ReplaceOperation(type, match, replaceValue, useRegex);
            } else {
                operation.setType(type);
                operation.setMatchPattern(match);
                operation.setReplaceValue(replaceValue);
                operation.setUseRegex(useRegex);
            }

            return true;
        }

        ReplaceRule.ReplaceOperation getOperation() {
            return confirmed ? operation.copy() : null;
        }
    }
}
