package model;

import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class HighlightCondition {
    public enum MessageVersion {
        ORIGINAL("Original"),
        MODIFIED("Modified"),
        ANY("Any");

        private final String label;

        MessageVersion(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum MatchPart {
        STRING_IN_REQUEST("String In Request"),
        STRING_IN_RESPONSE("String In Response"),
        REQUEST_LENGTH("Request Length"),
        RESPONSE_LENGTH("Response Length"),
        URL("URL"),
        STATUS_CODE("Status Code"),
        DOMAIN_NAME("Domain Name"),
        PROTOCOL("Protocol"),
        HTTP_METHOD("HTTP Method"),
        FILE_EXTENSION("File Extension");

        private final String label;

        MatchPart(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public boolean supportsNumericComparison() {
            return this == REQUEST_LENGTH || this == RESPONSE_LENGTH || this == STATUS_CODE;
        }
    }

    public enum Relationship {
        CONTAINS("contains"),
        NOT_CONTAINS("does not contain"),
        EQUALS("equals"),
        NOT_EQUALS("does not equal"),
        GREATER_THAN("is greater than"),
        LESS_THAN("is less than"),
        MATCHES_REGEX("matches regex"),
        NOT_MATCHES_REGEX("does not match regex");

        private final String label;

        Relationship(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public boolean isRegex() {
            return this == MATCHES_REGEX || this == NOT_MATCHES_REGEX;
        }

        public boolean requiresNumericComparison() {
            return this == GREATER_THAN || this == LESS_THAN;
        }
    }

    private MessageVersion messageVersion;
    private MatchPart matchPart;
    private Relationship relationship;
    private String matchValue;

    public HighlightCondition() {
        this(MessageVersion.ORIGINAL, MatchPart.STRING_IN_REQUEST, Relationship.CONTAINS, "");
    }

    public HighlightCondition(MessageVersion messageVersion,
                              MatchPart matchPart,
                              Relationship relationship,
                              String matchValue) {
        this.messageVersion = Objects.requireNonNull(messageVersion);
        this.matchPart = Objects.requireNonNull(matchPart);
        this.relationship = Objects.requireNonNull(relationship);
        this.matchValue = matchValue == null ? "" : matchValue;
    }

    public MessageVersion getMessageVersion() {
        return messageVersion;
    }

    public void setMessageVersion(MessageVersion messageVersion) {
        this.messageVersion = Objects.requireNonNull(messageVersion);
    }

    public MatchPart getMatchPart() {
        return matchPart;
    }

    public void setMatchPart(MatchPart matchPart) {
        this.matchPart = Objects.requireNonNull(matchPart);
    }

    public Relationship getRelationship() {
        return relationship;
    }

    public void setRelationship(Relationship relationship) {
        this.relationship = Objects.requireNonNull(relationship);
    }

    public String getMatchValue() {
        return matchValue;
    }

    public void setMatchValue(String matchValue) {
        this.matchValue = matchValue == null ? "" : matchValue;
    }

    public HighlightCondition copy() {
        return new HighlightCondition(messageVersion, matchPart, relationship, matchValue);
    }

    public boolean matches(RequestLogEntry entry) {
        return switch (matchPart) {
            case STRING_IN_REQUEST, REQUEST_LENGTH, URL, DOMAIN_NAME, PROTOCOL, HTTP_METHOD, FILE_EXTENSION ->
                    matchesRequest(entry, this::extractRequestValues);
            case STRING_IN_RESPONSE, RESPONSE_LENGTH, STATUS_CODE ->
                    matchesResponse(entry, this::extractResponseValues);
        };
    }

    private boolean matchesRequest(RequestLogEntry entry, Function<HttpRequest, List<String>> extractor) {
        HttpRequest original = entry.getOriginalRequest();
        HttpRequest modified = entry.getModifiedRequest();
        return evaluateWithVersion(original, modified, extractor);
    }

    private boolean matchesResponse(RequestLogEntry entry, Function<HttpResponse, List<String>> extractor) {
        HttpResponse original = entry.getOriginalResponse();
        HttpResponse modified = entry.getModifiedResponse();
        return evaluateWithVersion(original, modified, extractor);
    }

    private <T> boolean evaluateWithVersion(T original,
                                            T modified,
                                            Function<T, List<String>> extractor) {
        boolean originalMatched = false;

        if (messageVersion == MessageVersion.ORIGINAL || messageVersion == MessageVersion.ANY) {
            if (original != null) {
                originalMatched = evaluateAgainstStrings(extractor.apply(original));
            }
            if (messageVersion == MessageVersion.ORIGINAL) {
                return originalMatched;
            }
            if (originalMatched && messageVersion == MessageVersion.ANY) {
                return true;
            }
        }

        if (messageVersion == MessageVersion.MODIFIED || messageVersion == MessageVersion.ANY) {
            if (modified != null) {
                boolean modifiedMatched = evaluateAgainstStrings(extractor.apply(modified));
                if (messageVersion == MessageVersion.MODIFIED) {
                    return modifiedMatched;
                }
                if (modifiedMatched) {
                    return true;
                }
            } else if (messageVersion == MessageVersion.MODIFIED) {
                return false;
            }
        }

        return messageVersion == MessageVersion.ANY && originalMatched;
    }

    private List<String> extractRequestValues(HttpRequest request) {
        return switch (matchPart) {
            case STRING_IN_REQUEST -> Collections.singletonList(request.toString());
            case REQUEST_LENGTH -> Collections.singletonList(String.valueOf(request.toString().length()));
            case URL -> Collections.singletonList(request.url());
            case DOMAIN_NAME -> Collections.singletonList(resolveDomain(request));
            case PROTOCOL -> Collections.singletonList(resolveProtocol(request.httpService()));
            case HTTP_METHOD -> Collections.singletonList(request.method());
            case FILE_EXTENSION -> Collections.singletonList(normalizeFileExtension(request.fileExtension()));
            default -> Collections.emptyList();
        };
    }

    private List<String> extractResponseValues(HttpResponse response) {
        return switch (matchPart) {
            case STRING_IN_RESPONSE -> Collections.singletonList(response.bodyToString());
            case RESPONSE_LENGTH -> Collections.singletonList(String.valueOf(response.bodyToString().length()));
            case STATUS_CODE -> Collections.singletonList(String.valueOf(response.statusCode()));
            default -> Collections.emptyList();
        };
    }

    private String resolveDomain(HttpRequest request) {
        try {
            URI uri = new URI(request.url());
            if (uri.getHost() != null) {
                return uri.getHost();
            }
        } catch (URISyntaxException ignored) {
            // Fall through to service host
        }
        HttpService service = request.httpService();
        return service != null ? service.host() : "";
    }

    private String resolveProtocol(HttpService service) {
        if (service == null) {
            return "";
        }
        return service.secure() ? "https" : "http";
    }

    private String normalizeFileExtension(String extension) {
        if (extension == null) {
            return "";
        }
        return extension.startsWith(".") ? extension.substring(1) : extension;
    }

    private boolean evaluateAgainstStrings(List<String> values) {
        if (values.isEmpty()) {
            return evaluateEmptyCollection();
        }
        return switch (relationship) {
            case CONTAINS -> values.stream().anyMatch(this::containsValue);
            case NOT_CONTAINS -> values.stream().noneMatch(this::containsValue);
            case EQUALS -> values.stream().anyMatch(this::equalsValue);
            case NOT_EQUALS -> values.stream().noneMatch(this::equalsValue);
            case GREATER_THAN -> values.stream().anyMatch(this::greaterThanValue);
            case LESS_THAN -> values.stream().anyMatch(this::lessThanValue);
            case MATCHES_REGEX -> values.stream().anyMatch(this::matchesRegex);
            case NOT_MATCHES_REGEX -> values.stream().noneMatch(this::matchesRegex);
        };
    }

    private boolean evaluateEmptyCollection() {
        return switch (relationship) {
            case CONTAINS, EQUALS, GREATER_THAN, LESS_THAN, MATCHES_REGEX -> false;
            case NOT_CONTAINS, NOT_EQUALS, NOT_MATCHES_REGEX -> true;
        };
    }

    private boolean containsValue(String candidate) {
        return normalize(candidate).contains(normalize(matchValue));
    }

    private boolean equalsValue(String candidate) {
        return normalize(candidate).equals(normalize(matchValue));
    }

    private boolean matchesRegex(String candidate) {
        try {
            Pattern pattern = Pattern.compile(matchValue, Pattern.DOTALL);
            return pattern.matcher(normalize(candidate)).find();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    private boolean greaterThanValue(String candidate) {
        Double candidateValue = parseNumeric(candidate);
        Double targetValue = parseNumeric(matchValue);
        if (candidateValue == null || targetValue == null) {
            return false;
        }
        return candidateValue > targetValue;
    }

    private boolean lessThanValue(String candidate) {
        Double candidateValue = parseNumeric(candidate);
        Double targetValue = parseNumeric(matchValue);
        if (candidateValue == null || targetValue == null) {
            return false;
        }
        return candidateValue < targetValue;
    }

    private String normalize(String input) {
        return input == null ? "" : input;
    }

    private Double parseNumeric(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public String toString() {
        return messageVersion.getLabel() + " " + matchPart.getLabel() + " " + relationship.getLabel() + " '" + matchValue + "'";
    }
}
