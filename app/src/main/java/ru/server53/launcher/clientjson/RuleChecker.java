package ru.server53.launcher.clientjson;

public class RuleChecker {
    private final AllowanceRule[] rules;

    public RuleChecker(AllowanceRule[] rules) {
        this.rules = rules;
    }

    public boolean checkAllowance(LaunchContext context) {
        for (AllowanceRule rule : rules) {
            if (!rule.allows(context)) {
                return false;
            }
        }
        return true;
    }
}
