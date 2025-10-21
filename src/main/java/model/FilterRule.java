package model;

import java.util.function.Predicate;

public class FilterRule {
    private final String name;
    private final Predicate<RequestLogEntry> predicate;
    private boolean enabled;

    public FilterRule(String name, Predicate<RequestLogEntry> predicate) {
        this.name = name;
        this.predicate = predicate;
        this.enabled = true;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean matches(RequestLogEntry entry) {
        return !enabled || predicate.test(entry);
    }
}
