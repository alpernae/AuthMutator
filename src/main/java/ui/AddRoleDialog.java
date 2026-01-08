package ui;

import model.AuthToken;
import model.UserRole;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AddRoleDialog extends JDialog {
    private JTextField nameField;
    private DefaultTableModel tokenTableModel;
    private UserRole role;
    private UserRole originalRole; // To track if we are editing
    private boolean confirmed;

    public AddRoleDialog(Frame owner) {
        this(owner, null);
    }

    public AddRoleDialog(Frame owner, UserRole existingRole) {
        super(owner, existingRole == null ? "Add User Role" : "Edit User Role", true);
        this.originalRole = existingRole;
        initializeUI();
        if (existingRole != null) {
            populateFields(existingRole);
        }
        pack();
        setLocationRelativeTo(owner);
    }

    private void populateFields(UserRole role) {
        nameField.setText(role.getName());
        for (AuthToken token : role.getTokens()) {
            tokenTableModel.addRow(new Object[] { token.getType(), token.getName(), token.getValue() });
        }
    }

    private void initializeUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Role Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Role Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        panel.add(nameField, gbc);

        // Header for Tokens
        gbc.gridx = 0;
        gbc.gridy = 3; // Adjusted gridy
        gbc.gridwidth = 2;
        gbc.weighty = 0;
        JLabel helpLabel = new JLabel("Define cookies and headers to inject:");
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.BOLD));
        panel.add(helpLabel, gbc);

        // Tokens Panel (Table)
        gbc.gridy = 4; // Adjusted gridy
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel tokensPanel = createTokensPanel();
        panel.add(tokensPanel, gbc);

        // Buttons
        gbc.gridy = 5; // Adjusted gridy
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save Role");
        saveButton.addActionListener(e -> save());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, gbc);

        add(panel);
    }

    private JPanel createTokensPanel() {
        JPanel p = new JPanel(new BorderLayout());
        String[] cols = { "Type", "Name", "Value" };
        tokenTableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(tokenTableModel);

        JComboBox<AuthToken.Type> typeCombo = new JComboBox<>(AuthToken.Type.values());
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(typeCombo));

        p.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel ctrls = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addHeaderBtn = new PrimaryButton("Add Header");
        addHeaderBtn.addActionListener(e -> tokenTableModel.addRow(new Object[] { AuthToken.Type.HEADER, "", "" }));

        JButton addCookieBtn = new PrimaryButton("Add Cookie");
        addCookieBtn.setToolTipText("Add a 'Cookie' header.");
        addCookieBtn
                .addActionListener(e -> tokenTableModel.addRow(new Object[] { AuthToken.Type.HEADER, "Cookie", "" }));

        JButton removeBtn = new PrimaryButton("Remove");
        removeBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0)
                tokenTableModel.removeRow(r);
        });

        ctrls.add(addHeaderBtn);
        ctrls.add(addCookieBtn);
        ctrls.add(removeBtn);
        p.add(ctrls, BorderLayout.SOUTH);
        return p;
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Role Name is required", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<AuthToken> tokens = new ArrayList<>();
        for (int i = 0; i < tokenTableModel.getRowCount(); i++) {
            AuthToken.Type type = (AuthToken.Type) tokenTableModel.getValueAt(i, 0);
            String tokenName = (String) tokenTableModel.getValueAt(i, 1);
            String value = (String) tokenTableModel.getValueAt(i, 2);

            if (tokenName != null && !tokenName.trim().isEmpty()) {
                tokens.add(new AuthToken(type, tokenName.trim(), value != null ? value : ""));
            }
        }
        if (tokens.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one token", "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        role = new UserRole(name);
        role.setTokens(tokens);
        // If editing, preserve enabled state, otherwise default to true/false logic
        if (originalRole != null) {
            role.setEnabled(originalRole.isEnabled());
        } else {
            role.setEnabled(true);
        }

        confirmed = true;
        dispose();
    }

    public UserRole getUserRole() {
        return confirmed ? role : null;
    }
}
