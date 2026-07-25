package ru.server53.launcher;

import java.nio.file.Path;
import java.nio.file.Paths;

import ru.server53.launcher.clientjson.LibraryInfo;
import ru.server53.launcher.downloads.AssetInfo;


public class MinecraftPathsBuilder {
    private final Path rootPath;

    MinecraftPathsBuilder(Path rootPath) {
        this.rootPath = rootPath.toAbsolutePath();
    }

    MinecraftPathsBuilder(String rootPath) {
        this(Paths.get(rootPath));
    }

    public Path getRootDirectory() {
        return rootPath;
    }

    public Path getLibrariesDirectory() {
        return rootPath.resolve("libraries");
    }

    public Path getVersionsDirectory() {
        return rootPath.resolve("versions");
    }

    public Path getVersionJarPath(String versionId) {
        return getVersionsDirectory()
            .resolve(versionId)
            .resolve(versionId+".jar");
    }

    public Path getNativesDirectory(String versionId) {
        return getVersionsDirectory()
            .resolve(versionId)
            .resolve("natives");
    }

    public Path getAssetsRootDirectory() {
        return getRootDirectory().resolve("assets");
    }

    public Path getAssetsIndexPath(String assetIndex) {
        return getAssetsRootDirectory()
            .resolve("indexes")
            .resolve(assetIndex+".json");
    }

    public Path getAssetsObjectsDirectory() {
        return getAssetsRootDirectory().resolve("objects");
    }

    public Path getAssetPath(AssetInfo asset) {
        return getAssetsObjectsDirectory()
            .resolve(asset.hash().substring(0,2))
            .resolve(asset.hash());
    }

    public Path getLibraryPath(LibraryInfo lib) {
        String fileName = lib.artifactId + "-" + lib.version
            + (lib.classifier.isEmpty() ? "" : "-" + lib.classifier)
            + ".jar";

        Path path = getLibrariesDirectory();

        for (String part : lib.groupId.split("\\.")) {
            path = path.resolve(part);
        }

        return path.resolve(lib.artifactId)
                   .resolve(lib.version)
                   .resolve(fileName);
    }
}
