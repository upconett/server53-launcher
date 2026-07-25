package ru.server53.launcher;

import ru.server53.launcher.clientjson.LaunchContext;
import ru.server53.launcher.clientjson.LibraryInfo;


public class NativeLibUtils {
    public static boolean isNativeLib(LibraryInfo lib) {
        return lib.classifier.startsWith("natives");
    }

    public static boolean isSuitableNativeLib(LibraryInfo lib, LaunchContext context) {
        if (!isNativeLib(lib)) { return false; }
        String osArch = getOSArch(lib);
        return osArch.equals(context.osArch());
    }

    public static String getOSArch(LibraryInfo lib) {
        if (!isNativeLib(lib)) { return ""; }
        String classifier = lib.classifier;
        if (classifier.contains("mac")) {
            if (classifier.contains("arm")) {
                return "osx-arm";
            } else {
                return "osx";
            }
        } else if (classifier.contains("win")) {
            if (classifier.contains("arm")) {
                return "windows-arm";
            } else if (classifier.contains("x86")) {
                return "windows-x86";
            } else {
                return "windows";
            }
        } else {
            return "linux";
        }
    }
}
