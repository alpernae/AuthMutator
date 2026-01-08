package ui;

import burp.api.montoya.MontoyaApi;
import model.ReplaceRule;
import model.UserRole;
import model.HighlightRule;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ReplaceRulesPanel extends JPanel {
    private final MontoyaApi api;
    private final List<ReplaceRule> rules;
    private final List<UserRole> userRoles; // Declared
    private final ReplaceRuleTableModel tableModel;
    private JTable rulesTable;
    private final Consumer<List<ReplaceRule>> onRulesChanged;
    private final Consumer<java.io.File> importHandler;
    private final Consumer<java.io.File> exportHandler;
    private final Consumer<List<UserRole>> onRolesChanged; // Declared

    private javax.swing.table.DefaultTableModel roleTableModel; // Declared
    private JTable roleTable; // Declared
    private HighlightRulesPanel highlightRulesPanel;

    public ReplaceRulesPanel(MontoyaApi api,
            List<ReplaceRule> initialRules,
            List<UserRole> initialRoles,
            List<HighlightRule> initialHighlightRules,
            Consumer<List<ReplaceRule>> onRulesChanged,
            Consumer<List<UserRole>> onRolesChanged,
            Consumer<List<HighlightRule>> onHighlightRulesChanged,
            Consumer<java.io.File> importHandler,
            Consumer<java.io.File> exportHandler) {
        this.api = api;
        this.rules = new ArrayList<>(initialRules);
        this.userRoles = new ArrayList<>(initialRoles != null ? initialRoles : new ArrayList<>()); // Initialized
        this.tableModel = new ReplaceRuleTableModel(rules);
        this.onRulesChanged = onRulesChanged;
        this.importHandler = importHandler;
        this.exportHandler = exportHandler;
        this.onRolesChanged = onRolesChanged;

        setLayout(new BorderLayout());

        // Create Sub-Panels
        JPanel rolesPanel = createRolesPanel();

        this.highlightRulesPanel = new HighlightRulesPanel(api, initialHighlightRules, onHighlightRulesChanged,
                this::getRoleNames);
        this.highlightRulesPanel.setBorder(BorderFactory.createTitledBorder("Highlight Rules"));

        JPanel replacementRulesPanel = createRulesPanel();

        // Top Split: Roles | Highlight Rules
        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rolesPanel, this.highlightRulesPanel);
        topSplit.setResizeWeight(0.5); // Equal width

        // Main Split: Top / Bottom (Replacement Rules)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, replacementRulesPanel);
        mainSplit.setResizeWeight(0.4); // Top takes 40% height

        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel createRulesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Replacement Rules"));

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
        panel.add(scrollPane, BorderLayout.CENTER);

        // Control panel (Add Rule, Edit, Remove)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
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

        controlPanel.add(addButton);
        controlPanel.add(editButton);
        controlPanel.add(deleteButton);
        controlPanel.add(enableButton);
        controlPanel.add(importButton);
        controlPanel.add(exportButton);

        panel.add(controlPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRolesPanel() {
        // Create a simple panel to manage roles
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("User Roles (Auth Profiles)"));

        // Model and Table for Roles
        String[] columnNames = { "Enabled", "Name", "Tokens" };
        roleTableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return Boolean.class;
                return super.getColumnClass(columnIndex);
            }
        };

        for (UserRole role : userRoles) {
            roleTableModel
                    .addRow(new Object[] { role.isEnabled(), role.getName(), role.getTokens().size() + " tokens" });
        }

        roleTable = new JTable(roleTableModel);
        roleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(roleTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new PrimaryButton("Add Role");
        addBtn.addActionListener(e -> {
            AddRoleDialog dialog = new AddRoleDialog((Frame) SwingUtilities.getWindowAncestor(this));
            dialog.setVisible(true);
            UserRole newRole = dialog.getUserRole();
            if (newRole != null) {
                userRoles.add(newRole);
                roleTableModel
                        .addRow(new Object[] { newRole.isEnabled(), newRole.getName(),
                                newRole.getTokens().size() + " tokens" });
                notifyRolesChanged();
                api.logging().logToOutput("Added role: " + newRole.getName());
            }
        });

        JButton editBtn = new PrimaryButton("Edit");
        editBtn.addActionListener(e -> editRole());

        JButton removeBtn = new PrimaryButton("Delete");
        removeBtn.addActionListener(e -> {
            int selected = roleTable.getSelectedRow();
            if (selected >= 0) {
                UserRole removedRole = userRoles.remove(selected);
                roleTableModel.removeRow(selected);
                notifyRolesChanged();
                api.logging().logToOutput("Removed role: " + removedRole.getName());
            }
        });

        JButton toggleBtn = new PrimaryButton("Enable/Disable");
        toggleBtn.addActionListener(e -> toggleRole());

        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(removeBtn);
        buttons.add(toggleBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private void editRole() {
        int selected = roleTable.getSelectedRow();
        if (selected >= 0) {
            UserRole roleToEdit = userRoles.get(selected);
            AddRoleDialog dialog = new AddRoleDialog((Frame) SwingUtilities.getWindowAncestor(this), roleToEdit);
            dialog.setVisible(true);
            UserRole updatedRole = dialog.getUserRole();
            if (updatedRole != null) {
                userRoles.set(selected, updatedRole);
                roleTableModel.setValueAt(updatedRole.isEnabled(), selected, 0);
                roleTableModel.setValueAt(updatedRole.getName(), selected, 1);
                roleTableModel.setValueAt(updatedRole.getTokens().size() + " tokens", selected, 2);
                notifyRolesChanged();
                api.logging().logToOutput("Updated role: " + updatedRole.getName());
            }
        }
    }

    private void toggleRole() {
        int selected = roleTable.getSelectedRow();
        if (selected >= 0) {
            UserRole role = userRoles.get(selected);
            role.setEnabled(!role.isEnabled());
            roleTableModel.setValueAt(role.isEnabled(), selected, 0);
            notifyRolesChanged();
            api.logging().logToOutput("Toggled role: " + role.getName() + " - Enabled: " + role.isEnabled());
        }
    }

    private void notifyRolesChanged() {
        if (onRolesChanged != null) {
            onRolesChanged.accept(userRoles);
        }
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
        // Collect role names
        java.util.List<String> roleNames = userRoles.stream().map(UserRole::getName).collect(Collectors.toList());
        ReplaceRuleDialog dialog = new ReplaceRuleDialog((Frame) SwingUtilities.getWindowAncestor(this), null,
                roleNames);
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
            java.util.List<String> roleNames = userRoles.stream().map(UserRole::getName).collect(Collectors.toList());
            ReplaceRuleDialog dialog = new ReplaceRuleDialog((Frame) SwingUtilities.getWindowAncestor(this), rule,
                    roleNames);
            dialog.setVisible(true);
            if (dialog.getRule() != null) {
                tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
                notifyRulesChanged();
                api.logging().logToOutput("Updated rule: " + rule.getName());
            }
        }
    }

    // This method is no longer needed as role management is integrated
    // private void showAddRoleDialog() {
    // AddRoleDialog dialog = new AddRoleDialog((Frame)
    // SwingUtilities.getWindowAncestor(this));
    // dialog.setVisible(true);
    //
    // ReplaceRule newRule = dialog.getRule();
    // if (newRule != null) {
    // rules.add(newRule);
    // tableModel.fireTableRowsInserted(rules.size() - 1, rules.size() - 1);
    // notifyRulesChanged();
    // api.logging().logToOutput("Added role: " + newRule.getName());
    // }
    // }

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

    public List<String> getRoleNames() {
        return userRoles.stream().map(UserRole::getName).collect(Collectors.toList());
    }

    public List<UserRole> getUserRoles() {
        return new ArrayList<>(userRoles);
    }

    public void setUserRoles(List<UserRole> roles) {
        this.userRoles.clear();
        if (roles != null) {
            this.userRoles.addAll(roles);
        }
        // Update table
        if (roleTableModel != null) {
            roleTableModel.setRowCount(0);
            for (UserRole role : userRoles) {
                roleTableModel
                        .addRow(new Object[] { role.isEnabled(), role.getName(), role.getTokens().size() + " tokens" });
            }
        }
    }

    public void setHighlightRules(List<HighlightRule> rules) {
        if (highlightRulesPanel != null) {
            highlightRulesPanel.setRules(rules);
        }
    }

    public List<HighlightRule> getHighlightRules() {
        if (highlightRulesPanel != null) {
            return highlightRulesPanel.getRules();
        }
        return new ArrayList<>();
    }

    public List<ReplaceRule> getRules() {
        return new ArrayList<>(rules);
    }

    public void setRules(List<ReplaceRule> newRules) {
        rules.clear();
        if (newRules != null) {
            rules.addAll(newRules);
        }
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
