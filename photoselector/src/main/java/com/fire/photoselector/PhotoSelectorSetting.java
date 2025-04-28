package com.fire.photoselector;

/**
 * Created by Fire on 2017/4/10.
 */

public class PhotoSelectorSetting {
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
    /**
     * PhotoView宽高比例,用于判断图片高度是否超出屏幕范围
     */
    public static float SCREEN_RATIO;
    /**
     * 状态栏高度
     */
    public static int STATUS_BAR_HEIGHT;
    /**
     * 是否选择原图
     */
    public static boolean IS_SELECTED_ORIGINAL_IMAGE = false;
    public static int ITEM_SIZE;
}
