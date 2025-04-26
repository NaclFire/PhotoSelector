package com.fire.photoselectortest;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    public static final int REQUEST_CODE_STORAGE = 1001;

    // 获取需要申请的权限列表
    public static String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6 ~ 12
            return new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
        } else {
            return new String[]{}; // API < 23，运行时权限不适用
        }
    }

    // 检查是否已全部授权
    public static boolean hasStoragePermissions(@NonNull Activity activity) {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // 请求权限
    public static void requestStoragePermissions(@NonNull Activity activity) {
        String[] permissions = getRequiredPermissions();
        List<String> toRequest = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(permission);
            }
        }

        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity, toRequest.toArray(new String[0]), REQUEST_CODE_STORAGE);
        }
    }
}

