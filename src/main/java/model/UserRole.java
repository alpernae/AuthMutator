package model;

import java.util.ArrayList;
import java.util.List;

public class UserRole {
    private String name;
    private boolean enabled;
    private final List<AuthToken> tokens;

    public UserRole(String name) {
        this.name = name;
        this.enabled = false;
        this.tokens = new ArrayList<>();
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

    public List<AuthToken> getTokens() {
        return new ArrayList<>(tokens);
    }

    public void setTokens(List<AuthToken> tokens) {
        this.tokens.clear();
        if (tokens != null) {
            this.tokens.addAll(tokens);
        }
    }

    public void addToken(AuthToken token) {
        this.tokens.add(token);
    }
}
