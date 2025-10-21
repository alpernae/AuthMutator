package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates persistent state for replace and highlight rules so we can load/save easily.
 */
public class ExtensionState {
    private final List<ReplaceRule> replaceRules;
    private final List<HighlightRule> highlightRules;

    public ExtensionState() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    public ExtensionState(List<ReplaceRule> replaceRules, List<HighlightRule> highlightRules) {
        this.replaceRules = new ArrayList<>(replaceRules);
        this.highlightRules = new ArrayList<>(highlightRules);
    }

    public List<ReplaceRule> getReplaceRules() {
        return new ArrayList<>(replaceRules);
    }

    public List<HighlightRule> getHighlightRules() {
        return new ArrayList<>(highlightRules);
    }
}
