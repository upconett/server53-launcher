package ru.server53.launcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

import ru.server53.launcher.clientjson.ClientJson;
import ru.server53.launcher.clientjson.LaunchArgument;
import ru.server53.launcher.clientjson.LaunchContext;
import ru.server53.launcher.clientjson.LibraryInfo;

public class LaunchManager {
    private final String MINECRAFT_VERSION;
    private final Path MINECRAFT_DIRECTORY;
    private final Path ALL_VERSIONS_PATH;
    private final Path LIBRARIES_PATH;
    private final Path VERSION_PATH;
    private final Path NATIVES_PATH;
    private final Path VERSION_JAR_PATH;
    private final Path ASSETS_PATH;
    private final String USERNAME;
    private final String USER_UUID;
    private final String USER_ACCESS_TOKEN;
    private final String CLASSPATH_SEPARATOR;

    private final ClientJson clientJson;
    private final LaunchContext context;


    public LaunchManager(
        Path minecraftDirectory,
        String username,
        String uuid,
        String accessToken,
        ClientJson clientJson,
        LaunchContext context
    ) throws IOException
    {
        this.clientJson = clientJson;
        this.context = context;

        USERNAME = username;
        USER_UUID = uuid;
        USER_ACCESS_TOKEN = accessToken;

        MINECRAFT_VERSION = clientJson.id();
        MINECRAFT_DIRECTORY = minecraftDirectory
            .toAbsolutePath()
            .normalize()
            .toRealPath();

        ALL_VERSIONS_PATH = MINECRAFT_DIRECTORY.resolve("versions");
        VERSION_PATH = ALL_VERSIONS_PATH.resolve(MINECRAFT_VERSION);
        VERSION_JAR_PATH = VERSION_PATH.resolve(MINECRAFT_VERSION+".jar");
        NATIVES_PATH = VERSION_PATH.resolve("natives");
        ASSETS_PATH = MINECRAFT_DIRECTORY.resolve("assets");
        LIBRARIES_PATH = MINECRAFT_DIRECTORY.resolve("libraries");

        CLASSPATH_SEPARATOR = context.isWindows() ? ";" : ":";
    }

    
    public void launchGame(

    ) throws IOException {
        List<String> allArgs = new ArrayList<>();
        allArgs.add("/Users/upco/Library/Application Support/tlauncher/mojang_jre/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home/bin/java");
        for (var arg : clientJson.argumentsJVM()) {
            if (arg.isAllowed(context)) {
                allArgs.addAll(List.of(arg.getPopulatedValues(context)));
            }
        }
        allArgs.add(clientJson.mainClass());
        for (var arg : clientJson.argumentsGame()) {
            if (arg.isAllowed(context)) {
                allArgs.addAll(List.of(arg.getPopulatedValues(context)));
            }
        }
        String[] command = allArgs.toArray(String[]::new);
        System.out.println("Using command: ");
        for (var arg : command) { System.out.print("\""+arg+"\" "); }
        System.out.println();
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        pb.start();
    }
}
