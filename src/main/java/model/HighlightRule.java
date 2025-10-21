package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HighlightRule {
    private String name;
    private Color color;
    private boolean enabled;
    private LogicalOperator logicalOperator;
    private final List<HighlightCondition> conditions;

    public enum LogicalOperator {
        ALL("All conditions"),
        ANY("Any condition");

        private final String label;

        LogicalOperator(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public HighlightRule(String name, Color color) {
        this.name = name;
        this.color = color == null ? Color.YELLOW : color;
        this.enabled = true;
        this.logicalOperator = LogicalOperator.ALL;
        this.conditions = new ArrayList<>();
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator == null ? LogicalOperator.ALL : logicalOperator;
    }

    public List<HighlightCondition> getConditions() {
        return new ArrayList<>(conditions);
    }

    public void setConditions(List<HighlightCondition> newConditions) {
        conditions.clear();
        if (newConditions != null) {
            newConditions.stream()
                    .filter(Objects::nonNull)
                    .map(HighlightCondition::copy)
                    .forEach(conditions::add);
        }
    }

    public boolean hasConditions() {
        return !conditions.isEmpty();
    }

    public String describeCriteria() {
        if (hasConditions()) {
            List<HighlightCondition> snapshot = getConditions();
            if (snapshot.isEmpty()) {
                return "No criteria";
            }
            HighlightCondition first = snapshot.get(0);
            if (snapshot.size() == 1) {
                return first.toString();
            }
            return logicalOperator.getLabel() + " (" + snapshot.size() + " conditions)";
        }
        return "No criteria";
    }

    public boolean matches(RequestLogEntry entry) {
        if (!enabled) {
            return false;
        }
        if (!hasConditions()) {
            return false;
        }
        return evaluateAdvanced(entry);
    }

    private boolean evaluateAdvanced(RequestLogEntry entry) {
        if (logicalOperator == LogicalOperator.ALL) {
            for (HighlightCondition condition : conditions) {
                if (!condition.matches(entry)) {
                    return false;
                }
            }
            return !conditions.isEmpty();
        }
        for (HighlightCondition condition : conditions) {
            if (condition.matches(entry)) {
                return true;
            }
        }
        return false;
    }
}
