package com.fire.photoselector.models;

import com.fire.photoselector.bean.ImagePathBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fire on 2017/4/10.
 */

public class PhotoSelectorSetting {
    public static List<ImagePathBean> SELECTED_PHOTOS = new ArrayList<>();
    public static ArrayList<ImagePathBean> PHOTOS_LIST_TRANSFER = new ArrayList<>();
    /**
     * 最多可选照片数量
     */
    public static int MAX_PHOTO_SUM = 9;
    /**
     * 照片列表列数
     */
    public static int COLUMN_COUNT = 4;
    /**
     * 是否显示选择原图按钮
     */
    public static boolean IS_SHOW_SELECTED_ORIGINAL_IMAGE = true;
    public static final String LAST_MODIFIED_LIST = "last_modified_list";
    /**
     * PhotoView宽高比例,用于判断图片高度是否超出屏幕范围
     */
    public static float SCREEN_RATIO;
    /**
     * 状态栏高度
     */
    public static int STATUS_BAR_HEIGHT;
    /**
     * 选择原图
     */
    public static String SELECTED_ORIGINAL_IMAGE = "is_selected_original_image";
    /**
     * 是否选择原图
     */
    public static boolean IS_SELECTED_ORIGINAL_IMAGE = false;
    public static int ITEM_SIZE;

    public static boolean isPhotoSelected(ImagePathBean path) {
        return SELECTED_PHOTOS.contains(path);
    }

    public static boolean togglePhotoSelected(ImagePathBean imagePathBean) {
        if (SELECTED_PHOTOS.contains(imagePathBean)) {
            SELECTED_PHOTOS.remove(imagePathBean);
            return true;
        } else {
            if (SELECTED_PHOTOS.size() == MAX_PHOTO_SUM) {
                return false;
            } else {
                SELECTED_PHOTOS.add(imagePathBean);
                return true;
            }
        }
    }
}
