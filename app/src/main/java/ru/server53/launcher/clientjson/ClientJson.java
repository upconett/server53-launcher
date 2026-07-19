package ru.server53.launcher.clientjson;

import java.util.Map;

public record ClientJson(
    String type,
    String mainClass,
    LibraryInfo[] libraries,
    Map<String, DownloadInfo> downloads,
    String assets,
    AssetIndexInfo assetIndex,
    LaunchArgument[] argumentsJVM,
    LaunchArgument[] argumentsGame,
    String javaVersionComponent,
    int javaMajorVersion
) { }
