package util;

import burp.api.montoya.MontoyaApi;
import model.ExtensionConfig;
import model.ExtensionState;
import model.HighlightRule;
import model.ReplaceRule;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Handles loading and saving the extension state (config + rules) to disk.
 */
public class PersistenceService {
    private static final String STATE_FILE_NAME = ".AuthMutator.json";

    private final MontoyaApi api;
    private final Path stateFile;

    public PersistenceService(MontoyaApi api) {
        this.api = api;
        Path home = Path.of(System.getProperty("user.home", "."));
        this.stateFile = home.resolve(STATE_FILE_NAME);
    }

    public ExtensionState loadState(ExtensionConfig config) {
        if (Files.exists(stateFile)) {
            try {
                String json = Files.readString(stateFile, StandardCharsets.UTF_8);
                return parseState(json, config);
            } catch (IOException e) {
                api.logging().logToError("Failed to read state file: " + e.getMessage());
            }
        }
        return new ExtensionState();
    }

    public void saveState(ExtensionConfig config, ExtensionState state) {
        try {
        JSONObject root = JsonUtil.stateToJson(config,
            state.getReplaceRules(),
            state.getHighlightRules());
            ensureParentDirectory();
            Files.writeString(stateFile, root.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            api.logging().logToError("Failed to persist state: " + e.getMessage());
        }
    }

    public ExtensionState importState(Path file, ExtensionConfig config) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        ExtensionState imported = parseState(json, config);
        // Persist imported state as the new default.
        saveState(config, imported);
        return imported;
    }

    public void exportState(Path file, ExtensionConfig config, ExtensionState state) throws IOException {
    JSONObject root = JsonUtil.stateToJson(config,
        state.getReplaceRules(),
        state.getHighlightRules());
        ensureTargetDirectory(file);
        Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
    }

    private ExtensionState parseState(String json, ExtensionConfig config) {
        try {
            JSONObject root = JSONObject.parseObject(json);
            JSONObject cfg = root.optJSONObject("config");
            if (cfg != null) {
                JsonUtil.configFromJson(cfg, config);
            }
            JSONArray replaceArr = root.optJSONArray("replaceRules");
            JSONArray highlightArr = root.optJSONArray("highlightRules");

            List<ReplaceRule> replaceRules = JsonUtil.rulesFromJson(replaceArr != null ? replaceArr : new JSONArray());
            List<HighlightRule> highlightRules = JsonUtil.highlightRulesFromJson(highlightArr != null ? highlightArr : new JSONArray());

            return new ExtensionState(replaceRules, highlightRules);
        } catch (Exception ex) {
            api.logging().logToError("Invalid state JSON: " + ex.getMessage());
            return new ExtensionState();
        }
    }

    private void ensureParentDirectory() throws IOException {
        Path parent = stateFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private void ensureTargetDirectory(Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}
