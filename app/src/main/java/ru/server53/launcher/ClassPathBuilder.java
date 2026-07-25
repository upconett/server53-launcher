package ru.server53.launcher;

import java.io.IOException;
import java.nio.file.Path;

import ru.server53.launcher.clientjson.ClientJson;
import ru.server53.launcher.clientjson.LibraryInfo;


public class ClassPathBuilder {
    private final static String CLASSPATH_SEPARATOR = OSUtils.isWindows() ? ";" : ":";

    private final ClientJson clientJson;
    private final MinecraftPathsBuilder pathBuilder;

    ClassPathBuilder(
        ClientJson clientJson,
        MinecraftPathsBuilder pathBuilder
    ) throws IOException {
        this.clientJson = clientJson;
        this.pathBuilder = pathBuilder;
    }

    public String build(LibraryInfo[] libs) {
        Path versionJsonPath = pathBuilder.getVersionJarPath(clientJson.id());

        StringBuilder output = new StringBuilder();

        for (var lib : libs) {
            if (lib.downloadOnly) { continue; }
            output.append(pathBuilder.getLibraryPath(lib));
            output.append(CLASSPATH_SEPARATOR);
        }
        output.append(versionJsonPath.toString());

        return output.toString();
    }   
}
