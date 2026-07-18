// package ru.server53.launcher;

// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// import org.apache.commons.text.StringSubstitutor;
// import org.json.JSONArray;
// import org.json.JSONObject;


// public class VersionInfoParser {
//     private final String MINECRAFT_VERSION;
//     private final Path MINECRAFT_DIRECTORY;
//     private final Path ALL_VERSIONS_PATH;
//     private final Path LIBRARIES_PATH;
//     private final Path VERSION_PATH;
//     private final Path NATIVES_PATH;
//     private final Path VERSION_JAR_PATH;
//     private final Path ASSETS_PATH;
//     private final Path VERSION_JSON_PATH;
//     private final String CLASSPATH_SEPARATOR;
//     private final String USERNAME;
//     private final String USER_UUID;
//     private final String USER_ACCESS_TOKEN;

//     private final JSONObject versionJSON;


//     public VersionInfoParser(
//         String minecraftDirectory,
//         String minecraftVersion,
//         String username,
//         String uuid,
//         String accessToken
//     ) throws IOException 
//     {
//         MINECRAFT_VERSION = minecraftVersion;
//         USERNAME = username;
//         MINECRAFT_DIRECTORY = Paths.get(minecraftDirectory)
//             .toAbsolutePath()
//             .normalize()
//             .toRealPath();
//         LIBRARIES_PATH = MINECRAFT_DIRECTORY.resolve("libraries");
//         ALL_VERSIONS_PATH = MINECRAFT_DIRECTORY.resolve("versions");
//         VERSION_PATH = ALL_VERSIONS_PATH.resolve(MINECRAFT_VERSION);
//         NATIVES_PATH = VERSION_PATH.resolve("natives");
//         ASSETS_PATH = MINECRAFT_DIRECTORY.resolve("assets");
//         VERSION_JAR_PATH = VERSION_PATH.resolve(minecraftVersion+".jar");
//         VERSION_JSON_PATH = VERSION_PATH.resolve(minecraftVersion+".json");
//         CLASSPATH_SEPARATOR = OSUtils.isWindows() ? ";" : ":";
//         versionJSON = new JSONObject(Files.readString(VERSION_JSON_PATH));
//         USER_UUID = uuid;
//         USER_ACCESS_TOKEN = accessToken;
//     }


//     public String[] parse() throws IOException {
//         List<String> output = new ArrayList<>();

//         LibraryInfo[] libs = parseLibraries();
//         String classPath = buildClassPath(libs);
//         String assetIndex = versionJSON.getString("assets");
//         String[] jvmArgs = parseJVMArgs();
//         String[] gameArgs = parseGameArgs();
//         populateArgs(jvmArgs, classPath, assetIndex);
//         populateArgs(gameArgs, classPath, assetIndex);

//         for (String arg : jvmArgs) { output.add(arg); }
//         parseFunnyStuff(output);
//         for (String arg : gameArgs) { output.add(arg); }

//         String[] command = output.toArray(new String[0]);
//         return withJava(command);
//     }


//     private String[] withJava(String[] command) {
//         String[] newCommand = new String[command.length+1];
//         newCommand[0] = "java";
//         for (int i = 0; i < command.length; i++)
//             newCommand[i+1] = command[i];
//         return newCommand;
//     }


//     private void parseFunnyStuff(List<String> output) {
//         output.add(versionJSON.getString("mainClass"));
//     }

//     private LibraryInfo[] parseLibraries() {
//         List<LibraryInfo> output = new ArrayList<>(); 
//         JSONArray libsJSON = versionJSON.getJSONArray("libraries");

//         for (int i = 0; i < libsJSON.length(); i++) {
//             JSONObject libJSON = libsJSON.getJSONObject(i);
//             if (!allowBySystem(libJSON)) {
//                 continue;
//             }
//             if (libJSON.has("downloadOnly")) {
//                 continue;
//             }
//             String name = libJSON.getString("name");
//             LibraryInfo lib = LibraryInfo.fromLibsPath(LIBRARIES_PATH, name);
//             output.add(lib);
//         }

//         return output.toArray(new LibraryInfo[0]);
//     }


//     private String buildClassPath(LibraryInfo[] libs) {
//         StringBuilder output = new StringBuilder();
//         for (var lib : libs) {
//             output.append(lib.path().toString());
//             output.append(CLASSPATH_SEPARATOR);
//         }
//         output.append(VERSION_JAR_PATH.toString());
//         return output.toString();
//     }

//     private void populateArgs(String[] args, String classPath, String assetIndex) {
//         Map<String, String> map = new HashMap<>();
//         map.put("natives_directory", NATIVES_PATH.toString());
//         map.put("launcher_name", "Server53Launcher");
//         map.put("launcher_version", "0");
//         map.put("classpath", classPath);
//         map.put("version_name", MINECRAFT_VERSION);
//         map.put("library_directory", LIBRARIES_PATH.toString());
//         map.put("classpath_separator", CLASSPATH_SEPARATOR);
//         map.put("auth_player_name", USERNAME);
//         map.put("game_directory", MINECRAFT_DIRECTORY.toString());
//         map.put("assets_root", ASSETS_PATH.toString());
//         map.put("assets_index_name", assetIndex);
//         map.put("auth_uuid", USER_UUID);
//         map.put("auth_access_token", USER_ACCESS_TOKEN);
//         map.put("clientid", "");
//         map.put("auth_xuid", "");
//         map.put("user_type", "msa");
//         map.put("version_type", "modded");
//         map.put("quickPlayPath", "");
//         map.put("quickPlaySingleplayer", "");
//         map.put("quickPlayMultiplayer", "");
//         map.put("quickPlayRealms", "");

//         for (int i = 0; i < args.length; i++) {
//             StringSubstitutor substitutor = new StringSubstitutor(map);
//             args[i] = substitutor.replace(args[i]);
//         }
//     }

//     private String[] parseJVMArgs() {
//         List<String> output = new ArrayList<>();
//         JSONObject argsJSON = versionJSON.getJSONObject("arguments");
//         JSONArray jvmArgsJSON = argsJSON.getJSONArray("jvm");

//         for (int i = 0; i < jvmArgsJSON.length(); i++) {
//             JSONObject argJSON = jvmArgsJSON.getJSONObject(i);
//             if (!allowBySystem(argJSON)) {
//                 continue;
//             }
//             for (String value : argValuesToArray(argJSON)) {
//                 output.add(value);
//             }
//         }

//         return output.toArray(new String[0]);
//     }

//     private String[] parseGameArgs() {
//         List<String> output = new ArrayList<>();
//         JSONObject argsJSON = versionJSON.getJSONObject("arguments");
//         JSONArray gameArgsJSON = argsJSON.getJSONArray("game");

//         for (int i = 0; i < gameArgsJSON.length(); i++) {
//             JSONObject argJSON = gameArgsJSON.getJSONObject(i);
//             if (
//                 !allowBySystem(argJSON)
//                 || !allowByFeatures(argJSON)
//             ) {
//                 continue;
//             }
//             for (String value : argValuesToArray(argJSON)) {
//                 output.add(value);
//             }
//         }

//         return output.toArray(new String[0]);
//     }

//     private String[] argValuesToArray(JSONObject argJSON) {
//         List<String> output = new ArrayList<>();
//         Object value = argJSON.get("value");

//         if (value instanceof String) {
//             output.add((String) value);
//         } else if (value instanceof JSONArray) {
//             JSONArray arr = (JSONArray) value;
//             for (int j = 0; j < arr.length(); j++) {
//                 output.add(arr.getString(j));
//             }
//         } else {
//             throw new IllegalArgumentException(
//                 "Unexpected value type: " + value.getClass()
//             );
//         }

//         return output.toArray(new String[0]);
//     }

//     private boolean allowByFeatures(JSONObject objJSON) {
//         Map<String, Boolean> possibleFeatures = new HashMap<>();
//         possibleFeatures.put("is_demo_user", false);
//         possibleFeatures.put("has_custom_resolution", false);
//         possibleFeatures.put("has_quick_plays_support", false);
//         possibleFeatures.put("has_quick_play_singleplayer", false);
//         possibleFeatures.put("has_quick_play_multiplayer", false);
//         possibleFeatures.put("has_quick_play_realms", false);

//         if (!objJSON.has("rules")) {
//             return true;
//         }
//         JSONArray rulesJSON = objJSON.getJSONArray("rules");
//         for (int i = 0; i < rulesJSON.length(); i++) {
//             JSONObject ruleJSON = rulesJSON.getJSONObject(i);
//             if (!ruleJSON.has("features")) {
//                 continue;
//             }
//             JSONObject featuresJSON = ruleJSON.getJSONObject("features");
//             for (var entry : possibleFeatures.entrySet()) {
//                 if (featuresJSON.has(entry.getKey())) {
//                     return entry.getValue();
//                 }
//             }
//         }
//         return false;
//     }
    
//     private boolean allowBySystem(JSONObject objJSON) {
//         if (!objJSON.has("rules")) {
//             return true;
//         }
//         JSONArray rulesJSON = objJSON.getJSONArray("rules");
//         for (int i = 0; i < rulesJSON.length(); i++) {
//             JSONObject ruleJSON = rulesJSON.getJSONObject(i);
//             if (!ruleJSON.has("os")) {
//                 continue;
//             }
//             JSONObject osJSON = ruleJSON.getJSONObject("os");
//             if (!osJSON.has("name")) {
//                 continue;
//             }
//             String osName = osJSON.getString("name");
//             if (osName.equalsIgnoreCase(OSUtils.getName())) {
//                 return true;
//             }
//         }
//         return false;
//     } 
// }
