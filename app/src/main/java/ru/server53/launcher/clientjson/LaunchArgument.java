package ru.server53.launcher.clientjson;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;


public class LaunchArgument implements RuleControlled {
    private final String[] rawValues;
    private final RuleChecker ruleChecker;

    public LaunchArgument(
        String[] rawValues,
        AllowanceRule[] rules
    ) {
        this.rawValues = rawValues;
        this.ruleChecker = new RuleChecker(rules);
    }

    public boolean isAllowed(LaunchContext context) {
        return ruleChecker.checkAllowance(context);
    }

    public String[] getPopulatedValues(LaunchContext context) {
        Map<String, Object> allArgs = context.getArgumentsForSubstitution();
        StringSubstitutor substitutor = new StringSubstitutor(allArgs);

        String[] populatedValues = Arrays.copyOf(rawValues, rawValues.length);
        for (int i = 0; i < populatedValues.length; i++) {
            populatedValues[i] = substitutor.replace(populatedValues[i]);
        }
        return populatedValues;
    }
}
