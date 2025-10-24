package model;

import burp.api.montoya.core.ToolType;

public class ExtensionConfig {
    private boolean extensionEnabled;
    private boolean onlyInScope;
    private boolean interceptEnabled;
    private boolean autoModifyRequests;
    private boolean unauthenticatedTesting;
    private boolean applyRulesToUnauthenticatedRequest;
    private boolean excludeStaticFiles;

    // Per-tool application flags
    private boolean applyToProxy;
    private boolean applyToRepeater;
    private boolean applyToIntruder;
    private boolean applyToScanner;

    // Preview mode for Proxy: don't modify traffic, but compute and show diffs
    private boolean previewInProxy;
    private int maxLogEntries;

    public ExtensionConfig() {
    this.extensionEnabled = false;
        this.onlyInScope = false;
        this.interceptEnabled = true;
    this.autoModifyRequests = true;
    this.unauthenticatedTesting = false;
    this.applyRulesToUnauthenticatedRequest = false;
    this.excludeStaticFiles = true;

        // Sensible defaults: don't affect browser traffic by default
        this.applyToProxy = false;
        this.applyToRepeater = true;
        this.applyToIntruder = true;
        this.applyToScanner = false;
        this.previewInProxy = true;
        this.maxLogEntries = 1000;
    }

    public boolean isOnlyInScope() {
        return onlyInScope;
    }

    public void setOnlyInScope(boolean onlyInScope) {
        this.onlyInScope = onlyInScope;
    }

    public boolean isInterceptEnabled() {
        return interceptEnabled;
    }

    public void setInterceptEnabled(boolean interceptEnabled) {
        this.interceptEnabled = interceptEnabled;
    }

    public boolean isAutoModifyRequests() {
        return autoModifyRequests;
    }

    public void setAutoModifyRequests(boolean autoModifyRequests) {
        this.autoModifyRequests = autoModifyRequests;
    }

    public boolean isUnauthenticatedTesting() {
        return unauthenticatedTesting;
    }

    public void setUnauthenticatedTesting(boolean unauthenticatedTesting) {
        this.unauthenticatedTesting = unauthenticatedTesting;
    }

    public boolean isApplyRulesToUnauthenticatedRequest() {
        return applyRulesToUnauthenticatedRequest;
    }

    public void setApplyRulesToUnauthenticatedRequest(boolean applyRulesToUnauthenticatedRequest) {
        this.applyRulesToUnauthenticatedRequest = applyRulesToUnauthenticatedRequest;
    }

    public boolean isApplyToProxy() {
        return applyToProxy;
    }

    public void setApplyToProxy(boolean applyToProxy) {
        this.applyToProxy = applyToProxy;
    }

    public boolean isApplyToRepeater() {
        return applyToRepeater;
    }

    public void setApplyToRepeater(boolean applyToRepeater) {
        this.applyToRepeater = applyToRepeater;
    }

    public boolean isApplyToIntruder() {
        return applyToIntruder;
    }

    public void setApplyToIntruder(boolean applyToIntruder) {
        this.applyToIntruder = applyToIntruder;
    }

    public boolean isApplyToScanner() {
        return applyToScanner;
    }

    public void setApplyToScanner(boolean applyToScanner) {
        this.applyToScanner = applyToScanner;
    }

    public boolean isPreviewInProxy() {
        return previewInProxy;
    }

    public void setPreviewInProxy(boolean previewInProxy) {
        this.previewInProxy = previewInProxy;
    }

    public boolean isExtensionEnabled() {
        return extensionEnabled;
    }

    public void setExtensionEnabled(boolean extensionEnabled) {
        this.extensionEnabled = extensionEnabled;
    }

    public int getMaxLogEntries() {
        return maxLogEntries;
    }

    public void setMaxLogEntries(int maxLogEntries) {
        this.maxLogEntries = Math.max(100, maxLogEntries);
    }

    public boolean isExcludeStaticFiles() {
        return excludeStaticFiles;
    }

    public void setExcludeStaticFiles(boolean excludeStaticFiles) {
        this.excludeStaticFiles = excludeStaticFiles;
    }

    // Helper: should we apply to a given tool?
    public boolean isToolEnabled(ToolType toolType) {
        if (toolType == null) return false;
        return switch (toolType) {
            case PROXY -> applyToProxy;
            case REPEATER -> applyToRepeater;
            case INTRUDER -> applyToIntruder;
            case SCANNER -> applyToScanner;
            default -> false; // conservative default for other tools
        };
    }
}
