package ru.server53.launcher;

import java.nio.file.Path;
import java.nio.file.Paths;

import ru.server53.launcher.clientjson.ClientJson;
import ru.server53.launcher.clientjson.ClientJsonParser;
import ru.server53.launcher.downloads.AssetDownloader;
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

        DownloadManager downloadManager = DownloadManager.ofDefaultConfiguration();
        AssetDownloader assetDownloader = new AssetDownloader(clientJson, downloadManager);

        Path newGamePath = Paths.get("fastGame");

        assetDownloader.downloadGameTo(newGamePath);
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
