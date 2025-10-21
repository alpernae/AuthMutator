package handler;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.core.ToolType;
import model.ExtensionConfig;
import model.RequestLogEntry;
import model.RequestLogModel;
import model.ReplaceRule;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RequestHandler implements HttpHandler {
    private final MontoyaApi api;
    private final RequestLogModel requestLogModel;
    private final ExtensionConfig config;
    private final AtomicInteger requestCounter;
    private List<ReplaceRule> replaceRules;
    private final ConcurrentHashMap<Integer, Pending> pendingByMessageId = new ConcurrentHashMap<>();
    private final ExecutorService previewExecutor;
    private final AtomicInteger previewThreadCounter = new AtomicInteger(1);

    private static class Pending {
        final HttpRequest original;
        final HttpRequest modified;
        final HttpRequest unauth;
        final boolean unauthTesting;
        final boolean preview;
        final boolean modifiedSent;
        volatile Integer tableEntryId;
        volatile HttpResponse previewModifiedResponse;
        volatile HttpResponse unauthResponse;
        volatile boolean awaitingModifiedPreviewResponse;
        volatile boolean awaitingUnauthResponse;

        Pending(HttpRequest original,
                HttpRequest modified,
                HttpRequest unauth,
                boolean unauthTesting,
                boolean preview,
                boolean modifiedSent) {
            this.original = original;
            this.modified = modified;
            this.unauth = unauth;
            this.unauthTesting = unauthTesting;
            this.preview = preview;
            this.modifiedSent = modifiedSent;
            this.awaitingModifiedPreviewResponse = preview && modified != null;
            this.awaitingUnauthResponse = unauth != null;
        }

        boolean hasModifiedChange() {
            return modified != null && !modified.toString().equals(original.toString());
        }

        boolean hasUnauthVariant() {
            return unauth != null;
        }
    }

    public RequestHandler(MontoyaApi api, RequestLogModel requestLogModel, ExtensionConfig config) {
        this.api = api;
        this.requestLogModel = requestLogModel;
        this.config = config;
        this.requestCounter = new AtomicInteger(1);
        this.replaceRules = List.of();
        this.previewExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "idor-auth-preview-" + previewThreadCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    public void setReplaceRules(List<ReplaceRule> rules) {
        this.replaceRules = List.copyOf(rules);
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        // Check if extension is enabled
        if (!config.isExtensionEnabled()) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }
        
        // Check if we should process this request
        if (!config.isInterceptEnabled()) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        // Respect per-tool settings
        ToolType toolType = requestToBeSent.toolSource() != null ? requestToBeSent.toolSource().toolType() : null;
        boolean toolEnabled = config.isToolEnabled(toolType);

        // Proxy preview: if tool is PROXY and not enabled but preview is on, we compute diffs without modifying
        boolean proxyPreview = (toolType == ToolType.PROXY) && !toolEnabled && config.isPreviewInProxy();

        // Check scope if enabled
        if (config.isOnlyInScope() && !api.scope().isInScope(requestToBeSent.url())) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        int messageId = requestToBeSent.messageId();
        HttpRequest originalSnapshot = HttpRequest.httpRequest(
                requestToBeSent.httpService(),
                requestToBeSent.toString()
        );

        boolean transformsAllowed = toolEnabled || proxyPreview;
        boolean unauthTestingEnabled = transformsAllowed && config.isUnauthenticatedTesting();
        boolean shouldApplyRulesForMain = transformsAllowed && config.isAutoModifyRequests();
        boolean shouldApplyRulesForUnauth = unauthTestingEnabled && config.isApplyRulesToUnauthenticatedRequest();
        boolean shouldComputeRules = shouldApplyRulesForMain || shouldApplyRulesForUnauth || proxyPreview;

        HttpRequest modifiedRequest = null;
        if (shouldComputeRules) {
            HttpRequest candidate = applyReplaceRules(requestToBeSent);
            if (candidate != null && !candidate.toString().equals(requestToBeSent.toString())) {
                modifiedRequest = candidate;
            }
        }

        boolean modifiedRequestSent = shouldApplyRulesForMain && modifiedRequest != null;

        HttpRequest unauthRequest = null;
        if (unauthTestingEnabled) {
            HttpRequest baseForUnauth;
            if (config.isApplyRulesToUnauthenticatedRequest() && modifiedRequest != null) {
                baseForUnauth = modifiedRequest;
            } else {
                baseForUnauth = originalSnapshot;
            }
            HttpRequest stripped = stripCookies(baseForUnauth);
            if (!stripped.toString().equals(baseForUnauth.toString())) {
                unauthRequest = stripped;
            }
        }

        Pending pending = new Pending(
                originalSnapshot,
                modifiedRequest,
                unauthRequest,
                unauthTestingEnabled,
                proxyPreview,
                modifiedRequestSent
        );

        boolean shouldTrack = transformsAllowed && (pending.hasModifiedChange() || pending.hasUnauthVariant() || proxyPreview);

        if (proxyPreview && (pending.hasModifiedChange() || pending.hasUnauthVariant())) {
            int id = requestCounter.getAndIncrement();
            RequestLogEntry entry = new RequestLogEntry(
                    id,
                    originalSnapshot,
                    pending.hasModifiedChange() ? modifiedRequest : null,
                    pending.hasUnauthVariant() ? unauthRequest : null,
                    null,
                    null,
                    null,
                    false,
                    pending.unauthTesting
            );
            requestLogModel.addEntry(entry);
            pending.tableEntryId = id;
        }

        if (shouldTrack) {
            pendingByMessageId.put(messageId, pending);
        }

        if (pending.hasUnauthVariant()) {
            scheduleSyntheticRequest(pending, unauthRequest, ResponseVariant.UNAUTH);
        }

        if (proxyPreview && pending.hasModifiedChange()) {
            scheduleSyntheticRequest(pending, modifiedRequest, ResponseVariant.MODIFIED);
        }

        if (proxyPreview) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        if (modifiedRequestSent) {
            return RequestToBeSentAction.continueWith(modifiedRequest);
        }

        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    public void shutdown() {
        replaceRules = List.of();
        pendingByMessageId.clear();
        previewExecutor.shutdownNow();
        try {
            if (!previewExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                api.logging().logToOutput("Preview executor did not terminate cleanly within timeout.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // Check if extension is enabled
        if (!config.isExtensionEnabled()) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }
        
        // Check scope if enabled
        if (config.isOnlyInScope() && !api.scope().isInScope(responseReceived.initiatingRequest().url())) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        // Respect per-tool settings
        ToolType toolType = responseReceived.toolSource() != null ? responseReceived.toolSource().toolType() : null;
        boolean toolEnabled = config.isToolEnabled(toolType);
        boolean proxyPreview = (toolType == ToolType.PROXY) && !toolEnabled && config.isPreviewInProxy();

        int messageId = responseReceived.messageId();
        Pending pending = pendingByMessageId.remove(messageId);

        if (pending != null && (toolEnabled || proxyPreview)) {
            HttpResponse httpResponse = responseReceived;
            if (pending.preview) {
                if (pending.tableEntryId != null && pending.tableEntryId > 0) {
                    int entryId = pending.tableEntryId;
                    HttpResponse modifiedResponse = pending.previewModifiedResponse;
                    HttpResponse unauthResponse = pending.unauthResponse;
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        RequestLogEntry existing = requestLogModel.findById(entryId);
                        if (existing == null) {
                            return;
                        }
                        RequestLogEntry updated = new RequestLogEntry(
                                entryId,
                                pending.original,
                                pending.hasModifiedChange() ? pending.modified : null,
                                pending.hasUnauthVariant() ? pending.unauth : null,
                                httpResponse,
                                modifiedResponse != null ? modifiedResponse : existing.getModifiedResponse(),
                                unauthResponse != null ? unauthResponse : existing.getUnauthResponse(),
                                false,
                                pending.unauthTesting
                        );
                        requestLogModel.replaceById(entryId, updated);
                    });
                }
            } else {
                boolean shouldLog = pending.hasModifiedChange() || pending.hasUnauthVariant();
                if (shouldLog) {
                    int id = requestCounter.getAndIncrement();
                    HttpResponse originalResponse = pending.modifiedSent ? null : httpResponse;
                    HttpResponse modifiedResponse = pending.modifiedSent ? httpResponse : null;
                    RequestLogEntry entry = new RequestLogEntry(
                            id,
                            pending.original,
                            pending.hasModifiedChange() ? pending.modified : null,
                            pending.hasUnauthVariant() ? pending.unauth : null,
                            originalResponse,
                            modifiedResponse,
                            pending.unauthResponse,
                            pending.modifiedSent,
                            pending.unauthTesting
                    );
                    requestLogModel.addEntry(entry);
                    pending.tableEntryId = id;

                    if (pending.awaitingUnauthResponse && pending.unauthResponse != null) {
                        pending.awaitingUnauthResponse = false;
                    }
                }
            }
        }

        return ResponseReceivedAction.continueWith(responseReceived);
    }

    private void scheduleSyntheticRequest(Pending pending, HttpRequest request, ResponseVariant variant) {
        if (request == null) {
            return;
        }
        try {
            previewExecutor.submit(() -> {
                try {
                    var requestResponse = api.http().sendRequest(request);
                    HttpResponse response = requestResponse != null ? requestResponse.response() : null;
                    if (response == null) {
                        markVariantComplete(pending, variant);
                        return;
                    }

                    if (variant == ResponseVariant.MODIFIED) {
                        pending.previewModifiedResponse = response;
                        pending.awaitingModifiedPreviewResponse = false;
                    } else {
                        pending.unauthResponse = response;
                        pending.awaitingUnauthResponse = false;
                    }

                    Integer entryId = pending.tableEntryId;
                    if (entryId != null && entryId > 0) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            updateEntryResponses(
                                    pending,
                                    variant == ResponseVariant.MODIFIED ? response : null,
                                    variant == ResponseVariant.UNAUTH ? response : null
                            );
                        });
                    }
                } catch (Exception ex) {
                    api.logging().logToError("Synthetic send failed: " + ex.getMessage());
                    markVariantComplete(pending, variant);
                }
            });
        } catch (RejectedExecutionException reject) {
            api.logging().logToError("Preview executor rejected task: " + reject.getMessage());
            markVariantComplete(pending, variant);
        }
    }

    private void updateEntryResponses(Pending pending, HttpResponse newModifiedResponse, HttpResponse newUnauthResponse) {
        Integer entryId = pending.tableEntryId;
        if (entryId == null || entryId <= 0) {
            return;
        }
        RequestLogEntry existing = requestLogModel.findById(entryId);
        if (existing == null) {
            return;
        }

        RequestLogEntry updated = new RequestLogEntry(
                entryId,
                existing.getOriginalRequest() != null ? existing.getOriginalRequest() : pending.original,
                existing.getModifiedRequest() != null ? existing.getModifiedRequest() : (pending.hasModifiedChange() ? pending.modified : null),
                existing.getUnauthRequest() != null ? existing.getUnauthRequest() : (pending.hasUnauthVariant() ? pending.unauth : null),
                existing.getOriginalResponse(),
                newModifiedResponse != null ? newModifiedResponse : existing.getModifiedResponse(),
                newUnauthResponse != null ? newUnauthResponse : existing.getUnauthResponse(),
                existing.wasModifiedRequestSent(),
                existing.isUnauthenticatedTesting() || pending.unauthTesting
        );
        requestLogModel.replaceById(entryId, updated);
    }

    private void markVariantComplete(Pending pending, ResponseVariant variant) {
        if (variant == ResponseVariant.MODIFIED) {
            pending.awaitingModifiedPreviewResponse = false;
        } else {
            pending.awaitingUnauthResponse = false;
        }
    }

    private enum ResponseVariant {
        MODIFIED,
        UNAUTH
    }

    private HttpRequest stripCookies(HttpRequest request) {
        HttpRequest current = request;
        boolean changed = false;

        for (ParsedHttpParameter param : request.parameters()) {
            if (param.type() == HttpParameterType.COOKIE) {
                HttpParameter cookieParam = HttpParameter.cookieParameter(param.name(), param.value());
                current = current.withRemovedParameters(cookieParam);
                changed = true;
            }
        }

        for (var header : request.headers()) {
            if ("cookie".equalsIgnoreCase(header.name())) {
                current = current.withRemovedHeader(header);
                changed = true;
            }
        }

        return changed ? current : request;
    }

    private HttpRequest applyReplaceRules(HttpRequest request) {
        HttpRequest modifiedRequest = request;
        boolean wasModified = false;

        api.logging().logToOutput("Processing request to: " + request.url() + " | Rules count: " + replaceRules.size());

        for (ReplaceRule rule : replaceRules) {
            if (!rule.isEnabled()) {
                api.logging().logToOutput("  Skipping disabled rule: " + rule.getName());
                continue;
            }
            if (!rule.hasOperations()) {
                api.logging().logToOutput("  Rule has no operations: " + rule.getName());
                continue;
            }

            try {
                api.logging().logToOutput("  Applying rule: " + rule.getName() + " (" + rule.getOperations().size() + " operations)");
                HttpRequest newRequest = applyRule(modifiedRequest, rule);
                if (newRequest != null && !newRequest.toString().equals(modifiedRequest.toString())) {
                    modifiedRequest = newRequest;
                    wasModified = true;
                    api.logging().logToOutput("  ✓ Rule applied successfully: " + rule.getName());
                } else {
                    api.logging().logToOutput("  ✗ Rule did not modify request: " + rule.getName());
                }
            } catch (Exception e) {
                api.logging().logToError("Error applying rule '" + rule.getName() + "': " + e.getMessage());
            }
        }

        api.logging().logToOutput("Request processing complete. Was modified: " + wasModified);
        return wasModified ? modifiedRequest : null;
    }

    private HttpRequest applyRule(HttpRequest request, ReplaceRule rule) {
        HttpRequest current = request;
        boolean modified = false;

        for (ReplaceRule.ReplaceOperation operation : rule.getOperations()) {
            HttpRequest updated = applyOperation(current, operation);
            if (updated != null && !updated.toString().equals(current.toString())) {
                current = updated;
                modified = true;
                api.logging().logToOutput("    ✓ Operation applied: " + operation.describe());
            } else {
                api.logging().logToOutput("    ✗ Operation had no effect: " + operation.describe());
            }
        }

        return modified ? current : null;
    }

    private HttpRequest applyOperation(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        return switch (operation.getType()) {
            case REQUEST_STRING -> replaceInRequestString(request, operation);
            case REQUEST_HEADER -> modifyRequestHeader(request, operation);
            case REQUEST_BODY -> replaceRequestBody(request, operation);
            case REQUEST_PARAM_NAME -> renameParameter(request, operation);
            case REQUEST_PARAM_VALUE -> replaceParameterValue(request, operation);
            case REQUEST_COOKIE_NAME -> renameCookie(request, operation);
            case REQUEST_COOKIE_VALUE -> replaceCookieValue(request, operation);
            case REMOVE_PARAMETER_BY_NAME -> removeParameterByName(request, operation);
            case REMOVE_PARAMETER_BY_VALUE -> removeParameterByValue(request, operation);
            case REMOVE_COOKIE_BY_NAME -> removeCookieByName(request, operation);
            case REMOVE_COOKIE_BY_VALUE -> removeCookieByValue(request, operation);
            case REMOVE_HEADER_BY_NAME -> removeHeaderByName(request, operation);
            case REMOVE_HEADER_BY_VALUE -> removeHeaderByValue(request, operation);
            case MATCH_PARAM_NAME_REPLACE_VALUE -> setParameterValueByName(request, operation);
            case MATCH_COOKIE_NAME_REPLACE_VALUE -> setCookieValueByName(request, operation);
            case MATCH_HEADER_NAME_REPLACE_VALUE -> setHeaderValueByName(request, operation);
        };
    }

    private HttpRequest replaceInRequestString(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String pattern = operation.getMatchPattern();
        String replace = operation.getReplaceValue();
        if (pattern == null || pattern.isEmpty()) {
            return request;
        }

        String requestString = request.toString();
        String updated;
        if (operation.isUseRegex()) {
            try {
                updated = requestString.replaceAll(pattern, replace);
            } catch (PatternSyntaxException ex) {
                api.logging().logToError("Invalid regex in operation " + operation.describe() + ": " + ex.getMessage());
                return request;
            }
        } else {
            updated = requestString.replace(pattern, replace);
        }

        if (!updated.equals(requestString)) {
            return HttpRequest.httpRequest(request.httpService(), updated);
        }
        return request;
    }

    private HttpRequest replaceRequestBody(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String pattern = operation.getMatchPattern();
        String replace = operation.getReplaceValue();
        if (pattern == null || pattern.isEmpty()) {
            return request;
        }

        String body = request.bodyToString();
        String updated;
        if (operation.isUseRegex()) {
            try {
                updated = body.replaceAll(pattern, replace);
            } catch (PatternSyntaxException ex) {
                api.logging().logToError("Invalid regex in operation " + operation.describe() + ": " + ex.getMessage());
                return request;
            }
        } else {
            updated = body.replace(pattern, replace);
        }

        if (!updated.equals(body)) {
            return request.withBody(updated);
        }
        return request;
    }

    private HttpRequest modifyRequestHeader(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern() == null ? "" : operation.getMatchPattern().trim();
        String replace = operation.getReplaceValue() == null ? "" : operation.getReplaceValue();

        if (match.isEmpty()) {
            return addHeaderFromReplace(request, replace);
        }

        Pattern pattern = compilePattern(operation, true);
        HttpRequest current = request;
        boolean changed = false;
        for (var header : request.headers()) {
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(header.name()).find()
                    : header.name().equalsIgnoreCase(match);
            if (matches) {
                String sanitizedValue = sanitizeHeaderValue(header.name(), replace);
                current = current.withUpdatedHeader(header.name(), sanitizedValue);
                changed = true;
            }
        }

        if (!changed && !operation.isUseRegex()) {
            String sanitizedValue = sanitizeHeaderValue(match, replace);
            current = current.withAddedHeader(match, sanitizedValue);
            changed = true;
        }
        return changed ? current : request;
    }

    private HttpRequest renameParameter(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        String newName = sanitizeTokenName(operation.getReplaceValue());
        if (match == null || match.isEmpty() || newName.isEmpty()) {
            return request;
        }

        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (ParsedHttpParameter param : request.parameters()) {
            if (!isEditableParameter(param)) {
                continue;
            }
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(param.name()).find()
                    : param.name().equals(match);
            if (matches) {
                HttpParameter oldParam = toHttpParameter(param);
                HttpParameter newParam = createParameter(param.type(), newName, param.value());
                current = current.withRemovedParameters(oldParam).withAddedParameters(newParam);
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest replaceParameterValue(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        String newValueRaw = operation.getReplaceValue();
        if (match == null || match.isEmpty()) {
            return request;
        }

        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (ParsedHttpParameter param : request.parameters()) {
            if (!isEditableParameter(param)) {
                continue;
            }
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(param.value()).find()
                    : param.value().equals(match);
            if (matches) {
                String sanitizedValue = sanitizeParameterValue(param.name(), newValueRaw);
                HttpParameter updated = createParameter(param.type(), param.name(), sanitizedValue);
                current = current.withUpdatedParameters(updated);
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest renameCookie(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        String newName = sanitizeTokenName(operation.getReplaceValue());
        if (match == null || match.isEmpty() || newName.isEmpty()) {
            return request;
        }

        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (ParsedHttpParameter param : request.parameters()) {
            if (param.type() != HttpParameterType.COOKIE) {
                continue;
            }
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(param.name()).find()
                    : param.name().equals(match);
            if (matches) {
                HttpParameter oldParam = HttpParameter.cookieParameter(param.name(), param.value());
                HttpParameter newParam = HttpParameter.cookieParameter(newName, param.value());
                current = current.withRemovedParameters(oldParam).withAddedParameters(newParam);
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest replaceCookieValue(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        String newValueRaw = operation.getReplaceValue();
        if (match == null || match.isEmpty()) {
            return request;
        }

        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (ParsedHttpParameter param : request.parameters()) {
            if (param.type() != HttpParameterType.COOKIE) {
                continue;
            }
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(param.value()).find()
                    : param.value().equals(match);
            if (matches) {
                String sanitizedValue = sanitizeCookieValue(param.name(), newValueRaw);
                HttpParameter updated = HttpParameter.cookieParameter(param.name(), sanitizedValue);
                current = current.withUpdatedParameters(updated);
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest removeParameterByName(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        if (match == null || match.isEmpty()) {
            return request;
        }
        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (ParsedHttpParameter param : request.parameters()) {
            if (!isEditableParameter(param)) {
                continue;
            }
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(param.name()).find()
                    : param.name().equals(match);
            if (matches) {
                current = current.withRemovedParameters(toHttpParameter(param));
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest removeParameterByValue(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        if (match == null || match.isEmpty()) {
            return request;
        }
        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (ParsedHttpParameter param : request.parameters()) {
            if (!isEditableParameter(param)) {
                continue;
            }
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(param.value()).find()
                    : param.value().equals(match);
            if (matches) {
                current = current.withRemovedParameters(toHttpParameter(param));
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest removeCookieByName(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        if (match == null || match.isEmpty()) {
            return request;
        }
        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (ParsedHttpParameter param : request.parameters()) {
            if (param.type() != HttpParameterType.COOKIE) {
                continue;
            }
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(param.name()).find()
                    : param.name().equals(match);
            if (matches) {
                current = current.withRemovedParameters(HttpParameter.cookieParameter(param.name(), param.value()));
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest removeCookieByValue(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        if (match == null || match.isEmpty()) {
            return request;
        }
        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (ParsedHttpParameter param : request.parameters()) {
            if (param.type() != HttpParameterType.COOKIE) {
                continue;
            }
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(param.value()).find()
                    : param.value().equals(match);
            if (matches) {
                current = current.withRemovedParameters(HttpParameter.cookieParameter(param.name(), param.value()));
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest removeHeaderByName(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        if (match == null || match.isEmpty()) {
            return request;
        }
        Pattern pattern = compilePattern(operation, true);
        HttpRequest current = request;
        boolean changed = false;
        for (var header : request.headers()) {
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(header.name()).find()
                    : header.name().equalsIgnoreCase(match);
            if (matches) {
                current = current.withRemovedHeader(header);
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest removeHeaderByValue(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        if (match == null || match.isEmpty()) {
            return request;
        }
        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (var header : request.headers()) {
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(header.value()).find()
                    : header.value().equals(match);
            if (matches) {
                current = current.withRemovedHeader(header);
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest setParameterValueByName(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        String newValueRaw = operation.getReplaceValue();
        if (match == null || match.isEmpty()) {
            return request;
        }
        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (ParsedHttpParameter param : request.parameters()) {
            if (!isEditableParameter(param)) {
                continue;
            }
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(param.name()).find()
                    : param.name().equals(match);
            if (matches) {
                String sanitizedValue = sanitizeParameterValue(param.name(), newValueRaw);
                HttpParameter updated = createParameter(param.type(), param.name(), sanitizedValue);
                current = current.withUpdatedParameters(updated);
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest setCookieValueByName(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        String newValueRaw = operation.getReplaceValue();
        if (match == null || match.isEmpty()) {
            return request;
        }
        Pattern pattern = compilePattern(operation, false);
        HttpRequest current = request;
        boolean changed = false;
        for (ParsedHttpParameter param : request.parameters()) {
            if (param.type() != HttpParameterType.COOKIE) {
                continue;
            }
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(param.name()).find()
                    : param.name().equals(match);
            if (matches) {
                String sanitizedValue = sanitizeCookieValue(param.name(), newValueRaw);
                HttpParameter updated = HttpParameter.cookieParameter(param.name(), sanitizedValue);
                current = current.withUpdatedParameters(updated);
                changed = true;
            }
        }
        return changed ? current : request;
    }

    private HttpRequest setHeaderValueByName(HttpRequest request, ReplaceRule.ReplaceOperation operation) {
        String match = operation.getMatchPattern();
        String newValue = operation.getReplaceValue();
        if (match == null || match.isEmpty()) {
            return request;
        }
        Pattern pattern = compilePattern(operation, true);
        HttpRequest current = request;
        boolean changed = false;
        for (var header : request.headers()) {
            boolean matches = operation.isUseRegex()
                    ? pattern != null && pattern.matcher(header.name()).find()
                    : header.name().equalsIgnoreCase(match);
            if (matches) {
                String sanitizedValue = sanitizeHeaderValue(header.name(), newValue);
                current = current.withUpdatedHeader(header.name(), sanitizedValue);
                changed = true;
            }
        }
        if (!changed && !operation.isUseRegex()) {
            String sanitizedValue = sanitizeHeaderValue(match, newValue);
            current = current.withAddedHeader(match, sanitizedValue);
            changed = true;
        }
        return changed ? current : request;
    }

    private HttpRequest addHeaderFromReplace(HttpRequest request, String replaceValue) {
        if (replaceValue == null || replaceValue.isBlank()) {
            api.logging().logToError("Cannot add header: replacement value is empty");
            return request;
        }
        int colon = replaceValue.indexOf(':');
        String name;
        String value;
        if (colon < 0) {
            name = replaceValue.trim();
            value = "";
        } else {
            name = replaceValue.substring(0, colon).trim();
            value = replaceValue.substring(colon + 1).trim();
        }
        if (name.isEmpty()) {
            api.logging().logToError("Cannot add header: unable to parse name from '" + replaceValue + "'");
            return request;
        }
        return request.withAddedHeader(name, value);
    }

    private String sanitizeHeaderValue(String headerName, String replacement) {
        if (replacement == null) {
            return "";
        }
        String trimmed = replacement.trim();
        int colon = trimmed.indexOf(':');
        if (colon >= 0) {
            String possibleName = trimmed.substring(0, colon).trim();
            String possibleValue = trimmed.substring(colon + 1).trim();
            if (!possibleValue.isEmpty() && possibleName.equalsIgnoreCase(headerName)) {
                return possibleValue;
            }
        }
        return trimmed;
    }

    private String sanitizeTokenName(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        int colon = trimmed.indexOf(':');
        if (colon >= 0) {
            trimmed = trimmed.substring(0, colon).trim();
        }
        int equals = trimmed.indexOf('=');
        if (equals >= 0) {
            trimmed = trimmed.substring(0, equals).trim();
        }
        return trimmed;
    }

    private String sanitizeParameterValue(String parameterName, String replacement) {
        if (replacement == null) {
            return "";
        }
        String trimmed = replacement.trim();
        if (parameterName != null && !parameterName.isEmpty()) {
            String equalsPrefix = parameterName + "=";
            if (trimmed.startsWith(equalsPrefix)) {
                return trimmed.substring(equalsPrefix.length()).trim();
            }
            String spacedPrefix = parameterName + " =";
            if (trimmed.startsWith(spacedPrefix)) {
                return trimmed.substring(spacedPrefix.length()).trim();
            }
        }
        return trimmed;
    }

    private String sanitizeCookieValue(String cookieName, String replacement) {
        if (replacement == null) {
            return "";
        }
        String trimmed = replacement.trim();
        if (trimmed.regionMatches(true, 0, "cookie:", 0, 7)) {
            trimmed = trimmed.substring(7).trim();
        }
        if (cookieName != null && !cookieName.isEmpty()) {
            String equalsPrefix = cookieName + "=";
            if (trimmed.startsWith(equalsPrefix)) {
                return trimmed.substring(equalsPrefix.length()).trim();
            }
            String spacedPrefix = cookieName + " =";
            if (trimmed.startsWith(spacedPrefix)) {
                return trimmed.substring(spacedPrefix.length()).trim();
            }
        }
        return trimmed;
    }

    private boolean isEditableParameter(ParsedHttpParameter param) {
        return param.type() == HttpParameterType.URL || param.type() == HttpParameterType.BODY;
    }

    private HttpParameter toHttpParameter(ParsedHttpParameter param) {
        return createParameter(param.type(), param.name(), param.value());
    }

    private HttpParameter createParameter(HttpParameterType type, String name, String value) {
        return switch (type) {
            case URL -> HttpParameter.urlParameter(name, value);
            case BODY -> HttpParameter.bodyParameter(name, value);
            case COOKIE -> HttpParameter.cookieParameter(name, value);
            default -> HttpParameter.parameter(name, value, type);
        };
    }

    private Pattern compilePattern(ReplaceRule.ReplaceOperation operation, boolean ignoreCase) {
        if (!operation.isUseRegex()) {
            return null;
        }
        String pattern = operation.getMatchPattern();
        if (pattern == null || pattern.isEmpty()) {
            return null;
        }
        int flags = Pattern.DOTALL;
        if (ignoreCase) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        try {
            return Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException ex) {
            api.logging().logToError("Invalid regex in operation " + operation.describe() + ": " + ex.getMessage());
            return null;
        }
    }
}
