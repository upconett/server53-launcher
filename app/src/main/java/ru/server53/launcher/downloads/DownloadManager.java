package ru.server53.launcher.downloads;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import ru.server53.launcher.clientjson.AssetIndexInfo;
import ru.server53.launcher.clientjson.ClientJson;


public class DownloadManager {
    private final ClientJson clientJson;
    private final String ASSETS_DOWNLOAD_URL = "https://resources.download.minecraft.net";

    public DownloadManager(
        ClientJson clientJson
    ) {
        this.clientJson = clientJson;
    }

    public Path downloadGameTo(Path downloadDirectory) throws IOException, InterruptedException, URISyntaxException {
        Path assetsDirectoryPath = downloadDirectory.resolve("assets");
        Path assetIndexJsonPath = downloadAssetIndex(assetsDirectoryPath);
        Map<String, AssetInfo> assetsInfo = AssetIndexParser.parse(assetIndexJsonPath);
        downloadAssets(assetsDirectoryPath, assetsInfo);
        return downloadDirectory;
    }

    private Path downloadAssetIndex(Path assetsDirectoryPath) throws IOException, InterruptedException {
        AssetIndexInfo assetIndex = clientJson.assetIndex();

        Path assetsIndexesPath = assetsDirectoryPath.resolve("indexes");
        Path assetIndexJsonPath = assetsIndexesPath.resolve("%s.json".formatted(assetIndex.id));

        Files.createDirectories(assetsIndexesPath);

        URI downloadURL = assetIndex.download.url;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(downloadURL)
            .GET()
            .build();
        HttpClient client = HttpClient.newHttpClient();

        System.out.println("Downloading '%s'...    ".formatted(assetIndexJsonPath.toString()));
        client.send(request, BodyHandlers.ofFile(assetIndexJsonPath));
        System.out.println("✅");

        return assetIndexJsonPath;
    }
    
    private void downloadAssets(Path assetsDirectoryPath, Map<String, AssetInfo> assetsInfo) throws IOException, URISyntaxException, InterruptedException {
        Path assetsObjectsDirectoryPath = assetsDirectoryPath.resolve("objects");
        Files.createDirectories(assetsObjectsDirectoryPath);

        int total = assetsInfo.size();
        int count = 1;

        for (AssetInfo assetInfo : assetsInfo.values()) {
            count++;
            System.out.print("[%d/%d] ".formatted(count, total));
            downloadAsset(assetInfo, assetsObjectsDirectoryPath);
        }
    }

    private Path downloadAsset(AssetInfo assetInfo, Path assetsObjectsDirectoryPath) throws IOException, URISyntaxException, InterruptedException {
        String assetSubdirectory = assetInfo.hash().substring(0, 2);
        Path assetSubdirectoryPath = assetsObjectsDirectoryPath.resolve(assetSubdirectory);
        Path assetPath = assetSubdirectoryPath.resolve(assetInfo.hash());

        Files.createDirectories(assetSubdirectoryPath);

        String assetURLPath = "%s/%s".formatted(assetSubdirectory, assetInfo.hash());
        URI downloadURL = new URI(ASSETS_DOWNLOAD_URL).resolve(assetURLPath);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(downloadURL)
            .GET()
            .build();
        HttpClient client = HttpClient.newHttpClient();

        System.out.print("Downloading '%s'...    ".formatted(assetPath.toString()));
        client.send(request, BodyHandlers.ofFile(assetPath));
        System.out.println("✅");

        return assetPath;
    }
}
