package ru.server53.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

public class StartupManager {
    static String MC_DIR = "minecraft-maybe-required-data";

    static String username;
    static String version;
    static String assetIndex;
    static UUID uuid;
    static String classpath;


    public static void launch(String version, String username) throws Exception {
        System.out.println("Game starts here!");

        Path versionDir = Paths.get(MC_DIR, "versions", version);
        JSONObject versionJson = new JSONObject(Files.readString(versionDir.resolve(version + ".json")));

        uuid = UUID.nameUUIDFromBytes(username.getBytes());
        
        String mainClass = versionJson.getString("mainClass");

        assetIndex = versionJson.getJSONObject("assetIndex").getString("id");

        List<String> command = new ArrayList<>();

        command.add("java");
        command.add("-Xmx8G");

        command.add("-cp");
        command.add(buildClasspath(versionJson, version));

        command.add(mainClass);

        command.add("--uuid");
        command.add(username);

        command.add("--accessToken");
        command.add("0");

        command.add("--gameDir");
        command.add(MC_DIR);

        command.add("--assetDir");
        command.add(Paths.get(MC_DIR, "assets").toString());

        command.add("--assetIndex");
        command.add(assetIndex);

        command.add("--usertype");
        command.add("mojang");

        System.out.println(command.toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        pb.start();
    }


    private static String buildClasspath(JSONObject versionJson, String version) {
        StringBuilder cp = new StringBuilder();

        String sep = "";
        
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            sep = ";";
        } else {
            sep = ":";
        }

        JSONArray libs = versionJson.getJSONArray("libraries");

        for (int i = 0; i < libs.length(); i++) {
            JSONObject lib = libs.getJSONObject(i);

            Path libPath = null;

            if (lib.has("name"))
            {
                String name = lib.getString("name");
                String[] parts = name.split(":");

                String group = parts[0].replace('.', '/');
                String artifact = parts[1];
                String mavenVersion = parts[2];

                String pathStr = group + "/" + artifact + "/" + mavenVersion + "/" + artifact + "-" + mavenVersion + ".jar";
                
                libPath = Paths.get(MC_DIR, "libraries", pathStr);
            } else if (
                lib.has("downloads") 
                && lib.getJSONObject("downloads").has("artifact")
            ) {
                JSONObject artifact = lib.getJSONObject("downloads").getJSONObject("artifact");
                
                String path = artifact.getString("url");

                libPath = Paths.get(MC_DIR, "libraries", path);

            } 

            if (libPath != null && Files.exists(libPath)) {
                cp.append(libPath.toAbsolutePath()).append(sep);
            }
        }

        Path jar = Paths.get(MC_DIR, "versions", version, version + ".jar");

        cp.append(jar.toAbsolutePath());

        return cp.toString();
    }

}
