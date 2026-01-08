package ui;

import model.ReplaceRule;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ReplaceRuleDialog extends JDialog {
    private ReplaceRule rule;
    private final java.util.List<String> availableRoles;
    private JTextField nameField;
    private JComboBox<String> roleCombo; // Added
    private ReplaceOperationTableModel operationTableModel;
    private JTable operationTable;
    private JButton editOperationButton;
    private JButton removeOperationButton;
    private boolean confirmed;

    public ReplaceRuleDialog(Frame parent, ReplaceRule existingRule, java.util.List<String> availableRoles) {
        super(parent, existingRule == null ? "Add Replace Rule" : "Edit Replace Rule", true);
        this.rule = existingRule;
        this.availableRoles = availableRoles != null ? availableRoles : java.util.Collections.emptyList();
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

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Rule Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        panel.add(nameField, gbc);

        // Role Selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Apply User Role:"), gbc);
        gbc.gridx = 1;
        roleCombo = new JComboBox<>();
        roleCombo.addItem("None");
        for (String role : availableRoles) {
            roleCombo.addItem(role);
        }
        panel.add(roleCombo, gbc);

        // Operations panel
        gbc.gridx = 0;
        gbc.gridy = 2; // Shifted down
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JPanel operationsPanel = createOperationsPanel();
        panel.add(operationsPanel, gbc);

        gbc.gridy = 3; // Shifted down
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
        operationTable.getSelectionModel().addListSelectionListener(e -> updateOperationButtons());
        panel.add(new JScrollPane(operationTable), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new PrimaryButton("Add");
        addButton.addActionListener(e -> addOperation());
        editOperationButton = new PrimaryButton("Edit");
        editOperationButton.addActionListener(e -> editOperation());
        removeOperationButton = new PrimaryButton("Remove");
        removeOperationButton.addActionListener(e -> removeOperation());
        controls.add(addButton);
        controls.add(editOperationButton);
        controls.add(removeOperationButton);
        panel.add(controls, BorderLayout.SOUTH);

        updateOperationButtons();
        return panel;
    }

    private void populateFields(ReplaceRule rule) {
        nameField.setText(rule.getName());
        operationTableModel.setOperations(rule.getOperations());

        String currentRole = rule.getTargetRole();
        if (currentRole == null || currentRole.isEmpty()) {
            roleCombo.setSelectedItem("None");
        } else {
            roleCombo.setSelectedItem(currentRole);
        }

        operationTable.clearSelection();
        updateOperationButtons();
    }

    private boolean validateAndSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Rule name cannot be empty", "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String selectedRole = (String) roleCombo.getSelectedItem();
        boolean hasRole = selectedRole != null && !"None".equals(selectedRole);

        if (operationTableModel.getRowCount() == 0 && !hasRole) {
            JOptionPane.showMessageDialog(this, "Select a User Role OR add at least one replacement operation",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (rule == null) {
            rule = new ReplaceRule(name);
        } else {
            rule.setName(name);
        }

        if ("None".equals(selectedRole)) {
            rule.setTargetRole(null);
        } else {
            rule.setTargetRole(selectedRole);
        }

        rule.setOperations(operationTableModel.getOperations());
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
            updateOperationButtons();
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
            updateOperationButtons();
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
        updateOperationButtons();
    }

    private void updateOperationButtons() {
        boolean hasSelection = operationTable != null && operationTable.getSelectedRow() >= 0;
        if (editOperationButton != null) {
            editOperationButton.setEnabled(hasSelection);
        }
        if (removeOperationButton != null) {
            removeOperationButton.setEnabled(hasSelection);
        }
    }

    private static class ReplaceOperationTableModel extends AbstractTableModel {
        private final List<ReplaceRule.ReplaceOperation> operations = new ArrayList<>();
        private final String[] columns = { "Type", "Match Pattern", "Replace Value", "Regex" };

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
                case 2 -> operation.getReplaceValue();
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
        // Removed authTokensPanel and related fields
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
            gbc.weightx = 1.0;

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

            // Removed authTokensPanel creation and addition

            gbc.gridy = 5; // Adjusted gridy
            gbc.weighty = 0;
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

        // Removed createAuthTokensPanel()

        private ReplaceRule.OperationType selectedType() {
            ReplaceRule.OperationType type = (ReplaceRule.OperationType) typeCombo.getSelectedItem();
            return type != null ? type : ReplaceRule.OperationType.REQUEST_STRING;
        }

        private void updateFieldState() {
            ReplaceRule.OperationType type = selectedType();
            if (type == null) {
                return;
            }

            // Match Logic
            boolean supportsMatch = type.supportsMatch();
            boolean requiresMatch = type.requiresMatch();

            matchLabel.setVisible(supportsMatch);
            matchField.setVisible(supportsMatch);
            matchField.setEnabled(supportsMatch);
            if (!supportsMatch) {
                matchField.setText("");
            }

            // Replace Logic
            boolean showReplace = type.supportsReplace();
            boolean requiresReplace = type.requiresReplace();

            replaceLabel.setText("Replace Value:");
            replaceLabel.setVisible(showReplace);
            replaceField.setVisible(showReplace);
            replaceField.setEnabled(showReplace);
            if (!showReplace) {
                replaceField.setText("");
            }

            regexCheckbox.setVisible(type.supportsRegex());
            regexCheckbox.setEnabled(type.supportsRegex());
            if (!type.supportsRegex()) {
                regexCheckbox.setSelected(false);
            }

            if (supportsMatch) {
                matchField.setToolTipText(requiresMatch && !type.allowsBlankMatch()
                        ? "Match pattern is required"
                        : "Match pattern (optional)");
            } else {
                matchField.setToolTipText(null);
            }

            if (showReplace) {
                replaceField.setToolTipText(
                        requiresReplace ? "Replacement value is required" : "Replacement value (optional)");
            } else {
                replaceField.setToolTipText(null);
            }

            hintLabel.setText(switch (type) {
                case REQUEST_HEADER -> "Leave Match empty to add a header using 'Header-Name: value' in Replace.";
                case REMOVE_PARAMETER_BY_NAME, REMOVE_PARAMETER_BY_VALUE,
                        REMOVE_COOKIE_BY_NAME, REMOVE_COOKIE_BY_VALUE,
                        REMOVE_HEADER_BY_NAME, REMOVE_HEADER_BY_VALUE ->
                    "Removes items that match the pattern.";
                case MATCH_PARAM_NAME_REPLACE_VALUE -> "Matches parameter names and sets their value to Replace.";
                case MATCH_COOKIE_NAME_REPLACE_VALUE -> "Matches cookie names and sets their value to Replace.";
                case MATCH_HEADER_NAME_REPLACE_VALUE -> "Matches header names and sets their value to Replace.";
                default -> "";
            });

            // Removed authTokensPanel.setVisible(isAuthProfile);

            // Re-layout explicitly to handle visibility changes
            revalidate();
            repaint();
            pack(); // Resize dialog to fit new content
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

            // Removed AuthProfile token population
        }

        private boolean validateAndSave() {
            ReplaceRule.OperationType type = selectedType();
            String matchPattern = matchField.getText().trim();

            if (type.supportsMatch()) {
                if (type.requiresMatch() && matchPattern.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Match pattern is required", "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                if (!type.requiresMatch() && !type.allowsBlankMatch() && matchPattern.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Match pattern cannot be empty", "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else {
                matchPattern = "";
            }

            String replaceValue = replaceField.getText();
            if (type.supportsReplace()) {
                if (replaceValue.isEmpty() && type.requiresReplace()) {
                    JOptionPane.showMessageDialog(this, "Replace value cannot be empty for this operation type",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else {
                replaceValue = "";
            }

            boolean useRegex = type.supportsRegex() && regexCheckbox.isSelected();

            if (operation == null) {
                operation = new ReplaceRule.ReplaceOperation(type, matchPattern, replaceValue, useRegex);
            } else {
                operation.setType(type);
                operation.setMatchPattern(matchPattern);
                operation.setReplaceValue(replaceValue);
                operation.setUseRegex(useRegex);
            }
            return true;
        }

        public ReplaceRule.ReplaceOperation getOperation() {
            return confirmed ? operation : null;
        }
    }
}
