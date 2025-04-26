package com.fire.photoselector.bean;

import android.net.Uri;

import java.io.Serializable;
import java.util.Objects;

public class ImagePathBean implements Serializable {
    private String path;
    private String uriString;

    public ImagePathBean(String path, Uri uri) {
        this.path = path;
        this.uriString = uri != null ? uri.toString() : null;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Uri getUri() {
        return uriString != null ? Uri.parse(uriString) : null;
    }

    public void setUri(Uri uri) {
        this.uriString = uri != null ? uri.toString() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImagePathBean that = (ImagePathBean) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
