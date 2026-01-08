package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReplaceRule {
    private String name;
    private boolean enabled;
    private final List<ReplaceOperation> operations;
    private String targetRole;

    public enum OperationType {
        REQUEST_STRING("Request String", true, true, true, true, true, false),
        REQUEST_HEADER("Request Header", true, false, true, true, true, true),
        REQUEST_BODY("Request Body", true, true, true, true, true, false),
        REQUEST_PARAM_NAME("Request Param Name", true, true, true, true, true, false),
        REQUEST_PARAM_VALUE("Request Param Value", true, true, true, true, true, false),
        REQUEST_COOKIE_NAME("Request Cookie Name", true, true, true, true, true, false),
        REQUEST_COOKIE_VALUE("Request Cookie Value", true, true, true, true, true, false),
        REMOVE_PARAMETER_BY_NAME("Remove Parameter By Name", true, true, false, false, true, false),
        REMOVE_PARAMETER_BY_VALUE("Remove Parameter By Value", true, true, false, false, true, false),
        REMOVE_COOKIE_BY_NAME("Remove Cookie By Name", true, true, false, false, true, false),
        REMOVE_COOKIE_BY_VALUE("Remove Cookie By Value", true, true, false, false, true, false),
        REMOVE_HEADER_BY_NAME("Remove Header By Name", true, true, false, false, true, false),
        REMOVE_HEADER_BY_VALUE("Remove Header By Value", true, true, false, false, true, false),
        MATCH_PARAM_NAME_REPLACE_VALUE("Match Param Name, Replace Value", true, true, true, true, true, false),
        MATCH_COOKIE_NAME_REPLACE_VALUE("Match Cookie Name, Replace Value", true, true, true, true, true, false),
        MATCH_HEADER_NAME_REPLACE_VALUE("Match Header Name, Replace Value", true, true, true, true, true, false);

        private final String label;
        private final boolean supportsMatch;
        private final boolean requiresMatch;
        private final boolean supportsReplace;
        private final boolean requiresReplace;
        private final boolean supportsRegex;
        private final boolean allowsBlankMatch;

        OperationType(String label,
                boolean supportsMatch,
                boolean requiresMatch,
                boolean supportsReplace,
                boolean requiresReplace,
                boolean supportsRegex,
                boolean allowsBlankMatch) {
            this.label = label;
            this.supportsMatch = supportsMatch;
            this.requiresMatch = requiresMatch;
            this.supportsReplace = supportsReplace;
            this.requiresReplace = requiresReplace;
            this.supportsRegex = supportsRegex;
            this.allowsBlankMatch = allowsBlankMatch;
        }

        public String getLabel() {
            return label;
        }

        public boolean supportsMatch() {
            return supportsMatch;
        }

        public boolean requiresMatch() {
            return requiresMatch;
        }

        public boolean supportsReplace() {
            return supportsReplace;
        }

        public boolean requiresReplace() {
            return requiresReplace;
        }

        public boolean supportsRegex() {
            return supportsRegex;
        }

        public boolean allowsBlankMatch() {
            return allowsBlankMatch;
        }
    }

    public ReplaceRule(String name) {
        this.name = name;
        this.enabled = true;
        this.operations = new ArrayList<>();
        this.targetRole = ""; // Initialized
    }

    // Getters and setters
    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ReplaceOperation> getOperations() {
        return operations.stream().map(ReplaceOperation::copy).toList();
    }

    public void setOperations(List<ReplaceOperation> newOperations) {
        operations.clear();
        if (newOperations != null) {
            newOperations.stream()
                    .filter(Objects::nonNull)
                    .map(ReplaceOperation::copy)
                    .forEach(operations::add);
        }
    }

    public boolean hasOperations() {
        return !operations.isEmpty();
    }

    public boolean hasWorkToDo() {
        return !operations.isEmpty() || (targetRole != null && !targetRole.isEmpty());
    }

    public String describeOperations() {
        if (operations.isEmpty()) {
            return "No operations";
        }
        if (operations.size() == 1) {
            return operations.get(0).describe();
        }
        return operations.size() + " operations";
    }

    public static class ReplaceOperation {
        private OperationType type;
        private String matchPattern;
        private String replaceValue;
        private boolean useRegex;

        public ReplaceOperation(OperationType type, String matchPattern, String replaceValue, boolean useRegex) {
            this.type = Objects.requireNonNull(type);
            this.matchPattern = matchPattern == null ? "" : matchPattern;
            this.replaceValue = replaceValue == null ? "" : replaceValue;
            this.useRegex = useRegex;
        }

        public OperationType getType() {
            return type;
        }

        public void setType(OperationType type) {
            this.type = Objects.requireNonNull(type);
        }

        public String getMatchPattern() {
            return matchPattern;
        }

        public void setMatchPattern(String matchPattern) {
            this.matchPattern = matchPattern == null ? "" : matchPattern;
        }

        public String getReplaceValue() {
            return replaceValue;
        }

        public void setReplaceValue(String replaceValue) {
            this.replaceValue = replaceValue == null ? "" : replaceValue;
        }

        public boolean isUseRegex() {
            return useRegex;
        }

        public void setUseRegex(boolean useRegex) {
            this.useRegex = useRegex;
        }

        public ReplaceOperation copy() {
            return new ReplaceOperation(type, matchPattern, replaceValue, useRegex);
        }

        public String describe() {
            String pattern = matchPattern == null || matchPattern.isEmpty() ? "<empty>" : matchPattern;
            if (type.supportsReplace()) {
                String replacement = replaceValue == null || replaceValue.isEmpty() ? "<empty>" : replaceValue;
                return type.getLabel() + " | match: '" + pattern + "' → replace: '" + replacement + "'";
            }
            return type.getLabel() + " | match: '" + pattern + "'";
        }
    }
}
