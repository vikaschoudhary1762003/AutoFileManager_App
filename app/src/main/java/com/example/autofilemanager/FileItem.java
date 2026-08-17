package com.example.autofilemanager;

import android.net.Uri;

public class FileItem {

    private String name;
    private long size;
    private String mimeType;
    private long dateModified;
    private String path;
    private Uri uri;

    public FileItem(
            String name,
            long size,
            String mimeType,
            long dateModified,
            String path,
            Uri uri
    ) {
        this.name = name;
        this.size = size;
        this.mimeType = mimeType;
        this.dateModified = dateModified;
        this.path = path;
        this.uri = uri;
    }

    // FILE NAME
    public String getName() {
        return name;
    }

    // FILE SIZE
    public long getSize() {
        return size;
    }

    // MIME TYPE
    public String getMimeType() {
        return mimeType;
    }

    // DATE MODIFIED
    public long getDateModified() {
        return dateModified;
    }

    // FILE PATH
    public String getPath() {
        return path;
    }

    // FILE URI
    public Uri getUri() {
        return uri;
    }
}