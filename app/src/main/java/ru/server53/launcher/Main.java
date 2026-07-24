package ru.server53.launcher;

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

    protected static void parseClientJson() throws Exception {
        Path clientJsonPath = Paths.get(NeoForge_1_21_1_ClientJsonPath);
        ClientJsonParser parser = new ClientJsonParser(clientJsonPath);
        ClientJson clientJson = parser.parse();

        Path newGamePath = Paths.get("fastGame").toAbsolutePath();

        var cpb = new ClassPathBuilder(newGamePath, clientJson);
        String classPath = cpb.build(clientJson.libraries());

        String USERNAME = "upconett";
        String UUID = "82d680cb-867f-49b1-84bd-b4a7b85971d8";
        String ACCESS_TOKEN = "";

        var context = LaunchContext.ofDefaultValues(
            USERNAME,
            UUID,
            ACCESS_TOKEN,
            classPath,
            newGamePath,
            clientJson
        );

        DownloadManager downloadManager = DownloadManager.ofDefaultConfiguration();
        ResourceDownloader resourceDownloader = new ResourceDownloader(downloadManager, clientJson, context);

        resourceDownloader.downloadGame(newGamePath);

        var launchManager = new LaunchManager(newGamePath, USERNAME, UUID, ACCESS_TOKEN, clientJson, context);
        launchManager.launchGame();
    }

    // protected static void oldCrap() throws Exception {
    //     String uuid = UUID.randomUUID().toString();
    //     VersionInfoParser parser = new VersionInfoParser(
    //         // "/Users/upco/Documents/projects/minecraft/Server53Launcher/minecraft-maybe-required-data",
    //         "/Users/upco/minecraft-test",
    //         "OptiFine 1.12.2",
    //         "upconett", 
    //         uuid,
    //         ""
    //     );
    //     String[] command = parser.parse();
    //     for (String arg : command) System.out.print("\""+arg+"\" ");
    //     System.out.println();
    //     ProcessBuilder pb = new ProcessBuilder(command);
    //     pb.inheritIO();
    //     pb.start();
    // }

    public static void main(String[] args) throws Exception {
        parseClientJson();
    }
}
