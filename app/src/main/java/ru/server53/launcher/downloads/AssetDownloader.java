package ru.server53.launcher.downloads;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ru.server53.launcher.clientjson.AssetIndexInfo;
import ru.server53.launcher.clientjson.ClientJson;


public class AssetDownloader {
    private final ClientJson clientJson;
    private final String ASSETS_DOWNLOAD_URL = "https://resources.download.minecraft.net";
    private final DownloadManager downloadManager;

    private final int MAX_RETRIES = 5;

    public AssetDownloader(
        ClientJson clientJson,
        DownloadManager downloadManager
    ) {
        this.clientJson = clientJson;
        this.downloadManager = downloadManager;
    }

    public Path downloadGameTo(Path downloadDirectory) throws IOException, InterruptedException, URISyntaxException {
        Path assetsDirectoryPath = downloadDirectory.resolve("assets");
        Path assetIndexJsonPath = downloadAssetIndex(assetsDirectoryPath);
        Map<String, AssetInfo> assetsInfo = AssetIndexParser.parse(assetIndexJsonPath);
        var yetToDownload = checkAssets(assetsDirectoryPath, assetsInfo);
        downloadAssets(assetsDirectoryPath, yetToDownload);
        return downloadDirectory;
    }

    private Path downloadAssetIndex(Path assetsDirectoryPath) throws IOException, InterruptedException {
        AssetIndexInfo assetIndex = clientJson.assetIndex();

        Path assetsIndexesPath = assetsDirectoryPath.resolve("indexes");
        Path assetIndexJsonPath = assetsIndexesPath.resolve("%s.json".formatted(assetIndex.id));

        Files.createDirectories(assetsIndexesPath);

        URI downloadURL = assetIndex.download.url;

        Files.deleteIfExists(assetIndexJsonPath);

        System.out.println("Downloading '%s'...    ".formatted(assetIndexJsonPath.toString()));
        downloadManager.downloadFile(downloadURL, assetIndexJsonPath).join();
        System.out.println("✅");

        return assetIndexJsonPath;
    }

    private Map<String, AssetInfo> checkAssets(Path assetsDirectoryPath, Map<String, AssetInfo> assetsInfo) throws IOException {
        Map<String, AssetInfo> notPresentedMap = new HashMap<>();
        notPresentedMap.putAll(assetsInfo);

        Path assetsObjectsDirectoryPath = assetsDirectoryPath.resolve("objects");
        Files.createDirectories(assetsObjectsDirectoryPath);
        int total = assetsInfo.size();
        int notPresented = 0;

        for (var entry : assetsInfo.entrySet()) {
            AssetInfo assetInfo = entry.getValue();
            String assetSubdirectory = assetInfo.hash().substring(0, 2);
            Path assetSubdirectoryPath = assetsObjectsDirectoryPath.resolve(assetSubdirectory);
            Path assetPath = assetSubdirectoryPath.resolve(assetInfo.hash());
            if (!Files.exists(assetPath)) {
                notPresented++;
                System.out.println( "Not presented '%s'".formatted(assetInfo.hash()));
            } else {
                notPresentedMap.remove(entry.getKey());
            }
        }
        System.out.println("Files presented %d/%d".formatted(total-notPresented, total));
        return notPresentedMap;
    }
    
    private void downloadAssets(Path assetsDirectoryPath, Map<String, AssetInfo> assetsInfo) throws IOException, URISyntaxException, InterruptedException {
        Path assetsObjectsDirectoryPath = assetsDirectoryPath.resolve("objects");
        Files.createDirectories(assetsObjectsDirectoryPath);

        var downloadedCounter = new DownloadCounter(assetsInfo.size());
        var futures = new ArrayList<CompletableFuture<HttpResponse<Path>>>();
        for (AssetInfo assetInfo : assetsInfo.values()) {
            futures.add(downloadAssetWithRetry(assetInfo, assetsObjectsDirectoryPath, 0, downloadedCounter));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }


    private CompletableFuture<HttpResponse<Path>> downloadAssetWithRetry(
        AssetInfo assetInfo,
        Path assetsObjectsDirectoryPath,
        int attempt,
        DownloadCounter downloadedCounter
    ) throws IOException, URISyntaxException, InterruptedException {

        if (attempt >= MAX_RETRIES) {
            System.out.println("Reached MAX_RETRIES");
            return CompletableFuture.failedFuture(new RuntimeException("Hit MAX_RETRIES"));
        }

        String assetSubdirectory = assetInfo.hash().substring(0, 2);
        Path assetSubdirectoryPath = assetsObjectsDirectoryPath.resolve(assetSubdirectory);
        Path assetPath = assetSubdirectoryPath.resolve(assetInfo.hash());

        Files.createDirectories(assetSubdirectoryPath);
        Files.deleteIfExists(assetPath);

        String assetURLPath = "%s/%s".formatted(assetSubdirectory, assetInfo.hash());
        URI downloadURI = new URI(ASSETS_DOWNLOAD_URL).resolve(assetURLPath);

        return downloadManager.downloadFile(downloadURI, assetPath)
            .thenCompose((response) -> {
                try {
                    if (response.statusCode() == 200
                        && Files.size(assetPath) == assetInfo.size()
                    ) {
                        downloadedCounter.increment();
                        System.out.println("%d/%d, %s".formatted(downloadedCounter.getCurrent(), downloadedCounter.getTotal(), response.toString()));
                        return CompletableFuture.completedFuture(response);
                    } else {
                        System.out.println("Invalid file size");
                        return CompletableFuture.failedFuture(new Exception("Invalid File Size"));
                    }
                } catch (Exception e) {
                    System.out.println("Exception %s".formatted(e.toString()));
                    return CompletableFuture.failedFuture(e);
                }
            })
            .exceptionallyCompose((exception) -> {
                try {
                    System.out.println("Exception %s".formatted(exception.toString()));
                    return downloadAssetWithRetry(
                        assetInfo,
                        assetsObjectsDirectoryPath,
                        attempt + 1,
                        downloadedCounter
                    );
                } catch (Exception e) {
                    System.out.println("Exception %s".formatted(e.toString()));
                    return CompletableFuture.failedFuture(e);
                }
            });
    }
}


class DownloadCounter {
    private final int total;
    private int count;

    DownloadCounter(int total) {
        this.total = total;
        this.count = 0;
    }

    public synchronized void increment() {
        if (!done()) {
            this.count++;
        }
    }

    public synchronized boolean done() {
        return count == total;
    }

    public synchronized int getCurrent() {
        return count;
    }

    public synchronized int getTotal() {
        return total;
    }
}
