package com.fire.photoselector.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;

import com.bm.library.PhotoView;
import com.bumptech.glide.Glide;
import com.fire.photoselector.R;
import com.fire.photoselector.models.PhotoSelectorSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fire on 2017/4/11.
 */

public class PhotoViewAdapter extends PagerAdapter {
    private static final String TAG = "PhotoViewAdapter";
    private Context context;
    private List<String> list;
    private PhotoView photoView;
    private BitmapFactory.Options options;
    private List<PhotoView> photoViewList = new ArrayList<>();
    /**
     * 照片宽高比例
     */
    private float photoRatio;
    private OnPhotoViewClickListener onPhotoViewClickListener;

    public PhotoViewAdapter(Context context, List<String> list) {
        // 只初始化4个PhotoView,防止内存溢出
        for (int x = 0; x < 4; x++) {
            PhotoView pv = new PhotoView(context);
            pv.setBackgroundResource(R.color.photoViewBGColor);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            pv.setLayoutParams(layoutParams);
            photoViewList.add(pv);
        }
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(photoViewList.get(position % 4));
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        photoView = photoViewList.get(position % 4);
        photoView.setOnClickListener(v -> {
            if (onPhotoViewClickListener != null) {
                onPhotoViewClickListener.onPhotoClick(photoView, position);
            }
        });
        options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(list.get(position), options);
        photoRatio = (float) options.outWidth / options.outHeight;
        // 如果当前照片宽高比小于PhotoView宽高比,说明图片的高度超出了屏幕范围,需要从图片最上方显示,设置ScaleType.FIT_START
        // 其余情况设置ScaleType.FIT_CENTER
        if (photoRatio < PhotoSelectorSetting.SCREEN_RATIO) {
            photoView.setScaleType(ImageView.ScaleType.FIT_START);
        } else {
            photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        photoView.enable();
        Glide.with(context).load(list.get(position)).into(photoView);
        container.addView(photoView);
        return photoView;
    }

    public void setOnPhotoViewClickListener(OnPhotoViewClickListener onPhotoViewClickListener) {
        this.onPhotoViewClickListener = onPhotoViewClickListener;
    }

    public interface OnPhotoViewClickListener {
        void onPhotoClick(PhotoView view, int position);
    }
}