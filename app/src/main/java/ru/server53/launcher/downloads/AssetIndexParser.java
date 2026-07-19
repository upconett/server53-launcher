package ru.server53.launcher.downloads;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;


public class AssetIndexParser {

    public static Map<String, AssetInfo> parse(
        Path assetIndexJsonPath
    ) throws IOException {
        Map<String, AssetInfo> assetsInfo = new HashMap<>();

        JSONObject rawAssetIndex = new JSONObject(Files.readString(assetIndexJsonPath));
        JSONObject rawObjects = rawAssetIndex.getJSONObject("objects");
        for (String key : rawObjects.keySet()) {
            JSONObject rawAsset = rawObjects.getJSONObject(key);
            AssetInfo newAsset = parseAsset(key, rawAsset);
            assetsInfo.put(key, newAsset);
        }

        return assetsInfo;
    }

    private static AssetInfo parseAsset(String name, JSONObject raw) {
        String hash = raw.getString("hash");
        Long size = raw.getLong("size");
        return new AssetInfo(name, hash, size);
    }
}
