package ui;

import burp.api.montoya.MontoyaApi;
import model.ExtensionConfig;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {
    private final MontoyaApi api;
    private final ExtensionConfig config;
    private final Runnable onConfigChanged;
    
    private JCheckBox onlyInScopeCheckbox;
    private JCheckBox interceptEnabledCheckbox;
    private JCheckBox autoModifyCheckbox;
    private JCheckBox applyToProxyCheckbox;
    private JCheckBox applyToRepeaterCheckbox;
    private JCheckBox applyToIntruderCheckbox;
    private JCheckBox applyToScannerCheckbox;
    private JCheckBox previewInProxyCheckbox;
    private JSpinner maxEntriesSpinner;
    private JButton safeModeBtn;
    private JCheckBox unauthenticatedTestingCheckbox;
    private JCheckBox applyRulesToUnauthCheckbox;
    private JCheckBox excludeStaticFilesCheckbox;

    public SettingsPanel(MontoyaApi api, ExtensionConfig config, Runnable onConfigChanged) {
        this.api = api;
        this.config = config;
        this.onConfigChanged = onConfigChanged;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        
        JPanel settingsContainer = new JPanel();
        settingsContainer.setLayout(new BoxLayout(settingsContainer, BoxLayout.Y_AXIS));
        settingsContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Extension Settings");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsContainer.add(titleLabel);
        settingsContainer.add(Box.createVerticalStrut(20));

        // Scope settings
        JPanel scopePanel = new JPanel();
        scopePanel.setLayout(new BoxLayout(scopePanel, BoxLayout.Y_AXIS));
        scopePanel.setBorder(BorderFactory.createTitledBorder("Scope Settings"));
        scopePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        onlyInScopeCheckbox = new JCheckBox("Only log requests in scope");
        onlyInScopeCheckbox.setSelected(config.isOnlyInScope());
        onlyInScopeCheckbox.addActionListener(e -> {
            config.setOnlyInScope(onlyInScopeCheckbox.isSelected());
            api.logging().logToOutput("Only in scope: " + config.isOnlyInScope());
            notifyConfigChanged();
        });
        scopePanel.add(onlyInScopeCheckbox);

        excludeStaticFilesCheckbox = new JCheckBox("Exclude static files (images, CSS, JS, fonts, audio, video)");
        excludeStaticFilesCheckbox.setSelected(config.isExcludeStaticFiles());
        excludeStaticFilesCheckbox.setToolTipText("Skip interception and rule application for common static file types");
        excludeStaticFilesCheckbox.addActionListener(e -> {
            config.setExcludeStaticFiles(excludeStaticFilesCheckbox.isSelected());
            api.logging().logToOutput("Exclude static files: " + config.isExcludeStaticFiles());
            notifyConfigChanged();
        });
        scopePanel.add(excludeStaticFilesCheckbox);

        settingsContainer.add(scopePanel);
        settingsContainer.add(Box.createVerticalStrut(10));

    // Intercept settings
        JPanel interceptPanel = new JPanel();
        interceptPanel.setLayout(new BoxLayout(interceptPanel, BoxLayout.Y_AXIS));
        interceptPanel.setBorder(BorderFactory.createTitledBorder("Intercept Settings"));
        interceptPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        interceptEnabledCheckbox = new JCheckBox("Enable request interception");
        interceptEnabledCheckbox.setSelected(config.isInterceptEnabled());
        interceptEnabledCheckbox.addActionListener(e -> {
            config.setInterceptEnabled(interceptEnabledCheckbox.isSelected());
            api.logging().logToOutput("Intercept enabled: " + config.isInterceptEnabled());
            notifyConfigChanged();
        });
        interceptPanel.add(interceptEnabledCheckbox);

        autoModifyCheckbox = new JCheckBox("Automatically apply replace rules");
        autoModifyCheckbox.setSelected(config.isAutoModifyRequests());
        autoModifyCheckbox.addActionListener(e -> {
            config.setAutoModifyRequests(autoModifyCheckbox.isSelected());
            api.logging().logToOutput("Auto modify: " + config.isAutoModifyRequests());
            notifyConfigChanged();
        });
        interceptPanel.add(autoModifyCheckbox);

        unauthenticatedTestingCheckbox = new JCheckBox("Unauthenticated testing (strip cookies)");
        unauthenticatedTestingCheckbox.setSelected(config.isUnauthenticatedTesting());
        unauthenticatedTestingCheckbox.addActionListener(e -> {
            config.setUnauthenticatedTesting(unauthenticatedTestingCheckbox.isSelected());
            api.logging().logToOutput("Unauthenticated testing: " + config.isUnauthenticatedTesting());
            if (!config.isUnauthenticatedTesting()) {
                config.setApplyRulesToUnauthenticatedRequest(false);
                if (applyRulesToUnauthCheckbox != null) {
                    applyRulesToUnauthCheckbox.setSelected(false);
                }
            }
            notifyConfigChanged();
        });
        interceptPanel.add(unauthenticatedTestingCheckbox);

        applyRulesToUnauthCheckbox = new JCheckBox("Apply replace rules to unauth request");
        applyRulesToUnauthCheckbox.setSelected(config.isApplyRulesToUnauthenticatedRequest());
        applyRulesToUnauthCheckbox.addActionListener(e -> {
            config.setApplyRulesToUnauthenticatedRequest(applyRulesToUnauthCheckbox.isSelected());
            notifyConfigChanged();
        });
        applyRulesToUnauthCheckbox.setEnabled(config.isUnauthenticatedTesting());
        interceptPanel.add(applyRulesToUnauthCheckbox);

    settingsContainer.add(interceptPanel);
    settingsContainer.add(Box.createVerticalStrut(10));

        JPanel retentionPanel = new JPanel();
        retentionPanel.setLayout(new BoxLayout(retentionPanel, BoxLayout.Y_AXIS));
        retentionPanel.setBorder(BorderFactory.createTitledBorder("Request Log Retention"));
        retentionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel spinnerRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        spinnerRow.add(new JLabel("Maximum stored log entries:"));
        maxEntriesSpinner = new JSpinner(new SpinnerNumberModel(config.getMaxLogEntries(), 100, 100000, 100));
        maxEntriesSpinner.addChangeListener(e -> {
            int selected = (int) maxEntriesSpinner.getValue();
            config.setMaxLogEntries(selected);
            maxEntriesSpinner.setValue(config.getMaxLogEntries());
            notifyConfigChanged();
        });
        spinnerRow.add(maxEntriesSpinner);
        retentionPanel.add(spinnerRow);

        JLabel retentionHint = new JLabel("Oldest entries are discarded first when the limit is reached.");
        retentionHint.setFont(retentionHint.getFont().deriveFont(Font.ITALIC, retentionHint.getFont().getSize() - 1f));
        retentionPanel.add(retentionHint);

        settingsContainer.add(retentionPanel);
        settingsContainer.add(Box.createVerticalStrut(10));

        // Apply-to tools settings
    JPanel toolsPanel = new JPanel();
    toolsPanel.setLayout(new BoxLayout(toolsPanel, BoxLayout.Y_AXIS));
    toolsPanel.setBorder(BorderFactory.createTitledBorder("Apply Rules To"));
    toolsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Quick safety toggle
        safeModeBtn = new PrimaryButton("Safe Mode (Proxy Preview Only)");
        safeModeBtn.setToolTipText("Disable modifying Proxy traffic and keep preview on");
        safeModeBtn.addActionListener(e -> {
            config.setApplyToProxy(false);
            config.setPreviewInProxy(true);
            applyToProxyCheckbox.setSelected(false);
            previewInProxyCheckbox.setSelected(true);
            api.logging().logToOutput("Safe Mode enabled: Proxy modifications disabled, preview enabled");
            notifyConfigChanged();
        });

    applyToProxyCheckbox = new JCheckBox("Proxy (browser traffic)");
    applyToProxyCheckbox.setSelected(config.isApplyToProxy());
    applyToProxyCheckbox.setToolTipText("If disabled, rules will not modify Proxy traffic");
    applyToProxyCheckbox.addActionListener(e -> {
        config.setApplyToProxy(applyToProxyCheckbox.isSelected());
        notifyConfigChanged();
    });

    previewInProxyCheckbox = new JCheckBox("Preview in Proxy (don't modify, just log diffs)");
    previewInProxyCheckbox.setSelected(config.isPreviewInProxy());
    previewInProxyCheckbox.setToolTipText("Show original vs would-be-modified in the UI without changing live Proxy traffic");
    previewInProxyCheckbox.addActionListener(e -> {
        config.setPreviewInProxy(previewInProxyCheckbox.isSelected());
        notifyConfigChanged();
    });

    applyToRepeaterCheckbox = new JCheckBox("Repeater");
    applyToRepeaterCheckbox.setSelected(config.isApplyToRepeater());
    applyToRepeaterCheckbox.addActionListener(e -> {
        config.setApplyToRepeater(applyToRepeaterCheckbox.isSelected());
        notifyConfigChanged();
    });

    applyToIntruderCheckbox = new JCheckBox("Intruder");
    applyToIntruderCheckbox.setSelected(config.isApplyToIntruder());
    applyToIntruderCheckbox.addActionListener(e -> {
        config.setApplyToIntruder(applyToIntruderCheckbox.isSelected());
        notifyConfigChanged();
    });

    applyToScannerCheckbox = new JCheckBox("Scanner");
    applyToScannerCheckbox.setSelected(config.isApplyToScanner());
    applyToScannerCheckbox.addActionListener(e -> {
        config.setApplyToScanner(applyToScannerCheckbox.isSelected());
        notifyConfigChanged();
    });

    toolsPanel.add(safeModeBtn);
    toolsPanel.add(Box.createVerticalStrut(6));
    toolsPanel.add(applyToProxyCheckbox);
    toolsPanel.add(previewInProxyCheckbox);
    toolsPanel.add(applyToRepeaterCheckbox);
    toolsPanel.add(applyToIntruderCheckbox);
    toolsPanel.add(applyToScannerCheckbox);

    settingsContainer.add(toolsPanel);
    settingsContainer.add(Box.createVerticalStrut(10));

        add(settingsContainer, BorderLayout.NORTH);
    }

    public void refreshFromConfig() {
        onlyInScopeCheckbox.setSelected(config.isOnlyInScope());
        excludeStaticFilesCheckbox.setSelected(config.isExcludeStaticFiles());
        interceptEnabledCheckbox.setSelected(config.isInterceptEnabled());
        autoModifyCheckbox.setSelected(config.isAutoModifyRequests());
        applyToProxyCheckbox.setSelected(config.isApplyToProxy());
        applyToRepeaterCheckbox.setSelected(config.isApplyToRepeater());
        applyToIntruderCheckbox.setSelected(config.isApplyToIntruder());
        applyToScannerCheckbox.setSelected(config.isApplyToScanner());
        previewInProxyCheckbox.setSelected(config.isPreviewInProxy());
        if (unauthenticatedTestingCheckbox != null) {
            unauthenticatedTestingCheckbox.setSelected(config.isUnauthenticatedTesting());
        }
        if (applyRulesToUnauthCheckbox != null) {
            applyRulesToUnauthCheckbox.setSelected(config.isApplyRulesToUnauthenticatedRequest());
            applyRulesToUnauthCheckbox.setEnabled(config.isUnauthenticatedTesting());
        }
        if (maxEntriesSpinner != null) {
            maxEntriesSpinner.setValue(config.getMaxLogEntries());
        }
    }

    private void notifyConfigChanged() {
        if (onConfigChanged != null) {
            onConfigChanged.run();
        }
    }

}

