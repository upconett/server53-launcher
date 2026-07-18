package ru.server53.launcher.clientjson;

public interface RuleControlled {
    public boolean isAllowed(LaunchContext context);
}
