package com.fire.photoselector.models;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.bumptech.glide.module.GlideModule;

import java.io.File;

public final class MyGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        String path = context.getExternalFilesDir("Cache").getAbsolutePath() + File.separator + "/GlideImage";
        int diskCacheSizeBytes = 1024 * 1024 * 250; // 250 MB
        builder.setDiskCache(new DiskLruCacheFactory(path, diskCacheSizeBytes));
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {

    }
}
