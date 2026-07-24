package ru.server53.launcher;

import java.io.IOException;
import java.nio.file.Path;

import ru.server53.launcher.clientjson.ClientJson;
import ru.server53.launcher.clientjson.LibraryInfo;


public class ClassPathBuilder {
    private final Path LIBRARIES_PATH;
    private final Path VERSION_JAR_PATH;
    private final String CLASSPATH_SEPARATOR;

    ClassPathBuilder(Path minecraftDirectory, ClientJson clientJson) throws IOException {
        Path MINECRAFT_DIRECTORY = minecraftDirectory
            .toAbsolutePath()
            .normalize();
        LIBRARIES_PATH = MINECRAFT_DIRECTORY.resolve("libraries");
        CLASSPATH_SEPARATOR = OSUtils.isWindows() ? ";" : ":";
        VERSION_JAR_PATH = MINECRAFT_DIRECTORY.resolve("versions/%s/%s.jar".formatted(clientJson.id(), clientJson.id()));
    }

    public String build(LibraryInfo[] libs) {
        StringBuilder output = new StringBuilder();
        for (var lib : libs) {
            if (lib.downloadOnly) { continue; }
            output.append(getLibPath(LIBRARIES_PATH, lib).toString());
            output.append(CLASSPATH_SEPARATOR);
        }
        output.append(VERSION_JAR_PATH.toString());
        return output.toString();
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
}
