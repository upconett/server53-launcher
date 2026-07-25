package ru.server53.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.server53.launcher.clientjson.ClientJson;
import ru.server53.launcher.clientjson.ClientJsonParser;
import ru.server53.launcher.clientjson.LaunchContext;
import ru.server53.launcher.downloads.ResourceDownloader;
import ru.server53.launcher.downloads.DownloadManager;


public class Main {
    private final static String NeoForge_1_21_1_ClientJsonPath = "/Users/upco/Documents/projects/minecraft/Server53Launcher/minecraft-maybe-required-data/versions/NeoForge 1.21.1/NeoForge 1.21.1.json";

    protected static void printHello() {
        System.out.println("Hello from Server53 Launcher <3");
    }

    protected static void startup() throws Exception {
        StartupManager.launch("NeoForge 1.21.1", "upconett");
    }

    protected static void parseClientJson(Path clientJsonPath) throws Exception {
        ClientJsonParser parser = new ClientJsonParser(clientJsonPath);
        ClientJson clientJson = parser.parse();

        var pathBuilder = new MinecraftPathsBuilder("minecraft");

        System.out.println("Downloading to '%s'".formatted(pathBuilder.getRootDirectory()));

        var classPathBuilder = new ClassPathBuilder(clientJson, pathBuilder);
        String classPath = classPathBuilder.build(clientJson.libraries());

        String USERNAME = "upconett";
        String UUID = "82d680cb-867f-49b1-84bd-b4a7b85971d8";
        String ACCESS_TOKEN = "";

        var context = LaunchContext.ofDefaultValues(
            USERNAME,
            UUID,
            ACCESS_TOKEN,
            classPath,
            pathBuilder,
            clientJson
        );

        DownloadManager downloadManager = DownloadManager.ofDefaultConfiguration();
        ResourceDownloader resourceDownloader = new ResourceDownloader(
            downloadManager,
            clientJson,
            pathBuilder,
            context
        );

        resourceDownloader.downloadGame();
        var launchManager = new LaunchManager(clientJson, context);
        launchManager.launchGame();
    }

    public static void main(String[] args) throws Exception {
        Path clientJsonPath = Paths.get(NeoForge_1_21_1_ClientJsonPath);
        if (args.length < 1) { 
            if (Files.notExists(clientJsonPath)) {
                System.out.println("Enter client.json path as first argument");
                return;
            }
        } else {
            clientJsonPath = Paths.get(args[0]).toAbsolutePath();
        }
        if (Files.notExists(clientJsonPath)) {
            System.out.println("File '%s' does not exist".formatted(clientJsonPath));
        }
        parseClientJson(clientJsonPath);
    }
}
