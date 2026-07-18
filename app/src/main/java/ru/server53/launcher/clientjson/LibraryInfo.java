package ru.server53.launcher.clientjson;


public class LibraryInfo implements RuleControlled {
    public final String name;
    public final Boolean downloadOnly;
    public final DownloadInfo download;

    private final RuleChecker ruleChecker;


    public LibraryInfo(
        String name,
        Boolean downloadOnly,
        DownloadInfo download,
        AllowanceRule[] rules
    ) {
        this.name = name;
        this.downloadOnly = downloadOnly;
        this.download = download;
        this.ruleChecker = new RuleChecker(rules);
    }

    public boolean isAllowed(LaunchContext context) {
        return ruleChecker.checkAllowance(context);
    }

    // ##############################
    // REALLY IMPORTANT, DO NOT ERASE
    // ##############################
    // 
    // public static String nameToPath(String name) {
    //     StringBuilder sb = new StringBuilder("");

    //     String[] packageNameVersion = name.split(":", 4);

    //     String _package = packageNameVersion[0];
    //     String[] packageParts = _package.split("\\.");
    //     String _name = packageNameVersion[1];
    //     String _version = packageNameVersion[2];

    //     for (String part : packageParts) {
    //         sb.append(part);
    //         sb.append("/");
    //     }
    //     sb.append(_name);
    //     sb.append("/");
    //     sb.append(_version);
    //     sb.append("/");
    //     sb.append(_name);
    //     sb.append("-");
    //     sb.append(_version);

    //     if (packageNameVersion.length > 3) {
    //         sb.append("-");
    //         sb.append(packageNameVersion[3]);
    //     }
    //     sb.append(".jar");

    //     return sb.toString();
    // }
}
