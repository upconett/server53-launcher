package ru.server53.launcher.clientjson;

import java.util.Map;

public class FeaturesRule implements AllowanceRule {
    private final Map<String, Boolean> features;

    public FeaturesRule(Map<String, Boolean> features) {
        this.features = features;
    }

    @Override
    public boolean allows(LaunchContext context) {
        for (Map.Entry<String, Boolean> entry : this.features.entrySet()) {
            String feature = entry.getKey();
            if (!context.features().getOrDefault(feature, false)) {
                return false;
            }
        }
        return true;
    }
}
