package ui;

import model.ExtensionConfig;

import javax.swing.*;
import java.awt.*;

public class QuickControlsPanel extends JPanel {
    private final ExtensionConfig config;
    private JButton enableButton;
    private JCheckBox affectProxyCheckbox;
    private JCheckBox previewProxyCheckbox;
    private JCheckBox onlyInScopeCheckbox;
    private JCheckBox strictRoleScopedRulesCheckbox;
    private JCheckBox unauthTestingCheckbox;
    private JCheckBox applyRulesToUnauthCheckbox;
    private JCheckBox excludeStaticFilesCheckbox;
    private final Runnable onConfigChanged;
    private final Runnable onImport;
    private final Runnable onExport;
    private final Runnable onClearLog;

    public QuickControlsPanel(ExtensionConfig config, Runnable onConfigChanged, Runnable onImport, Runnable onExport,
            Runnable onClearLog) {
        this.config = config;
        this.onConfigChanged = onConfigChanged;
        this.onImport = onImport;
        this.onExport = onExport;
        this.onClearLog = onClearLog;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Quick Controls"),
            BorderFactory.createEmptyBorder(1, 4, 2, 4)));

        createLayout();
    }

    private void createLayout() {
        // Panel for checkboxes (left)
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 1));

        affectProxyCheckbox = new JCheckBox("Affect Proxy (modify browser traffic)");
        affectProxyCheckbox.setSelected(config.isApplyToProxy());
        affectProxyCheckbox.addActionListener(e -> {
            config.setApplyToProxy(affectProxyCheckbox.isSelected());
            signalConfigChanged();
        });

        previewProxyCheckbox = new JCheckBox("Preview in Proxy");
        previewProxyCheckbox.setSelected(config.isPreviewInProxy());
        previewProxyCheckbox.addActionListener(e -> {
            config.setPreviewInProxy(previewProxyCheckbox.isSelected());
            signalConfigChanged();
        });

        onlyInScopeCheckbox = new JCheckBox("In Scope Only");
        onlyInScopeCheckbox.setSelected(config.isOnlyInScope());
        onlyInScopeCheckbox.addActionListener(e -> {
            config.setOnlyInScope(onlyInScopeCheckbox.isSelected());
            signalConfigChanged();
        });

        strictRoleScopedRulesCheckbox = new JCheckBox("Strict role-scoped rules");
        strictRoleScopedRulesCheckbox.setSelected(config.isRoleScopedReplacementRules());
        strictRoleScopedRulesCheckbox
                .setToolTipText("Only run target-role rules when request already matches target role token context");
        strictRoleScopedRulesCheckbox.addActionListener(e -> {
            config.setRoleScopedReplacementRules(strictRoleScopedRulesCheckbox.isSelected());
            signalConfigChanged();
        });

        excludeStaticFilesCheckbox = new JCheckBox("Exclude static files");
        excludeStaticFilesCheckbox.setSelected(config.isExcludeStaticFiles());
        excludeStaticFilesCheckbox.setToolTipText("Skip images, CSS, JS, fonts, audio, video files");
        excludeStaticFilesCheckbox.addActionListener(e -> {
            config.setExcludeStaticFiles(excludeStaticFilesCheckbox.isSelected());
            signalConfigChanged();
        });

        applyRulesToUnauthCheckbox = new JCheckBox("Apply rules to unauth request");
        applyRulesToUnauthCheckbox.setSelected(config.isApplyRulesToUnauthenticatedRequest());
        applyRulesToUnauthCheckbox.addActionListener(e -> {
            config.setApplyRulesToUnauthenticatedRequest(applyRulesToUnauthCheckbox.isSelected());
            signalConfigChanged();
        });

        unauthTestingCheckbox = new JCheckBox("Unauthenticated testing");
        unauthTestingCheckbox.setSelected(config.isUnauthenticatedTesting());
        unauthTestingCheckbox.addActionListener(e -> {
            config.setUnauthenticatedTesting(unauthTestingCheckbox.isSelected());
            boolean extensionEnabled = config.isExtensionEnabled();
            applyRulesToUnauthCheckbox.setEnabled(unauthTestingCheckbox.isSelected() && extensionEnabled);
            if (!unauthTestingCheckbox.isSelected()) {
                applyRulesToUnauthCheckbox.setSelected(false);
                config.setApplyRulesToUnauthenticatedRequest(false);
            }
            signalConfigChanged();
        });

        leftPanel.add(affectProxyCheckbox);
        leftPanel.add(previewProxyCheckbox);
        leftPanel.add(onlyInScopeCheckbox);
        leftPanel.add(strictRoleScopedRulesCheckbox);
        leftPanel.add(excludeStaticFilesCheckbox);
        leftPanel.add(unauthTestingCheckbox);
        leftPanel.add(applyRulesToUnauthCheckbox);

        // Right panel for Actions and Toggle
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 1));

        // Import/Export Buttons
        JButton importBtn = new JButton("Import State"); // Standard swing button or PrimaryButton? Let's use JButton
                                                         // for now or PrimaryButton if visible.
        // PrimaryButton might be too colorful if we have many. Let's make them look
        // decent.
        // I'll stick to JButton to avoid import issues if PrimaryButton isn't imported,
        // but PrimaryButton is in ui package.
        // Let's use PrimaryButton for consistency if I import it.
        // QuickControlsPanel currently doesn't import PrimaryButton. I'll use JButton.
        importBtn.setMargin(new Insets(1, 8, 1, 8));
        importBtn.addActionListener(e -> {
            if (onImport != null)
                onImport.run();
        });

        JButton exportBtn = new JButton("Export State");
        exportBtn.setMargin(new Insets(1, 8, 1, 8));
        exportBtn.addActionListener(e -> {
            if (onExport != null)
                onExport.run();
        });

        JButton clearLogBtn = new JButton("Clear Log");
        clearLogBtn.setMargin(new Insets(1, 8, 1, 8));
        clearLogBtn.setToolTipText("Remove all entries from the request log table");
        clearLogBtn.addActionListener(e -> {
            if (onClearLog == null) {
                return;
            }
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Clear all request log entries?",
                    "Clear Request Log",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                onClearLog.run();
            }
        });

        // Enable/Disable button
        enableButton = new JButton();
        enableButton.setMargin(new Insets(1, 8, 1, 8));
        enableButton.addActionListener(e -> {
            boolean enabled = !config.isExtensionEnabled();
            config.setExtensionEnabled(enabled);
            updateEnableButtonState();
            updateControlEnablement();
            signalConfigChanged();
        });

        updateEnableButtonState();
        updateControlEnablement();

        rightPanel.add(importBtn);
        rightPanel.add(exportBtn);
        rightPanel.add(clearLogBtn);
        // Spacer
        rightPanel.add(Box.createHorizontalStrut(6));
        rightPanel.add(enableButton);

        add(leftPanel, BorderLayout.CENTER); // Changed to CENTER so it takes space
        add(rightPanel, BorderLayout.EAST);
    }

    public void refreshFromConfig() {
        affectProxyCheckbox.setSelected(config.isApplyToProxy());
        previewProxyCheckbox.setSelected(config.isPreviewInProxy());
        onlyInScopeCheckbox.setSelected(config.isOnlyInScope());
        strictRoleScopedRulesCheckbox.setSelected(config.isRoleScopedReplacementRules());
        excludeStaticFilesCheckbox.setSelected(config.isExcludeStaticFiles());
        unauthTestingCheckbox.setSelected(config.isUnauthenticatedTesting());
        applyRulesToUnauthCheckbox.setSelected(config.isApplyRulesToUnauthenticatedRequest());
        updateControlEnablement();
        updateEnableButtonState();
    }

    private void updateEnableButtonState() {
        boolean enabled = config.isExtensionEnabled();
        enableButton.setText(enabled ? "Extension: Enabled" : "Extension: Disabled");
        Color buttonBackground = UIManager.getColor("Button.background");
        Color buttonForeground = UIManager.getColor("Button.foreground");
        if (buttonBackground != null) {
            enableButton.setBackground(buttonBackground);
        }
        if (buttonForeground != null) {
            enableButton.setForeground(buttonForeground);
        }
    }

    private void updateControlEnablement() {
        boolean extensionEnabled = config.isExtensionEnabled();
        affectProxyCheckbox.setEnabled(extensionEnabled);
        previewProxyCheckbox.setEnabled(extensionEnabled);
        onlyInScopeCheckbox.setEnabled(extensionEnabled);
        strictRoleScopedRulesCheckbox.setEnabled(extensionEnabled);
        excludeStaticFilesCheckbox.setEnabled(extensionEnabled);
        unauthTestingCheckbox.setEnabled(extensionEnabled);
        applyRulesToUnauthCheckbox.setEnabled(extensionEnabled && unauthTestingCheckbox.isSelected());
    }

    private void signalConfigChanged() {
        if (onConfigChanged != null) {
            onConfigChanged.run();
        }
    }
}
