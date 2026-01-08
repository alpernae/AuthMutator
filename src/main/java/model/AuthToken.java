package model;

public class AuthToken {
    public enum Type {
        COOKIE,
        HEADER
    }

    private Type type;
    private String name;
    private String value;

    public AuthToken(Type type, String name, String value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
