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

    public static String getOSArch() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        
        // Check for Windows
        if (osName.contains("win")) {
            return detectWindowsArch(osArch);
        }
        
        // Check for macOS
        if (osName.contains("mac")) {
            return detectMacArch(osArch);
        }
        
        // Check for Linux
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return "linux";
        }
        
        // Default fallback
        return "linux";
    }
    
    private static String detectWindowsArch(String arch) {
        // ARM64 Windows
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "windows-arm";
        }
        
        // 32-bit x86 Windows
        if (arch.equals("x86") || arch.equals("i386") || arch.equals("i486") || 
            arch.equals("i586") || arch.equals("i686")) {
            return "windows-x86";
        }
        
        // 64-bit x86_64 Windows (default for Windows)
        return "windows";
    }
    
    private static String detectMacArch(String arch) {
        // Apple Silicon (ARM64)
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "osx-arm";
        }
        
        // Intel macOS (default)
        return "osx";
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
