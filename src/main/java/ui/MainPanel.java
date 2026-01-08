package ui;

import burp.api.montoya.MontoyaApi;
import handler.RequestHandler;
import model.ExtensionConfig;
import model.ExtensionState;
import model.HighlightRule;
import model.RequestLogModel;
import model.UserRole;
import util.PersistenceService;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainPanel extends JPanel {
    private final MontoyaApi api;
    private final RequestLogModel requestLogModel;
    private final ExtensionConfig config;
    private final RequestHandler requestHandler;
    private final PersistenceService persistenceService;
    private RequestTablePanel requestTablePanel;
    private FilterPanel filterPanel;
    private ReplaceRulesPanel replaceRulesPanel;
    // private HighlightRulesPanel highlightRulesPanel; // Removed as it is now
    // embedded
    private SettingsPanel settingsPanel;
    private QuickControlsPanel quickControlsPanel;

    public MainPanel(MontoyaApi api, RequestLogModel requestLogModel, ExtensionConfig config,
            RequestHandler requestHandler) {
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
        requestTablePanel.refreshColumnLayout();
        requestHandler.setReplaceRules(initialState.getReplaceRules());
        requestHandler.setUserRoles(initialState.getUserRoles());
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
        quickControlsPanel = new QuickControlsPanel(config, this::handleConfigChanged, this::importStateFromChooser,
                this::exportStateFromChooser);
        controls.add(quickControlsPanel, BorderLayout.NORTH);
        filterPanel = new FilterPanel(requestLogModel, requestTablePanel);
        controls.add(filterPanel, BorderLayout.SOUTH);
        requestLogTab.add(controls, BorderLayout.NORTH);
        requestLogTab.add(requestTablePanel, BorderLayout.CENTER);
        tabbedPane.addTab("Request Log", requestLogTab);

        // Replace Rules Tab
        // Replace Rules Tab (Rules and Roles and Highlight Rules)
        replaceRulesPanel = new ReplaceRulesPanel(api,
                initialState.getReplaceRules(),
                initialState.getUserRoles(),
                initialState.getHighlightRules(),
                this::handleReplaceRulesChanged,
                this::handleUserRolesChanged,
                this::handleHighlightRulesChanged,
                this::handleImportState,
                this::handleExportState);
        tabbedPane.addTab("Rules and Roles", replaceRulesPanel);

        // Highlight Rules Tab - REMOVED (Embedded in Rules and Roles)

        // Settings Tab
        settingsPanel = new SettingsPanel(api, config, this::handleConfigChanged);
        tabbedPane.addTab("Settings", settingsPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void updateHighlightRules() {
        if (requestTablePanel != null && replaceRulesPanel != null) {
            requestTablePanel.setHighlightRules(replaceRulesPanel.getHighlightRules());
        }
    }

    private void handleReplaceRulesChanged(java.util.List<model.ReplaceRule> rules) {
        requestHandler.setReplaceRules(rules);
        persistState();
    }

    private void handleHighlightRulesChanged(List<HighlightRule> rules) {
        requestHandler.setHighlightRules(rules);
        persistState();
    }

    private void handleUserRolesChanged(List<UserRole> roles) {
        requestHandler.setUserRoles(roles);
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
        replaceRulesPanel.setUserRoles(state.getUserRoles());
        replaceRulesPanel.setHighlightRules(state.getHighlightRules());
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
                replaceRulesPanel != null ? replaceRulesPanel.getRules() : new ArrayList<>(),
                replaceRulesPanel != null ? replaceRulesPanel.getHighlightRules() : new ArrayList<>(),
                replaceRulesPanel != null ? replaceRulesPanel.getUserRoles() : new ArrayList<>());
    }
}
