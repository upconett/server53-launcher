package ru.server53.launcher;

public class OSUtils {
    public static String getOS() {
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

    public static String getArch() {
        String arch = System.getProperty("os.arch");
        if (arch.equals("aarch64")) {
            return "arm64";
        }
        return "s";
    }

    public static String getOSWithArch() {
        String os = getOS();
        String arch = System.getProperty("os.arch");
        String osWithArch = os+", "+arch;
        System.out.println(osWithArch);
        return osWithArch;
    }

    public static boolean isWindows() {
        return getOS() == "windows";
    }

    public static boolean isLinux() {
        return getOS() == "linux";
    }

    public static boolean isMacOS() {
        return getOS() == "osx";
    }
}
