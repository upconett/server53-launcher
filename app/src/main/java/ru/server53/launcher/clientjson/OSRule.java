package ru.server53.launcher.clientjson;

public class OSRule implements AllowanceRule {
    private final String os;

    public OSRule(String os) {
        this.os = os;
    }

    @Override
    public boolean allows(LaunchContext context) {
        return this.os.equals(context.os());
    }
}
