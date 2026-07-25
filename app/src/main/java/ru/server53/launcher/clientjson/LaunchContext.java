package ru.server53.launcher.clientjson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ru.server53.launcher.MinecraftPathsBuilder;
import ru.server53.launcher.OSUtils;


public record LaunchContext (
    String os,
    String osArch,
    Map<String, Boolean> features,
    Map<String, String> otherArgs
){
    public static LaunchContext ofDefaultValues(
        String username,
        String uuid, 
        String accessToken,
        String classPath,
        MinecraftPathsBuilder pathBuilder,
        ClientJson clientJson
    ) throws IOException {
        String os = OSUtils.getOS();
        String osArch = OSUtils.getOSArch();

        var features = new HashMap<String, Boolean>();
        features.put("is_demo_user", false);
        features.put("has_custom_resolution", false);
        features.put("has_quick_plays_support", false);
        features.put("has_quick_play_singleplayer", false);
        features.put("has_quick_play_multiplayer", false);
        features.put("has_quick_play_realms", false);


        var MINECRAFT_VERSION = clientJson.id();
        var MINECRAFT_DIRECTORY = pathBuilder.getRootDirectory();

        var NATIVES_PATH = pathBuilder.getNativesDirectory(MINECRAFT_VERSION);
        var ASSETS_PATH = pathBuilder.getAssetsRootDirectory();
        var LIBRARIES_PATH = pathBuilder.getLibrariesDirectory();

        var CLASSPATH_SEPARATOR = OSUtils.isWindows() ? ";" : ":";

        var otherArgs = new HashMap<String, String>();
        otherArgs.put("natives_directory", NATIVES_PATH.toString());
        otherArgs.put("launcher_name", "Server53Launcher");
        otherArgs.put("launcher_version", "0");
        otherArgs.put("classpath", classPath);
        otherArgs.put("version_name", MINECRAFT_VERSION);
        otherArgs.put("library_directory", LIBRARIES_PATH.toString());
        otherArgs.put("classpath_separator", CLASSPATH_SEPARATOR);
        otherArgs.put("auth_player_name", username);
        otherArgs.put("game_directory", MINECRAFT_DIRECTORY.toString());
        otherArgs.put("assets_root", ASSETS_PATH.toString());
        otherArgs.put("assets_index_name", clientJson.assets());
        otherArgs.put("auth_uuid", uuid);
        otherArgs.put("auth_access_token", accessToken);
        otherArgs.put("clientid", "");
        otherArgs.put("auth_xuid", "");
        otherArgs.put("user_type", "msa");
        otherArgs.put("version_type", "modded");
        otherArgs.put("quickPlayPath", "");
        otherArgs.put("quickPlaySingleplayer", "");
        otherArgs.put("quickPlayMultiplayer", "");
        otherArgs.put("quickPlayRealms", "");
        otherArgs.put("resolution_width", "");
        otherArgs.put("resolution_height", "");

        return new LaunchContext(os, osArch, features, otherArgs);
    }

    public Map<String, Object> getArgumentsForSubstitution() {
        Map<String, Object> allArgs = new HashMap<>();
        allArgs.putAll(features);
        allArgs.putAll(otherArgs);
        return allArgs;
    }

    public boolean isWindows() { return os.equals("windows"); }
    public boolean isMac() { return os.equals("osx"); }
    public boolean isLinux() { return os.equals("linux"); }
}
