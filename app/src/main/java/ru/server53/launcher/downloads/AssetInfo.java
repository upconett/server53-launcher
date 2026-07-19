package ru.server53.launcher.downloads;

public record AssetInfo (
    String name,
    String hash,
    Long size
) {};
