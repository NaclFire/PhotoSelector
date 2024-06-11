package com.fire.photoselector.utils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by Fire on 2017/4/12.
 */

public class FileUtils {
    public static String getSizeString(long size) {
        float f = (float) size / (1024 * 1024);
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(f) + "MB";
    }

    public static long getFileLength(List<String> fileList) {
        long size = 0;
        for (String selectedPhoto : fileList) {
            size += new File(selectedPhoto).length();
        }
        return size;
    }
}
