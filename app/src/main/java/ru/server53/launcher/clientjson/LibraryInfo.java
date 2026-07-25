package ru.server53.launcher.clientjson;


public class LibraryInfo implements RuleControlled {
    public final String name;
    public final Boolean downloadOnly;
    public final DownloadInfo download;

    public final String groupId;
    public final String artifactId;
    public final String version;
    public final String classifier;

    private final RuleChecker ruleChecker;


    public LibraryInfo(
        String name,
        Boolean downloadOnly,
        DownloadInfo download,
        AllowanceRule[] rules
    ) {
        this.name = name;

        var nameParts = name.split(":", 4);
        if (nameParts.length < 3) {
            throw new IllegalArgumentException("Invalid library name: " + name);
        }

        this.groupId = nameParts[0];
        this.artifactId = nameParts[1];
        this.version = nameParts[2];
        this.classifier = (nameParts.length > 3) ? nameParts[3] : "";

        this.downloadOnly = downloadOnly;
        this.download = download;
        this.ruleChecker = new RuleChecker(rules);
    }

    public boolean isAllowed(LaunchContext context) {
        return ruleChecker.checkAllowance(context);
    }
}
