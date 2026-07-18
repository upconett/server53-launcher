package ru.server53.launcher;

public class OSUtils {
    public static String getName() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("win")) {
            return "windows";
        } else if (osName.startsWith("linux")) {
            return "linux";
        } else if (osName.startsWith("mac")) {
            return "osx";
        } else {
            return "un";
        }
    }

    public static boolean isWindows() {
        return getName() == "windows";
    }

    public static boolean isLinux() {
        return getName() == "linux";
    }

    public static boolean isMacOS() {
        return getName() == "osx";
    }
}
