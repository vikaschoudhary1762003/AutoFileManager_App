package com.example.autofilemanager;
import android.net.Uri;


public class DocumentModel {
    private final String name;
    private final String path;
    private final Uri uri;
    private final long size;
    private final String mimeType;

    public DocumentModel(String name, String path, Uri uri, long size, String mimeType) {
        this.name = name;
        this.path = path;
        this.uri = uri;
        this.size = size;
        this.mimeType = mimeType;
    }
    public String getName() { return name; }
    public String getPath() { return path; }
    public Uri getUri() { return uri; }
    public long getSize() { return size; }
    public String getMimeType() { return mimeType; }
}
