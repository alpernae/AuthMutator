package ui;

import burp.api.montoya.MontoyaApi;
import handler.RequestHandler;
import model.ExtensionConfig;
import model.ExtensionState;
import model.RequestLogModel;
import util.PersistenceService;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MainPanel extends JPanel {
    private final MontoyaApi api;
    private final RequestLogModel requestLogModel;
    private final ExtensionConfig config;
    private final RequestHandler requestHandler;
    private final PersistenceService persistenceService;
    private RequestTablePanel requestTablePanel;
    private FilterPanel filterPanel;
    private ReplaceRulesPanel replaceRulesPanel;
    private HighlightRulesPanel highlightRulesPanel;
    private SettingsPanel settingsPanel;
    private QuickControlsPanel quickControlsPanel;

    public MainPanel(MontoyaApi api, RequestLogModel requestLogModel, ExtensionConfig config, RequestHandler requestHandler) {
        this.api = api;
        this.requestLogModel = requestLogModel;
        this.config = config;
        this.requestHandler = requestHandler;
        this.persistenceService = new PersistenceService(api);

        ExtensionState initialState = persistenceService.loadState(config);
        this.requestLogModel.setMaxEntries(config.getMaxLogEntries());
        initializeUI(initialState);
        requestLogModel.setShowUnauthColumn(config.isUnauthenticatedTesting());
        requestTablePanel.refreshColumnLayout();
        requestHandler.setReplaceRules(initialState.getReplaceRules());
        requestTablePanel.setHighlightRules(initialState.getHighlightRules());
        quickControlsPanel.refreshFromConfig();
        settingsPanel.refreshFromConfig();
        persistState();
    }

    private void initializeUI(ExtensionState initialState) {
        setLayout(new BorderLayout());

        // Create tabbed pane for different sections
        JTabbedPane tabbedPane = new JTabbedPane();

        // Request Log Tab
        requestTablePanel = new RequestTablePanel(api, requestLogModel);
        JPanel requestLogTab = new JPanel(new BorderLayout());
        // Quick controls + Filter panel on top
        JPanel controls = new JPanel(new BorderLayout());
        quickControlsPanel = new QuickControlsPanel(config, this::handleConfigChanged);
        JPanel quickRow = new JPanel(new BorderLayout());
        quickRow.add(quickControlsPanel, BorderLayout.CENTER);
        quickRow.add(new StateActionsPanel(e -> importStateFromChooser(), e -> exportStateFromChooser()), BorderLayout.EAST);
        controls.add(quickRow, BorderLayout.NORTH);
        filterPanel = new FilterPanel(requestLogModel, requestTablePanel);
        controls.add(filterPanel, BorderLayout.SOUTH);
        requestLogTab.add(controls, BorderLayout.NORTH);
        requestLogTab.add(requestTablePanel, BorderLayout.CENTER);
        tabbedPane.addTab("Request Log", requestLogTab);

        // Replace Rules Tab
        replaceRulesPanel = new ReplaceRulesPanel(
                api,
                initialState.getReplaceRules(),
                this::handleReplaceRulesChanged,
                this::handleImportState,
                this::handleExportState
        );
        tabbedPane.addTab("Replace Rules", replaceRulesPanel);

        // Highlight Rules Tab
        highlightRulesPanel = new HighlightRulesPanel(api, initialState.getHighlightRules(), this::handleHighlightRulesChanged);
        tabbedPane.addTab("Highlight Rules", highlightRulesPanel);

        // Settings Tab
        settingsPanel = new SettingsPanel(api, config, this::handleConfigChanged);
        tabbedPane.addTab("Settings", settingsPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void updateHighlightRules() {
        if (requestTablePanel != null && highlightRulesPanel != null) {
            requestTablePanel.setHighlightRules(highlightRulesPanel.getRules());
        }
    }

    private void handleReplaceRulesChanged(java.util.List<model.ReplaceRule> rules) {
        requestHandler.setReplaceRules(rules);
        persistState();
    }

    private void handleHighlightRulesChanged(java.util.List<model.HighlightRule> rules) {
        requestTablePanel.setHighlightRules(rules);
        persistState();
    }

    private void handleConfigChanged() {
        requestLogModel.setMaxEntries(config.getMaxLogEntries());
        requestLogModel.setShowUnauthColumn(config.isUnauthenticatedTesting());
        persistState();
        if (quickControlsPanel != null) {
            quickControlsPanel.refreshFromConfig();
        }
        if (settingsPanel != null) {
            settingsPanel.refreshFromConfig();
        }
        if (requestTablePanel != null) {
            requestTablePanel.refreshColumnLayout();
        }
    }

    private void handleImportState(File file) {
        if (file == null) {
            return;
        }
        try {
            ExtensionState imported = persistenceService.importState(file.toPath(), config);
            applyImportedState(imported);
            JOptionPane.showMessageDialog(this, "Import successful.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage());
        }
    }

    private void handleExportState(File file) {
        if (file == null) {
            return;
        }
        try {
            persistenceService.exportState(file.toPath(), config, collectState());
            JOptionPane.showMessageDialog(this, "Export successful.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    private void importStateFromChooser() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            handleImportState(chooser.getSelectedFile());
        }
    }

    private void exportStateFromChooser() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            handleExportState(chooser.getSelectedFile());
        }
    }

    private void applyImportedState(ExtensionState state) {
        replaceRulesPanel.setRules(state.getReplaceRules());
        highlightRulesPanel.setRules(state.getHighlightRules());
        requestLogModel.setMaxEntries(config.getMaxLogEntries());
        requestLogModel.setShowUnauthColumn(config.isUnauthenticatedTesting());
        quickControlsPanel.refreshFromConfig();
        settingsPanel.refreshFromConfig();
        requestTablePanel.refreshColumnLayout();
        updateHighlightRules();
        persistState();
    }

    private void persistState() {
        persistenceService.saveState(config, collectState());
    }

    private ExtensionState collectState() {
        return new ExtensionState(
                replaceRulesPanel.getRules(),
                highlightRulesPanel.getRules()
        );
    }
}
