package ru.server53.launcher.clientjson;

import java.net.URISyntaxException;

public class AssetIndexInfo {
    public final String id;
    public final Long totalSize;
    public final Boolean known;
    public final DownloadInfo download;

    public AssetIndexInfo(
        String id,
        Long totalSize,
        Boolean known,
        String url,
        String sha1,
        Long size
    ) throws URISyntaxException {
        this.id = id;
        this.totalSize = totalSize;
        this.known = known;
        this.download = new DownloadInfo(url, sha1, size);
    }
}
