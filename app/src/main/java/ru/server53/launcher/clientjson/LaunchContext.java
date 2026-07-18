package ru.server53.launcher.clientjson;

import java.util.HashMap;
import java.util.Map;

//     IMPORTANT
//
//     Map<String, Boolean> possibleFeatures = new HashMap<>();
//     possibleFeatures.put("is_demo_user", false);
//     possibleFeatures.put("has_custom_resolution", false);
//     possibleFeatures.put("has_quick_plays_support", false);
//     possibleFeatures.put("has_quick_play_singleplayer", false);
//     possibleFeatures.put("has_quick_play_multiplayer", false);
//     possibleFeatures.put("has_quick_play_realms", false);

public record LaunchContext (
    String os,
    Map<String, Boolean> features
){
    public Map<String, Object> getArgumentsForSubstitution() {
        Map<String, Object> allArgs = new HashMap<>();
        allArgs.putAll(features);
        return allArgs;
    }
}
