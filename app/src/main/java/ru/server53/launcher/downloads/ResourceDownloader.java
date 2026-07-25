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

import ru.server53.launcher.MinecraftPathsBuilder;
import ru.server53.launcher.NativeLibUtils;
import ru.server53.launcher.clientjson.AssetIndexInfo;
import ru.server53.launcher.clientjson.ClientJson;
import ru.server53.launcher.clientjson.DownloadInfo;
import ru.server53.launcher.clientjson.LaunchContext;
import ru.server53.launcher.clientjson.LibraryInfo;


public class ResourceDownloader {
    private static final String ASSETS_DOWNLOAD_URL = "https://resources.download.minecraft.net";

    private final DownloadManager downloadManager;
    private final ClientJson clientJson;
    private final MinecraftPathsBuilder pathBuilder;
    private final LaunchContext context;

    private final int MAX_RETRIES = 5;

    public ResourceDownloader(
        DownloadManager downloadManager,
        ClientJson clientJson,
        MinecraftPathsBuilder pathBuilder,
        LaunchContext context
    ) {
        this.downloadManager = downloadManager;
        this.clientJson = clientJson;
        this.pathBuilder = pathBuilder;
        this.context = context;
    }

    public void downloadGame() throws IOException, InterruptedException, URISyntaxException {
        System.out.println("Downloading asset index:");
        Path assetIndexJsonPath = downloadAssetIndex();

        Map<String, AssetInfo> assetsInfo = AssetIndexParser.parse(assetIndexJsonPath);

        System.out.println("Downloading assets:");
        downloadAssets(assetsInfo);

        System.out.println("Downloading libraries:");
        downloadLibraries();

        System.out.println("Downloading client:");
        downloadClientJar();

        System.out.println("Unpacking natives:");
        unpackNatives();
    }

    private void unpackNatives() throws IOException {
        Path nativesDirectory = pathBuilder.getNativesDirectory(clientJson.id());
        for (LibraryInfo lib : clientJson.libraries()) {
            if (!lib.isAllowed(context)) { continue; }
            Path libFilePath = pathBuilder.getLibraryPath(lib);
            if (Files.notExists(libFilePath)) {
                throw new IOException("Natives lib '%s' does not exist, but required".formatted(libFilePath));
            }
            if (NativeLibUtils.isSuitableNativeLib(lib, context)) { 
                System.out.println("Unpacking %s".formatted(libFilePath));
                unpackNative(libFilePath, nativesDirectory);
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
                Files.deleteIfExists(target);
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

    private void downloadClientJar() throws IOException {
        Path clientPath = pathBuilder.getVersionJarPath(clientJson.id());

        Files.createDirectories(clientPath.getParent());
        if (Files.exists(clientPath)) { return; } // TODO: add size + sha1 checking

        DownloadInfo clientDownload = clientJson.downloads().get("client");

        System.out.println("Downloading %s".formatted(clientDownload.url));
        downloadManager.downloadFile(clientDownload.url, clientPath).join();
        System.out.println("Done");
    }

    private void downloadLibraries() 
    throws IOException, InterruptedException, URISyntaxException
    {
        LibraryInfo[] yetToDownload = checkLibraries();
        var futures = new ArrayList<CompletableFuture<HttpResponse<Path>>>();
        for (LibraryInfo lib : yetToDownload) {
            if (!lib.isAllowed(context)) {
                System.out.println("⚠️ Skipping %s".formatted(lib.name));
                continue;
            }
            URI downloadURI = lib.download.url;
            Path libFilePath = pathBuilder.getLibraryPath(lib);
            System.out.println(libFilePath.toString() + " " + downloadURI.toString());
            Files.createDirectories(libFilePath.getParent());
            futures.add(downloadManager.downloadFile(downloadURI, libFilePath));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private LibraryInfo[] checkLibraries() {
        var yetToDownload = new ArrayList<LibraryInfo>();
        for (LibraryInfo lib : clientJson.libraries()) {
            if (!lib.isAllowed(context)) { continue; }
            Path libFilePath = pathBuilder.getLibraryPath(lib);
            if (Files.exists(libFilePath)) { continue; }  // TODO: add size + sha1 checking
            yetToDownload.add(lib);
        }
        return yetToDownload.toArray(LibraryInfo[]::new);
    }


    private Path downloadAssetIndex()
    throws IOException, InterruptedException
    {
        AssetIndexInfo assetIndex = clientJson.assetIndex();
        Path assetIndexJsonPath = pathBuilder.getAssetsIndexPath(assetIndex.id);
        URI downloadURL = assetIndex.download.url;

        // TODO: add size + sha1 checking
        if (Files.exists(assetIndexJsonPath)) {
            return assetIndexJsonPath;
        }
        Files.createDirectories(assetIndexJsonPath.getParent());

        System.out.println("Downloading '%s'...    ".formatted(assetIndexJsonPath.toString()));
        downloadManager.downloadFile(downloadURL, assetIndexJsonPath).join();
        System.out.println("✅");

        return assetIndexJsonPath;
    }

    private Map<String, AssetInfo> checkAssets(Map<String, AssetInfo> assetsInfo) 
    throws IOException
    {
        Map<String, AssetInfo> notPresentedMap = new HashMap<>();
        notPresentedMap.putAll(assetsInfo);

        Path assetsObjectsDirectory = pathBuilder.getAssetsObjectsDirectory();
        Files.createDirectories(assetsObjectsDirectory);

        int total = assetsInfo.size();
        int notPresented = 0;

        for (var entry : assetsInfo.entrySet()) {
            AssetInfo assetInfo = entry.getValue();
            Path assetPath = pathBuilder.getAssetPath(assetInfo);
            if (!Files.exists(assetPath)) {
                notPresented++;
                System.out.println( "Not presented '%s'".formatted(assetInfo.hash()));  // TODO: add size + sha1 checking
            } else {
                notPresentedMap.remove(entry.getKey());
            }
        }
        System.out.println("Files presented %d/%d".formatted(total-notPresented, total));
        return notPresentedMap;
    }
    
    private void downloadAssets(Map<String, AssetInfo> assetsInfo) 
    throws IOException, URISyntaxException, InterruptedException
    {
        Map<String, AssetInfo> yetToDownload = checkAssets(assetsInfo);
        var downloadedCounter = new DownloadCounter(yetToDownload.size());
        var futures = new ArrayList<CompletableFuture<HttpResponse<Path>>>();
        for (AssetInfo assetInfo : yetToDownload.values()) {
            futures.add(downloadAssetWithRetry(
                assetInfo,
                0, 
                downloadedCounter
            ));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }


    private CompletableFuture<HttpResponse<Path>> downloadAssetWithRetry(
        AssetInfo assetInfo,
        int attempt,
        DownloadCounter downloadedCounter
    ) throws IOException, URISyntaxException, InterruptedException {
        if (attempt >= MAX_RETRIES) {
            System.out.println("Reached MAX_RETRIES");
            return CompletableFuture.failedFuture(new RuntimeException("Hit MAX_RETRIES"));
        }

        Path assetPath = pathBuilder.getAssetPath(assetInfo);

        Files.createDirectories(assetPath.getParent());
        Files.deleteIfExists(assetPath);

        String assetURLPath = "%s/%s".formatted(assetPath.getParent().getFileName(), assetInfo.hash());
        URI downloadURI = new URI(ASSETS_DOWNLOAD_URL).resolve(assetURLPath);
        System.out.println(downloadURI);

        // TODO: move retries to download manager
        return downloadManager.downloadFile(downloadURI, assetPath)
            .thenCompose((response) -> {
                try {
                    if (response.statusCode() == 200
                        && Files.size(assetPath) == assetInfo.size()
                    ) {
                        downloadedCounter.increment();
                        System.out.println(
                            "%d/%d, %s".formatted(
                                downloadedCounter.getCurrent(),
                                downloadedCounter.getTotal(),
                                response.toString()
                            ));
                        return CompletableFuture.completedFuture(response);
                    } else {
                        System.out.println(response.statusCode());
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
