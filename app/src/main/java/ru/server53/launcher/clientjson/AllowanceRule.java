package ru.server53.launcher.clientjson;

public interface AllowanceRule {
    public abstract boolean allows(LaunchContext context);
}
