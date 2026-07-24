package ru.server53.launcher.downloads;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import ru.server53.launcher.clientjson.AssetIndexInfo;
import ru.server53.launcher.clientjson.ClientJson;
import ru.server53.launcher.clientjson.DownloadInfo;
import ru.server53.launcher.clientjson.LaunchContext;
import ru.server53.launcher.clientjson.LibraryInfo;


public class ResourceDownloader {
    private static final String ASSETS_DOWNLOAD_URL = "https://resources.download.minecraft.net";

    private final DownloadManager downloadManager;
    private final ClientJson clientJson;
    private final LaunchContext context;

    private final int MAX_RETRIES = 5;

    public ResourceDownloader(
        DownloadManager downloadManager,
        ClientJson clientJson,
        LaunchContext context
    ) {
        this.downloadManager = downloadManager;
        this.clientJson = clientJson;
        this.context = context;
    }

    public Path downloadGame(Path downloadDirectory) throws IOException, InterruptedException, URISyntaxException {
        Path assetsDirectoryPath = downloadDirectory.resolve("assets");

        System.out.println("Downloading asset index:");
        Path assetIndexJsonPath = downloadAssetIndex(assetsDirectoryPath);

        Map<String, AssetInfo> assetsInfo = AssetIndexParser.parse(assetIndexJsonPath);

        System.out.println("Checking assets:");
        var yetToDownload = checkAssets(assetsDirectoryPath, assetsInfo);

        System.out.println("Downloading assets:");
        downloadAssets(assetsDirectoryPath, yetToDownload);

        System.out.println("Downloading libraries:");
        Path libsDirectoryPath = downloadDirectory.resolve("libraries");
        downloadLibraries(libsDirectoryPath);

        System.out.println("Downloading client:");
        Path versionsDirectoryPath = downloadDirectory.resolve("versions");
        downloadClient(versionsDirectoryPath);

        System.out.println("Unpacking natives:");
        unpackNatives(downloadDirectory);

        return downloadDirectory;
    }

    private void unpackNatives(Path downloadDirectory) throws IOException {
        Path libsDirectoryPath = downloadDirectory.resolve("libraries");
        Path versionsDirectoryPath = downloadDirectory.resolve("versions");
        Path clientDirectoryPath = versionsDirectoryPath.resolve(clientJson.id());
        Path nativesDirectoryPath = clientDirectoryPath.resolve("natives");
        
        for (LibraryInfo lib : clientJson.libraries()) {
            if (!lib.isAllowed(context)) { continue; }
            Path libFilePath = getLibPath(libsDirectoryPath, lib);
            if (Files.notExists(libFilePath)) {
                throw new IOException("Natives lib '%s' does not exist, but required".formatted(libFilePath));
            }
            if (libFilePath.getFileName().toString().contains("-natives-")) {
                if (libFilePath.getFileName().toString().contains("arm64")) {
                    System.out.println("Skip %s --> arm64".formatted(libFilePath));
                    continue;
                }
                System.out.println("Unpacking %s".formatted(libFilePath));
                unpackNative(libFilePath, nativesDirectoryPath);
            }
        }

    }

    private void unpackNative(Path libPath, Path nativesDirectoryPath) throws IOException {
        try (JarFile jar = new JarFile(libPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                // Skip directories and non-native files
                if (entry.isDirectory()) continue;
                if (!shouldExtract(entry.getName())) continue;
                
                // Extract the file
                Path entryPath = Paths.get(entry.getName());
                Path target = nativesDirectoryPath.resolve(entryPath.getFileName());
                Files.createDirectories(target.getParent());
                System.out.println("- %s".formatted(target));
                
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, target);
                }
                
                // Make executable on Linux/Mac
                if (!context.isWindows()) {
                    target.toFile().setExecutable(true);
                }
            }
        }
    }

    public boolean shouldExtract(String fileName) {
        String lower = fileName.toLowerCase();
        if (context.isWindows()) {
            return lower.endsWith(".dll");
        } else if (context.isMac()) {
            return lower.endsWith(".dylib") || lower.endsWith(".jnilib");
        } else if (context.isLinux()) {
            return lower.endsWith(".so");
        }
        return false;
    }

    private Path downloadClient(Path versionsDirectoryPath) throws IOException {
        Path clientDirectoryPath = versionsDirectoryPath.resolve(clientJson.id());
        Path clientPath = clientDirectoryPath.resolve(clientJson.id()+".jar");

        Files.createDirectories(clientDirectoryPath);

        DownloadInfo clientDownload = clientJson.downloads().get("client");

        System.out.println("Downloading %s".formatted(clientDownload.url));
        downloadManager.downloadFile(clientDownload.url, clientPath).join();
        System.out.println("Done");
        
        return clientPath;
    }

    private void downloadLibraries(Path libsDirectoryPath) throws IOException, InterruptedException, URISyntaxException {
        Files.createDirectories(libsDirectoryPath);

        var futures = new ArrayList<CompletableFuture<HttpResponse<Path>>>();
        for (LibraryInfo lib : clientJson.libraries()) {
            if (!lib.isAllowed(context)) {
                System.out.println("⚠️ Skipping %s".formatted(lib.name));
                continue;
            }
            URI downloadURI = lib.download.url;
            Path libFilePath = getLibPath(libsDirectoryPath, lib);
            System.out.println(libFilePath.toString() + " " + downloadURI.toString());
            Files.createDirectories(libFilePath.getParent());
            futures.add(downloadManager.downloadFile(downloadURI, libFilePath));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private Path getLibPath(Path libsDirectoryPath, LibraryInfo lib) {
        StringBuilder sb = new StringBuilder("");

        String[] packageNameVersion = lib.name.split(":", 4);

        String _package = packageNameVersion[0];
        String[] packageParts = _package.split("\\.");
        String _name = packageNameVersion[1];
        String _version = packageNameVersion[2];

        for (String part : packageParts) {
            sb.append(part);
            sb.append("/");
        }
        sb.append(_name);
        sb.append("/");
        sb.append(_version);
        sb.append("/");
        sb.append(_name);
        sb.append("-");
        sb.append(_version);

        if (packageNameVersion.length > 3) {
            sb.append("-");
            sb.append(packageNameVersion[3]);
        }
        sb.append(".jar");

        return libsDirectoryPath.resolve(sb.toString());
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
