package util;

import model.AuthToken;
import model.ExtensionConfig;
import model.ExtensionState;
import model.HighlightCondition;
import model.HighlightRule;
import model.ReplaceRule;
import model.UserRole;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonUtil {
    private JsonUtil() {
    }

    public static JSONArray rulesToJson(List<ReplaceRule> rules) {
        JSONArray array = new JSONArray();
        if (rules == null) {
            return array;
        }
        for (ReplaceRule rule : rules) {
            if (rule == null) {
                continue;
            }
            JSONObject ruleObj = new JSONObject();
            ruleObj.putValue("name", rule.getName());
            ruleObj.putValue("enabled", rule.isEnabled());

            JSONArray operations = new JSONArray();
            for (ReplaceRule.ReplaceOperation operation : rule.getOperations()) {
                operations.put(replaceOperationToJson(operation));
            }
            ruleObj.putValue("operations", operations);
            array.put(ruleObj);
        }
        return array;
    }

    public static List<ReplaceRule> rulesFromJson(JSONArray array) {
        List<ReplaceRule> rules = new ArrayList<>();
        if (array == null) {
            return rules;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }
            ReplaceRule rule = new ReplaceRule(obj.optString("name", ""));
            rule.setEnabled(obj.optBoolean("enabled", true));

            JSONArray operations = obj.optJSONArray("operations");
            List<ReplaceRule.ReplaceOperation> operationList = new ArrayList<>();
            if (operations != null) {
                for (int j = 0; j < operations.length(); j++) {
                    ReplaceRule.ReplaceOperation op = replaceOperationFromJson(operations.optJSONObject(j));
                    if (op != null) {
                        operationList.add(op);
                    }
                }
            } else {
                ReplaceRule.ReplaceOperation legacyOp = legacyReplaceOperationFromJson(obj);
                if (legacyOp != null) {
                    operationList.add(legacyOp);
                }
            }
            rule.setOperations(operationList);
            rules.add(rule);
        }
        return rules;
    }

    public static JSONArray userRolesToJson(List<UserRole> roles) {
        JSONArray array = new JSONArray();
        if (roles == null) {
            return array;
        }
        for (UserRole role : roles) {
            JSONObject obj = new JSONObject();
            obj.putValue("name", role.getName());
            obj.putValue("enabled", role.isEnabled());

            JSONArray tokens = new JSONArray();
            for (AuthToken token : role.getTokens()) {
                tokens.put(authTokenToJson(token));
            }
            obj.putValue("tokens", tokens);
            array.put(obj);
        }
        return array;
    }

    public static List<UserRole> userRolesFromJson(JSONArray array) {
        List<UserRole> roles = new ArrayList<>();
        if (array == null) {
            return roles;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null)
                continue;

            UserRole role = new UserRole(obj.optString("name", ""));
            role.setEnabled(obj.optBoolean("enabled", false));

            JSONArray tokens = obj.optJSONArray("tokens");
            if (tokens != null) {
                for (int j = 0; j < tokens.length(); j++) {
                    AuthToken token = authTokenFromJson(tokens.optJSONObject(j));
                    if (token != null) {
                        role.addToken(token);
                    }
                }
            }
            roles.add(role);
        }
        return roles;
    }

    private static JSONObject authTokenToJson(AuthToken token) {
        JSONObject obj = new JSONObject();
        obj.putValue("type", token.getType().name());
        obj.putValue("name", token.getName());
        obj.putValue("value", token.getValue());
        return obj;
    }

    private static AuthToken authTokenFromJson(JSONObject obj) {
        if (obj == null)
            return null;
        try {
            AuthToken.Type type = AuthToken.Type.valueOf(obj.optString("type", AuthToken.Type.HEADER.name()));
            return new AuthToken(type, obj.optString("name", ""), obj.optString("value", ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static JSONArray highlightRulesToJson(List<HighlightRule> rules) {
        JSONArray array = new JSONArray();
        if (rules == null) {
            return array;
        }
        for (HighlightRule rule : rules) {
            if (rule == null) {
                continue;
            }
            JSONObject obj = new JSONObject();
            obj.putValue("name", rule.getName());
            obj.putValue("enabled", rule.isEnabled());
            obj.putValue("color", colorToJson(rule.getColor()));
            obj.putValue("logic", rule.getLogicalOperator().name());

            JSONArray conditions = new JSONArray();
            for (HighlightCondition condition : rule.getConditions()) {
                conditions.put(conditionToJson(condition));
            }
            obj.putValue("conditions", conditions);
            array.put(obj);
        }
        return array;
    }

    public static List<HighlightRule> highlightRulesFromJson(JSONArray array) {
        List<HighlightRule> rules = new ArrayList<>();
        if (array == null) {
            return rules;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }
            Color color = colorFromJson(obj.optJSONObject("color"));
            HighlightRule rule = new HighlightRule(
                    obj.optString("name", ""),
                    color != null ? color : Color.YELLOW);
            rule.setEnabled(obj.optBoolean("enabled", true));

            String logic = obj.optString("logic", HighlightRule.LogicalOperator.ALL.name());
            try {
                rule.setLogicalOperator(HighlightRule.LogicalOperator.valueOf(logic));
            } catch (IllegalArgumentException ignored) {
                rule.setLogicalOperator(HighlightRule.LogicalOperator.ALL);
            }

            JSONArray conditions = obj.optJSONArray("conditions");
            List<HighlightCondition> parsedConditions = new ArrayList<>();
            if (conditions != null && conditions.length() > 0) {
                for (int j = 0; j < conditions.length(); j++) {
                    HighlightCondition condition = conditionFromJson(conditions.optJSONObject(j));
                    if (condition != null) {
                        parsedConditions.add(condition);
                    }
                }
            } else {
                parsedConditions.addAll(legacyHighlightConditionsFromJson(obj));
            }
            rule.setConditions(parsedConditions);
            rules.add(rule);
        }
        return rules;
    }

    public static JSONObject configToJson(ExtensionConfig config) {
        JSONObject obj = new JSONObject();
        obj.putValue("extensionEnabled", config.isExtensionEnabled());
        obj.putValue("onlyInScope", config.isOnlyInScope());
        obj.putValue("interceptEnabled", config.isInterceptEnabled());
        obj.putValue("autoModifyRequests", config.isAutoModifyRequests());
        obj.putValue("unauthenticatedTesting", config.isUnauthenticatedTesting());
        obj.putValue("applyRulesToUnauthenticatedRequest", config.isApplyRulesToUnauthenticatedRequest());
        obj.putValue("applyToProxy", config.isApplyToProxy());
        obj.putValue("applyToRepeater", config.isApplyToRepeater());
        obj.putValue("applyToIntruder", config.isApplyToIntruder());
        obj.putValue("applyToScanner", config.isApplyToScanner());
        obj.putValue("previewInProxy", config.isPreviewInProxy());
        obj.putValue("maxLogEntries", config.getMaxLogEntries());
        return obj;
    }

    public static void configFromJson(JSONObject obj, ExtensionConfig config) {
        if (obj == null || config == null) {
            return;
        }
        config.setExtensionEnabled(obj.optBoolean("extensionEnabled", true));
        config.setOnlyInScope(obj.optBoolean("onlyInScope", false));
        config.setInterceptEnabled(obj.optBoolean("interceptEnabled", true));
        config.setAutoModifyRequests(obj.optBoolean("autoModifyRequests", true));
        config.setUnauthenticatedTesting(obj.optBoolean("unauthenticatedTesting", false));
        config.setApplyRulesToUnauthenticatedRequest(obj.optBoolean("applyRulesToUnauthenticatedRequest", false));
        config.setApplyToProxy(obj.optBoolean("applyToProxy", false));
        config.setApplyToRepeater(obj.optBoolean("applyToRepeater", true));
        config.setApplyToIntruder(obj.optBoolean("applyToIntruder", true));
        config.setApplyToScanner(obj.optBoolean("applyToScanner", false));
        config.setPreviewInProxy(obj.optBoolean("previewInProxy", true));
        config.setMaxLogEntries(obj.optInt("maxLogEntries", config.getMaxLogEntries()));
    }

    public static JSONObject stateToJson(ExtensionConfig config,
            List<ReplaceRule> replaceRules,
            List<HighlightRule> highlightRules,
            List<UserRole> userRoles) {
        JSONObject root = new JSONObject();
        root.putValue("config", configToJson(config));
        root.putValue("replaceRules", rulesToJson(replaceRules));
        root.putValue("highlightRules", highlightRulesToJson(highlightRules));
        root.putValue("userRoles", userRolesToJson(userRoles));
        return root;
    }

    public static ExtensionState stateFromJson(JSONObject obj, ExtensionConfig config) {
        if (obj == null) {
            return new ExtensionState();
        }
        configFromJson(obj.optJSONObject("config"), config);
        List<ReplaceRule> replaceRules = rulesFromJson(obj.optJSONArray("replaceRules"));
        List<HighlightRule> highlightRules = highlightRulesFromJson(obj.optJSONArray("highlightRules"));
        List<UserRole> userRoles = userRolesFromJson(obj.optJSONArray("userRoles"));
        return new ExtensionState(replaceRules, highlightRules, userRoles);
    }

    private static JSONObject replaceOperationToJson(ReplaceRule.ReplaceOperation operation) {
        JSONObject obj = new JSONObject();
        obj.putValue("type", operation.getType().name());
        obj.putValue("matchPattern", operation.getMatchPattern());
        obj.putValue("replaceValue", operation.getReplaceValue());
        obj.putValue("useRegex", operation.isUseRegex());
        return obj;
    }

    private static ReplaceRule.ReplaceOperation replaceOperationFromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        ReplaceRule.OperationType type = parseOperationType(
                obj.optString("type", null),
                obj.optString("target", obj.optString("legacyTarget", "")));
        return new ReplaceRule.ReplaceOperation(
                type,
                obj.optString("matchPattern", ""),
                obj.optString("replaceValue", ""),
                obj.optBoolean("useRegex", false));
    }

    private static ReplaceRule.ReplaceOperation legacyReplaceOperationFromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        String match = obj.optString("matchPattern", "");
        if (match == null || match.isEmpty()) {
            return null;
        }
        ReplaceRule.OperationType type = parseOperationType(null, obj.optString("target", ""));
        return new ReplaceRule.ReplaceOperation(
                type,
                match,
                obj.optString("replaceValue", ""),
                obj.optBoolean("useRegex", false));
    }

    private static JSONObject colorToJson(Color color) {
        JSONObject obj = new JSONObject();
        obj.putValue("r", color.getRed());
        obj.putValue("g", color.getGreen());
        obj.putValue("b", color.getBlue());
        obj.putValue("a", color.getAlpha());
        return obj;
    }

    private static Color colorFromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        int r = obj.optInt("r", 255);
        int g = obj.optInt("g", 255);
        int b = obj.optInt("b", 0);
        int a = obj.optInt("a", 255);
        return new Color(r, g, b, a);
    }

    private static JSONObject conditionToJson(HighlightCondition condition) {
        JSONObject obj = new JSONObject();
        obj.putValue("messageVersion", condition.getMessageVersion().name());
        obj.putValue("matchPart", condition.getMatchPart().name());
        obj.putValue("relationship", condition.getRelationship().name());
        obj.putValue("matchValue", condition.getMatchValue());
        return obj;
    }

    private static HighlightCondition conditionFromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        try {
            HighlightCondition.MessageVersion messageVersion = HighlightCondition.MessageVersion.valueOf(
                    obj.optString("messageVersion", HighlightCondition.MessageVersion.ORIGINAL.name()));
            HighlightCondition.MatchPart matchPart = parseMatchPart(
                    obj.optString("matchPart", null),
                    obj.optString("target", obj.optString("legacyTarget", "")));
            HighlightCondition.Relationship relationship = HighlightCondition.Relationship.valueOf(
                    obj.optString("relationship", HighlightCondition.Relationship.CONTAINS.name()));
            String value = obj.optString("matchValue", "");
            return new HighlightCondition(messageVersion, matchPart, relationship, value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ReplaceRule.OperationType parseOperationType(String typeName, String legacyTarget) {
        if (typeName != null && !typeName.isEmpty()) {
            try {
                return ReplaceRule.OperationType.valueOf(typeName);
            } catch (IllegalArgumentException ignored) {
                // Fall back to legacy mapping
            }
        }

        if (legacyTarget == null) {
            return ReplaceRule.OperationType.REQUEST_STRING;
        }

        return switch (legacyTarget) {
            case "STRING" -> ReplaceRule.OperationType.REQUEST_STRING;
            case "BODY" -> ReplaceRule.OperationType.REQUEST_BODY;
            case "HEADER" -> ReplaceRule.OperationType.REQUEST_HEADER;
            case "PARAMETER" -> ReplaceRule.OperationType.MATCH_PARAM_NAME_REPLACE_VALUE;
            case "COOKIE" -> ReplaceRule.OperationType.MATCH_COOKIE_NAME_REPLACE_VALUE;
            default -> ReplaceRule.OperationType.REQUEST_STRING;
        };
    }

    private static HighlightCondition.MatchPart parseMatchPart(String matchPartName, String legacyTarget) {
        if (matchPartName != null && !matchPartName.isEmpty()) {
            try {
                return HighlightCondition.MatchPart.valueOf(matchPartName);
            } catch (IllegalArgumentException ignored) {
                // Fall back to legacy mapping
            }
        }

        if (legacyTarget == null) {
            return HighlightCondition.MatchPart.STRING_IN_REQUEST;
        }

        return switch (legacyTarget) {
            case "STATUS_CODE" -> HighlightCondition.MatchPart.STATUS_CODE;
            case "RESPONSE_STRING" -> HighlightCondition.MatchPart.STRING_IN_RESPONSE;
            case "COOKIE", "PARAMETER" -> HighlightCondition.MatchPart.STRING_IN_REQUEST;
            default -> HighlightCondition.MatchPart.STRING_IN_REQUEST;
        };
    }

    private static List<HighlightCondition> legacyHighlightConditionsFromJson(JSONObject obj) {
        if (obj == null) {
            return Collections.emptyList();
        }
        String pattern = obj.optString("matchPattern", "");
        if (pattern == null || pattern.isEmpty()) {
            return Collections.emptyList();
        }
        String legacyTarget = obj.optString("target", "");
        boolean useRegex = obj.optBoolean("useRegex", false);

        HighlightCondition.Relationship relationship = useRegex
                ? HighlightCondition.Relationship.MATCHES_REGEX
                : HighlightCondition.Relationship.CONTAINS;

        if ("STATUS_CODE".equals(legacyTarget)) {
            relationship = useRegex
                    ? HighlightCondition.Relationship.MATCHES_REGEX
                    : HighlightCondition.Relationship.EQUALS;
            return List.of(new HighlightCondition(
                    HighlightCondition.MessageVersion.ANY,
                    HighlightCondition.MatchPart.STATUS_CODE,
                    relationship,
                    pattern));
        }

        if ("RESPONSE_STRING".equals(legacyTarget)) {
            return List.of(new HighlightCondition(
                    HighlightCondition.MessageVersion.ANY,
                    HighlightCondition.MatchPart.STRING_IN_RESPONSE,
                    relationship,
                    pattern));
        }

        return List.of(new HighlightCondition(
                HighlightCondition.MessageVersion.ANY,
                HighlightCondition.MatchPart.STRING_IN_REQUEST,
                relationship,
                pattern));
    }
}
