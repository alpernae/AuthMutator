package ui;

import model.ExtensionConfig;

import javax.swing.*;
import java.awt.*;

public class QuickControlsPanel extends JPanel {
    private final ExtensionConfig config;
    private JToggleButton enableButton;
    private JCheckBox affectProxyCheckbox;
    private JCheckBox previewProxyCheckbox;
    private JCheckBox onlyInScopeCheckbox;
    private JCheckBox unauthTestingCheckbox;
    private JCheckBox applyRulesToUnauthCheckbox;
    private JCheckBox excludeStaticFilesCheckbox;
    private final Runnable onConfigChanged;
    private final Runnable onImport;
    private final Runnable onExport;

    public QuickControlsPanel(ExtensionConfig config, Runnable onConfigChanged, Runnable onImport, Runnable onExport) {
        this.config = config;
        this.onConfigChanged = onConfigChanged;
        this.onImport = onImport;
        this.onExport = onExport;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Quick Controls"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        createLayout();
    }

    private void createLayout() {
        // Panel for checkboxes (left)
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));

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
        leftPanel.add(excludeStaticFilesCheckbox);
        leftPanel.add(unauthTestingCheckbox);
        leftPanel.add(applyRulesToUnauthCheckbox);

        // Right panel for Actions and Toggle
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        // Import/Export Buttons
        JButton importBtn = new JButton("Import State"); // Standard swing button or PrimaryButton? Let's use JButton
                                                         // for now or PrimaryButton if visible.
        // PrimaryButton might be too colorful if we have many. Let's make them look
        // decent.
        // I'll stick to JButton to avoid import issues if PrimaryButton isn't imported,
        // but PrimaryButton is in ui package.
        // Let's use PrimaryButton for consistency if I import it.
        // QuickControlsPanel currently doesn't import PrimaryButton. I'll use JButton.
        importBtn.setMargin(new Insets(2, 6, 2, 6));
        importBtn.addActionListener(e -> {
            if (onImport != null)
                onImport.run();
        });

        JButton exportBtn = new JButton("Export State");
        exportBtn.setMargin(new Insets(2, 6, 2, 6));
        exportBtn.addActionListener(e -> {
            if (onExport != null)
                onExport.run();
        });

        // Enable Toggle
        enableButton = new JToggleButton("Extension: ENABLED");
        enableButton.setSelected(config.isExtensionEnabled());
        enableButton.setOpaque(true);
        enableButton.setMargin(new Insets(4, 10, 4, 10)); // Bigger button
        enableButton.setFocusPainted(false);
        enableButton.addActionListener(e -> {
            boolean enabled = enableButton.isSelected();
            config.setExtensionEnabled(enabled);
            updateEnableButtonState();
            // Enable/disable other controls
            affectProxyCheckbox.setEnabled(enabled);
            previewProxyCheckbox.setEnabled(enabled);
            onlyInScopeCheckbox.setEnabled(enabled);
            excludeStaticFilesCheckbox.setEnabled(enabled);
            unauthTestingCheckbox.setEnabled(enabled);
            applyRulesToUnauthCheckbox.setEnabled(enabled && unauthTestingCheckbox.isSelected());
            signalConfigChanged();
        });

        updateEnableButtonState();
        affectProxyCheckbox.setEnabled(config.isExtensionEnabled());
        previewProxyCheckbox.setEnabled(config.isExtensionEnabled());
        onlyInScopeCheckbox.setEnabled(config.isExtensionEnabled());
        excludeStaticFilesCheckbox.setEnabled(config.isExtensionEnabled());
        boolean extensionEnabled = config.isExtensionEnabled();
        unauthTestingCheckbox.setEnabled(extensionEnabled);
        applyRulesToUnauthCheckbox.setEnabled(extensionEnabled && unauthTestingCheckbox.isSelected());

        rightPanel.add(importBtn);
        rightPanel.add(exportBtn);
        // Spacer
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(enableButton);

        add(leftPanel, BorderLayout.CENTER); // Changed to CENTER so it takes space
        add(rightPanel, BorderLayout.EAST);
    }

    public void refreshFromConfig() {
        enableButton.setSelected(config.isExtensionEnabled());
        affectProxyCheckbox.setSelected(config.isApplyToProxy());
        previewProxyCheckbox.setSelected(config.isPreviewInProxy());
        onlyInScopeCheckbox.setSelected(config.isOnlyInScope());
        excludeStaticFilesCheckbox.setSelected(config.isExcludeStaticFiles());
        unauthTestingCheckbox.setSelected(config.isUnauthenticatedTesting());
        applyRulesToUnauthCheckbox.setSelected(config.isApplyRulesToUnauthenticatedRequest());
        affectProxyCheckbox.setEnabled(config.isExtensionEnabled());
        previewProxyCheckbox.setEnabled(config.isExtensionEnabled());
        onlyInScopeCheckbox.setEnabled(config.isExtensionEnabled());
        excludeStaticFilesCheckbox.setEnabled(config.isExtensionEnabled());
        boolean extensionEnabled = config.isExtensionEnabled();
        unauthTestingCheckbox.setEnabled(extensionEnabled);
        applyRulesToUnauthCheckbox.setEnabled(extensionEnabled && unauthTestingCheckbox.isSelected());
        updateEnableButtonState();
    }

    private void updateEnableButtonState() {
        boolean enabled = enableButton.isSelected();
        enableButton.setText(enabled ? "Extension: ENABLED" : "Extension: DISABLED");
        enableButton.setBackground(enabled ? new Color(144, 238, 144) : new Color(255, 160, 160));
        enableButton.setForeground(shouldUseDarkText() ? Color.BLACK : Color.DARK_GRAY);
    }

    private boolean shouldUseDarkText() {
        Color bg = getBackground();
        int brightness = (int) Math.sqrt(
                bg.getRed() * bg.getRed() * .241 +
                        bg.getGreen() * bg.getGreen() * .691 +
                        bg.getBlue() * bg.getBlue() * .068);
        return brightness < 160;
    }

    private void signalConfigChanged() {
        if (onConfigChanged != null) {
            onConfigChanged.run();
        }
    }
}
