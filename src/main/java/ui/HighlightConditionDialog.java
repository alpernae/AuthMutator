package ui;

import model.HighlightCondition;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class HighlightConditionDialog extends JDialog {
    private final HighlightCondition condition;
    private final java.util.List<String> availableRoles;
    private JComboBox<HighlightCondition.MessageVersion> versionCombo;
    private JComboBox<String> roleCombo; // New
    private JComboBox<HighlightCondition.MatchPart> partCombo;
    private JComboBox<HighlightCondition.Relationship> relationshipCombo;
    private JTextField valueField;
    private boolean confirmed;

    public HighlightConditionDialog(Window owner, HighlightCondition existing, java.util.List<String> availableRoles) {
        super(owner, existing == null ? "Add Condition" : "Edit Condition", ModalityType.APPLICATION_MODAL);
        this.condition = existing != null ? existing.copy() : new HighlightCondition();
        this.availableRoles = availableRoles != null ? availableRoles : java.util.Collections.emptyList();
        initializeUI();
        populateFields();
        pack();
        setLocationRelativeTo(owner);
    }

    public HighlightCondition getCondition() {
        return confirmed ? condition.copy() : null;
    }

    private void initializeUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        versionCombo = new JComboBox<>(HighlightCondition.MessageVersion.values());

        // Role Combo
        roleCombo = new JComboBox<>();
        roleCombo.addItem("Any");
        for (String role : availableRoles) {
            roleCombo.addItem(role);
        }

        partCombo = new JComboBox<>(HighlightCondition.MatchPart.values());
        relationshipCombo = new JComboBox<>(HighlightCondition.Relationship.values());
        valueField = new JTextField(28);

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Match original or modified:"), gbc);
        gbc.gridx = 1;
        panel.add(versionCombo, gbc);

        row++; // New Row
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("User Role:"), gbc);
        gbc.gridx = 1;
        panel.add(roleCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Match part:"), gbc);
        gbc.gridx = 1;
        panel.add(partCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Relationship:"), gbc);
        gbc.gridx = 1;
        panel.add(relationshipCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Match value:"), gbc);
        gbc.gridx = 1;
        panel.add(valueField, gbc);

        JPanel notePanel = new JPanel(new BorderLayout());
        notePanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        JLabel note = new JLabel("Regex relationships use Java syntax. Length comparisons use string values.");
        note.setFont(note.getFont().deriveFont(Font.ITALIC, note.getFont().getSize() - 1f));
        notePanel.add(note, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (validateAndSave()) {
                confirmed = true;
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(okButton);
        buttons.add(cancelButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(notePanel, BorderLayout.NORTH);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    private void populateFields() {
        versionCombo.setSelectedItem(condition.getMessageVersion());

        String role = condition.getTargetRole();
        if (role == null || role.isEmpty()) {
            roleCombo.setSelectedItem("Any");
        } else {
            roleCombo.setSelectedItem(role);
        }

        partCombo.setSelectedItem(condition.getMatchPart());
        relationshipCombo.setSelectedItem(condition.getRelationship());
        valueField.setText(condition.getMatchValue());
    }

    private boolean validateAndSave() {
        HighlightCondition.MatchPart selectedPart = (HighlightCondition.MatchPart) partCombo.getSelectedItem();
        String value = sanitizeMatchValue(selectedPart, valueField.getText());
        if (value.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Match value cannot be empty", "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        HighlightCondition.Relationship relationship = (HighlightCondition.Relationship) relationshipCombo
                .getSelectedItem();
        if (relationship != null && relationship.requiresNumericComparison()) {
            if (selectedPart == null || !selectedPart.supportsNumericComparison()) {
                JOptionPane.showMessageDialog(this,
                        "Numeric comparisons are only supported for status code and length fields.", "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (!isNumeric(value)) {
                JOptionPane.showMessageDialog(this, "Please enter a numeric value for the selected comparison.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (relationship != null && relationship.isRegex()) {
            try {
                Pattern.compile(value);
            } catch (PatternSyntaxException ex) {
                JOptionPane.showMessageDialog(this, "Invalid regular expression: " + ex.getMessage(),
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        condition.setMessageVersion((HighlightCondition.MessageVersion) versionCombo.getSelectedItem());
        condition.setTargetRole((String) roleCombo.getSelectedItem());
        condition.setMatchPart((HighlightCondition.MatchPart) partCombo.getSelectedItem());
        condition.setRelationship(relationship);
        condition.setMatchValue(value);
        return true;
    }

    private String sanitizeMatchValue(HighlightCondition.MatchPart part, String rawValue) {
        if (rawValue == null) {
            return "";
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty() || part == null) {
            return trimmed;
        }

        String label = part.getLabel();
        String lowerTrimmed = trimmed.toLowerCase();
        String lowerLabel = label.toLowerCase();
        if (lowerTrimmed.startsWith(lowerLabel)) {
            String remainder = trimmed.substring(label.length()).trim();
            if (!remainder.isEmpty() && (remainder.charAt(0) == ':' || remainder.charAt(0) == '=')) {
                remainder = remainder.substring(1).trim();
            }
            if (!remainder.isEmpty()) {
                trimmed = remainder;
            }
        }
        return trimmed;
    }

    private boolean isNumeric(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(trimmed);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
