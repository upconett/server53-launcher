package ru.server53.launcher.clientjson;

import java.net.URI;
import java.net.URISyntaxException;


public class DownloadInfo {
    public final URI url;
    public final String sha1;
    public final Long size;

    public DownloadInfo(
        String url,
        String sha1,
        Long size
    ) throws URISyntaxException {
        this.url = new URI(url);
        this.sha1 = sha1;
        this.size = size;
    }
}
