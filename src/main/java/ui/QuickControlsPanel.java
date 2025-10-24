package ui;

import model.ExtensionConfig;

import javax.swing.*;
import java.awt.*;

public class QuickControlsPanel extends JPanel {
    private final ExtensionConfig config;
    private final JToggleButton enableButton;
    private final JCheckBox affectProxyCheckbox;
    private final JCheckBox previewProxyCheckbox;
    private final JCheckBox onlyInScopeCheckbox;
    private final JCheckBox unauthTestingCheckbox;
    private final JCheckBox applyRulesToUnauthCheckbox;
    private final JCheckBox excludeStaticFilesCheckbox;
    private final Runnable onConfigChanged;

    public QuickControlsPanel(ExtensionConfig config, Runnable onConfigChanged) {
        this.config = config;
        this.onConfigChanged = onConfigChanged;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Quick Controls"));

    // Panel for checkboxes (left)
    JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

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

        // Main enable/disable toggle button (right)
        enableButton = new JToggleButton("Extension: ENABLED");
        enableButton.setSelected(config.isExtensionEnabled());
        enableButton.setOpaque(true);
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

        add(leftPanel, BorderLayout.WEST);
        add(enableButton, BorderLayout.EAST);
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
