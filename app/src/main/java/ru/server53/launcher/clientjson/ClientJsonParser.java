package ru.server53.launcher.clientjson;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;


public class ClientJsonParser {
    private final Path clientJsonPath;

    private JSONObject rawClientJson;


    public ClientJsonParser(
        Path clientJsonPath
    ) throws IOException {
        this.clientJsonPath = clientJsonPath;
    }


    public ClientJson parse() throws IOException, URISyntaxException {
        this.rawClientJson = new JSONObject(Files.readString(clientJsonPath));

        String clientType = rawClientJson.getString("type");
        String mainClass = rawClientJson.getString("mainClass");
        String assets = rawClientJson.getString("assets");

        AssetIndexInfo assetIndex = parseAssetIndex();
        LibraryInfo[] libraries = parseLibraries();
        Map<String, DownloadInfo> downloads = parseDownloads();

        JSONObject rawArguments = rawClientJson.getJSONObject("arguments");
        JSONArray rawArgumentsJvm = rawArguments.getJSONArray("jvm");
        JSONArray rawArgumentsGame = rawArguments.getJSONArray("game");

        LaunchArgument[] argumentsJVM = parseArguments(rawArgumentsJvm);
        LaunchArgument[] argumentsGame = parseArguments(rawArgumentsGame);

        JSONObject javaVersionJSON = rawClientJson.getJSONObject("javaVersion");
        String javaVersionComponent = javaVersionJSON.getString("component");
        int javaMajorVersion = javaVersionJSON.getInt("majorVersion");

        return new ClientJson(
            clientType,
            mainClass,
            libraries,
            downloads,
            assets,
            assetIndex,
            argumentsJVM,
            argumentsGame,
            javaVersionComponent,
            javaMajorVersion
        );
    }

    private AssetIndexInfo parseAssetIndex() throws URISyntaxException {
        JSONObject rawAssetIndex = rawClientJson.getJSONObject("assetIndex");
        String id = rawAssetIndex.getString("id");
        Long totalSize = rawAssetIndex.getLong("totalSize");
        Boolean known = rawAssetIndex.getBoolean("known");
        String url = rawAssetIndex.getString("url");
        String sha1 = rawAssetIndex.getString("sha1");
        Long size = rawAssetIndex.getLong("size");
        return new AssetIndexInfo(id, totalSize, known, url, sha1, size);
    }


    private LibraryInfo[] parseLibraries() throws URISyntaxException {
        List<LibraryInfo> libraries = new ArrayList<>(); 
        JSONArray rawLibraries = rawClientJson.getJSONArray("libraries");

        for (int i = 0; i < rawLibraries.length(); i++) {
            JSONObject rawLibrary = rawLibraries.getJSONObject(i);

            String name = rawLibrary.getString("name");
            AllowanceRule[] rules = parseRulesIfAny(rawLibrary);
            JSONObject rawDownloads = rawLibrary.getJSONObject("downloads");
            JSONObject rawArtifact = rawDownloads.getJSONObject("artifact");
            DownloadInfo download = parseDownload(rawArtifact);
            Boolean downloadOnly = false;
            if (rawLibrary.has("downloadOnly")) {
                downloadOnly = rawLibrary.getBoolean("downloadOnly");
            }

            LibraryInfo newLibrary = new LibraryInfo(name, downloadOnly, download, rules);
            libraries.add(newLibrary);
        }

        return libraries.toArray(new LibraryInfo[0]);
    }


    private DownloadInfo parseDownload(JSONObject raw) throws URISyntaxException {
        String url = raw.getString("url");
        String sha1 = raw.getString("sha1");
        Long size = raw.getLong("size");
        return new DownloadInfo(url, sha1, size);
    }


    private AllowanceRule[] parseRulesIfAny(JSONObject raw) {
        List<AllowanceRule> rules = new ArrayList<>();
        if (!raw.has("rules")) {
            return new AllowanceRule[0];
        }
        JSONArray rawRules = raw.getJSONArray("rules");
        for (int i = 0; i < rawRules.length(); i++) {
            AllowanceRule newRule = parseRule(rawRules.getJSONObject(i));
            rules.add(newRule);
        }
        return rules.toArray(new AllowanceRule[0]);
    }


    private AllowanceRule parseRule(JSONObject raw) {
        if (raw.has("os")) {
            JSONObject rawOS = raw.getJSONObject("os");
            String osName;
            if (rawOS.has("name")) {
                osName = rawOS.getString("name");
            } else {
                osName = "none";
            }
            return new OSRule(osName);

        } else if (raw.has("features")) {
            JSONObject rawFeatures = raw.getJSONObject("features");
            Map<String, Boolean> features = new HashMap<>();
            for (String key : rawFeatures.keySet()) {
                features.put(key, rawFeatures.getBoolean(key));
            }
            return new FeaturesRule(features);
        } else {
            throw new IllegalArgumentException("Invalid rule config: 'os' or 'features' required");
        }
    }


    private Map<String, DownloadInfo> parseDownloads() throws URISyntaxException {
        JSONObject rawDownloads = rawClientJson.getJSONObject("downloads");

        Map<String, DownloadInfo> downloads = new HashMap<>();
        for (String key : rawDownloads.keySet()) {
            JSONObject rawDownload = rawDownloads.getJSONObject(key);
            DownloadInfo download = parseDownload(rawDownload);
            downloads.put(key, download);
        }
        return downloads;
    }


    private LaunchArgument[] parseArguments(JSONArray raw) {
        LaunchArgument[] arguments = new LaunchArgument[raw.length()];
        for (int i = 0; i < raw.length(); i++) {
            JSONObject rawArgument = raw.getJSONObject(i);
            LaunchArgument newArgument = parseArgument(rawArgument);
            arguments[i] = newArgument;
        }
        return arguments;
    }

    private LaunchArgument parseArgument(JSONObject raw) {
        List<String> valuesList = new ArrayList<>();

        Object rawValue = raw.get("value");
        if (rawValue instanceof JSONArray) {
            JSONArray rawValueArr = (JSONArray) rawValue;
            for (int i = 0; i < rawValueArr.length(); i++) {
                valuesList.add(rawValueArr.getString(i));
            }
        } else if (rawValue instanceof String) {
            String rawValueStr = (String) rawValue;
            valuesList.add(rawValueStr);
        } else {
            throw new IllegalArgumentException("Argument value should be either 'String' or 'String[]'");
        }

        String[] values = valuesList.toArray(new String[0]);
        AllowanceRule[] rules = parseRulesIfAny(raw);

        return new LaunchArgument(values, rules);
    }
}
