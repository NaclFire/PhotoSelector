package com.fire.photoselector.activity;

import static com.fire.photoselector.models.PhotoSelectorSetting.COLUMN_COUNT;
import static com.fire.photoselector.models.PhotoSelectorSetting.IS_SELECTED_ORIGINAL_IMAGE;
import static com.fire.photoselector.models.PhotoSelectorSetting.IS_SHOW_SELECTED_ORIGINAL_IMAGE;
import static com.fire.photoselector.models.PhotoSelectorSetting.ITEM_SIZE;
import static com.fire.photoselector.models.PhotoSelectorSetting.MAX_PHOTO_SUM;
import static com.fire.photoselector.models.PhotoSelectorSetting.PHOTOS_LIST_TRANSFER;
import static com.fire.photoselector.models.PhotoSelectorSetting.SCREEN_RATIO;
import static com.fire.photoselector.models.PhotoSelectorSetting.SELECTED_PHOTOS;
import static com.fire.photoselector.models.PhotoSelectorSetting.STATUS_BAR_HEIGHT;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fire.photoselector.R;
import com.fire.photoselector.adapter.FolderListAdapter;
import com.fire.photoselector.adapter.PhotoListAdapter;
import com.fire.photoselector.bean.ImageFolderBean;
import com.fire.photoselector.databinding.ActivityPhotoSelectorBinding;
import com.fire.photoselector.models.PhotoSelectorSetting;
import com.fire.photoselector.utils.ACache;
import com.fire.photoselector.utils.FileUtils;
import com.fire.photoselector.utils.ScreenUtil;
import com.fire.photoselector.view.PreloadGridLayoutManager;
import com.fire.photoselector.view.PreloadLinearLayoutManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Fire on 2017/4/8.
 */

public class PhotoSelectorActivity extends AppCompatActivity implements OnClickListener {
    private static final String TAG = "PhotoSelectorActivity";
    private static final int REQUEST_PREVIEW_PHOTO = 100;
    private static final int MSG_REFRESH_PHOTO_ADAPTER = 0x01;
    private static final int MSG_REFRESH_FOLDER_ADAPTER = 0x02;
    private static OnPhotoSelectedCallback onPhotoSelectedCallback;

    /**
     * 保存相册目录名和相册所有照片路径
     */
    private ConcurrentHashMap<String, List<String>> photoGroupMap = new ConcurrentHashMap<>();
    /**
     * 保存相册目录名
     */
    private List<ImageFolderBean> photoFolders = new ArrayList<>();
    /**
     * 照片列表
     */
    private PhotoListAdapter photoListAdapter;
    /**
     * 目录列表
     */
    private FolderListAdapter folderListAdapter;
    /**
     * 所有的图片文件夹
     */
    private List<ImageFolderBean> folderBeanList = new ArrayList<>();
    private List<String> chileList;
    private List<String> currentPhotoFolder;
    private ActivityPhotoSelectorBinding binding;
    private MyHandler handler;
    private GetImagesThread getImagesThread;

    private static class MyHandler extends Handler {
        private WeakReference<PhotoSelectorActivity> reference;

        public MyHandler(PhotoSelectorActivity photoSelectorActivity) {
            reference = new WeakReference<>(photoSelectorActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PhotoSelectorActivity activity = reference.get();
            switch (msg.what) {
                case MSG_REFRESH_PHOTO_ADAPTER:
                    if (msg.arg1 == -1) {
                        activity.photoListAdapter.updatePhotoList(activity.binding.rvPhotoList, activity.currentPhotoFolder);
                    } else {
                        activity.photoListAdapter.notifyItemChanged(msg.arg1);
                    }
                    break;
                case MSG_REFRESH_FOLDER_ADAPTER:
                    activity.photoListAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }

    public static class Builder {

        public Builder setMaxPhotoSum(int maxPhotoSum) {
            MAX_PHOTO_SUM = maxPhotoSum;
            return this;
        }


        public Builder setColumnCount(int columnCount) {
            COLUMN_COUNT = columnCount;
            return this;
        }

        public Builder setShowSelectOrigin(boolean showSelectOrigin) {
            IS_SHOW_SELECTED_ORIGINAL_IMAGE = showSelectOrigin;
            return this;
        }

        public Builder setSelectedPhotos(List<String> selectedPhotos) {
            SELECTED_PHOTOS.clear();
            SELECTED_PHOTOS.addAll(selectedPhotos);
            return this;
        }

        public Builder setOnPhotoSelectedCallback(OnPhotoSelectedCallback onPhotoSelectedCallback) {
            PhotoSelectorActivity.onPhotoSelectedCallback = onPhotoSelectedCallback;
            return this;
        }

        public void build(Context context) {
            if (context != null) {
                Intent intent = new Intent(context, PhotoSelectorActivity.class);
                context.startActivity(intent);
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotoSelectorBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_no);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(R.color.textWriteColor));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        ITEM_SIZE = (ScreenUtil.getScreenWidth(this) - ScreenUtil.dp2px(this, COLUMN_COUNT * 2)) / COLUMN_COUNT;
        STATUS_BAR_HEIGHT = ScreenUtil.getStatusBarHeight(this);
        handler = new MyHandler(this);
        binding.tvSelectCancel.setOnClickListener(this);
        binding.tvAlbumName.setOnClickListener(this);
        binding.ivAlbumArrow.setOnClickListener(this);
        binding.btSelectOk.setOnClickListener(this);
        binding.btPreviewImage.setOnClickListener(this);
        binding.btSelectOriginalImage.setOnClickListener(this);
        binding.vAlpha.setOnClickListener(this);
        if (SELECTED_PHOTOS == null || SELECTED_PHOTOS.isEmpty()) {
            IS_SELECTED_ORIGINAL_IMAGE = false;
        }
        List<String> allPhoto = new ArrayList<>();
        photoGroupMap.put(getString(R.string.all_photos), new ArrayList<>());
        photoListAdapter = new PhotoListAdapter(this, allPhoto);
        if (COLUMN_COUNT <= 1) {
            binding.rvPhotoList.setLayoutManager(new PreloadLinearLayoutManager(this));
        } else {
            binding.rvPhotoList.setLayoutManager(new PreloadGridLayoutManager(this, COLUMN_COUNT));
        }
        binding.rvPhotoList.setItemViewCacheSize(200);
        binding.rvPhotoList.setDrawingCacheEnabled(true);
        binding.rvPhotoList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        binding.rvPhotoList.setHasFixedSize(true);
        binding.rvPhotoList.setAdapter(photoListAdapter);
//        binding.rvPhotoList.addOnScrollListener(new MyOnScrollListener());
        photoListAdapter.setOnRecyclerViewItemClickListener(new OnPhotoListClick());
        binding.rvFolderList.setLayoutManager(new LinearLayoutManager(this));
        ViewGroup.LayoutParams lp = binding.rvFolderList.getLayoutParams();
        lp.height = (int) (ScreenUtil.getScreenHeight(this) * 0.618);
        binding.rvFolderList.setLayoutParams(lp);
        folderListAdapter = new FolderListAdapter(this, photoFolders);
        folderListAdapter.setOnRecyclerViewItemClickListener(new OnFolderListClick());
        binding.rvFolderList.setAdapter(folderListAdapter);
        binding.btSelectOriginalImage.setVisibility(IS_SHOW_SELECTED_ORIGINAL_IMAGE ? View.VISIBLE : View.GONE);
        getImagesThread = new GetImagesThread();
        getImagesThread.start();
        changeOKButtonStatus();
    }


    /**
     * 扫描手机中所有图片
     */
    private class GetImagesThread extends Thread {
        private boolean running = true;

        @Override
        public void run() {
            currentPhotoFolder = ACache.get(PhotoSelectorActivity.this).getList("photo");
            if (currentPhotoFolder != null) {
                sendNotifyMsg(MSG_REFRESH_PHOTO_ADAPTER, -1);
                SystemClock.sleep(2000);
            }
            Cursor cursor = null;
            if (running) {
                Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";
                ContentResolver cr = getContentResolver();
                cursor = cr.query(imageUri, null, null, null, sortOrder);
                if (cursor == null) {
                    return;
                }
            }
            while (running && cursor.moveToNext()) {
                //获取图片的路径
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                Objects.requireNonNull(photoGroupMap.get(getString(R.string.all_photos))).add(path);
                //获取该图片的父路径名
                File file = new File(path).getParentFile();
                if (file != null) {
                    String parentName = file.getName();
                    //根据父路径名将图片放入到photoGroupMap中
                    List<String> key = photoGroupMap.get(parentName);
                    if (key == null) {
                        chileList = new ArrayList<>();
                        chileList.add(path);
                        photoGroupMap.put(parentName, chileList);
                    } else {
                        key.add(path);
                    }
                }
            }
            //扫描图片完成
            if (cursor != null)
                cursor.close();
            if (running) {
                photoFolders.addAll(subGroupOfImage(photoGroupMap));
                currentPhotoFolder = photoGroupMap.get(getString(R.string.all_photos));
                ACache.get(PhotoSelectorActivity.this).put("photo", photoGroupMap.get(getString(R.string.all_photos)));
                sendNotifyMsg(MSG_REFRESH_FOLDER_ADAPTER, -1);
                sendNotifyMsg(MSG_REFRESH_PHOTO_ADAPTER, -1);
            }
        }

        public void stopThread() {
            running = false;
        }
    }

    private List<String> value;

    private List<ImageFolderBean> subGroupOfImage(ConcurrentHashMap<String, List<String>> mGroupMap) {
        folderBeanList.clear();
        ImageFolderBean imageFolderBean;
        for (Map.Entry<String, List<String>> entry : mGroupMap.entrySet()) {
            imageFolderBean = new ImageFolderBean();
            String key = entry.getKey();
            imageFolderBean.setSelected(key.equals(getString(R.string.all_photos)));
            value = entry.getValue();
            imageFolderBean.setFolderName(key);
            imageFolderBean.setImageCounts(value.size());
            imageFolderBean.setImagePaths(value);
            if (key.equals(getString(R.string.all_photos))) {
                folderBeanList.add(0, imageFolderBean);
            } else {
                folderBeanList.add(imageFolderBean);
            }
        }
        return folderBeanList;
    }

    @Override
    public void onClick(View v) {
        if (v == binding.tvSelectCancel) {// 取消
            finish();
        } else if (v == binding.tvAlbumName || v == binding.ivAlbumArrow) {// 选择相册
            toggleFolderList();
        } else if (v == binding.btSelectOk) {// 确定按钮
            if (!SELECTED_PHOTOS.isEmpty()) {
                ArrayList<String> image = new ArrayList<>(SELECTED_PHOTOS);
                if (onPhotoSelectedCallback != null) {
                    onPhotoSelectedCallback.onPhotoSelected(image, IS_SELECTED_ORIGINAL_IMAGE);
                }
                finish();
            }
        } else if (v == binding.btPreviewImage) {// 预览照片
            if (!SELECTED_PHOTOS.isEmpty()) {
                Intent intent = new Intent(this, PhotoViewActivity.class);
                PHOTOS_LIST_TRANSFER.clear();
                PHOTOS_LIST_TRANSFER.addAll(SELECTED_PHOTOS);
                startActivityForResult(intent, REQUEST_PREVIEW_PHOTO);
            }
        } else if (v == binding.btSelectOriginalImage) {// 选择原图
            IS_SELECTED_ORIGINAL_IMAGE = !IS_SELECTED_ORIGINAL_IMAGE;
            changeOKButtonStatus();
        } else if (v == binding.vAlpha) {// 点击相册列表外部
            toggleFolderList();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            SCREEN_RATIO = (float) binding.vAlpha.getWidth() / binding.vAlpha.getHeight();
        }
    }

    private class OnFolderListClick implements FolderListAdapter.OnRecyclerViewItemClickListener {

        @Override
        public void onRecyclerViewItemClick(View v, int position) {
            toggleFolderSelected(position);
            folderListAdapter.notifyDataSetChanged();
            currentPhotoFolder = photoGroupMap.get(photoFolders.get(position).getFolderName());
            toggleFolderList();
            binding.tvAlbumName.setText(photoFolders.get(position).getFolderName());
//            photoListAdapter.updatePhotoList(binding.rvPhotoList, currentPhotoFolder);
            binding.tvAlbumName.postDelayed(new Runnable() {
                @Override
                public void run() {
//                    photoListAdapter.setData(currentPhotoFolder);
                    photoListAdapter.updatePhotoList(binding.rvPhotoList, currentPhotoFolder);
                }
            }, 100);
        }
    }

    private class OnPhotoListClick implements PhotoListAdapter.OnRecyclerViewItemClickListener {

        @Override
        public void onRecyclerViewItemClick(View v, int position) {
            if (v.getId() == R.id.iv_photo_checked) {
                boolean photoSelected = PhotoSelectorSetting.togglePhotoSelected(currentPhotoFolder.get(position));
                if (photoSelected) {
                    changeOKButtonStatus();
                } else {
                    String string = getString(R.string.photo_sum_max);
                    String format = String.format(string, MAX_PHOTO_SUM);
                    Toast.makeText(PhotoSelectorActivity.this, format, Toast.LENGTH_SHORT).show();
                }
                photoListAdapter.notifyItemChanged(position);
            } else {
                Intent intent = new Intent(PhotoSelectorActivity.this, PhotoViewActivity.class);
                PHOTOS_LIST_TRANSFER.clear();
                PHOTOS_LIST_TRANSFER.addAll(currentPhotoFolder);
                intent.putExtra("Index", position);
                startActivityForResult(intent, REQUEST_PREVIEW_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PREVIEW_PHOTO:
                if (resultCode == RESULT_OK) {
                    PHOTOS_LIST_TRANSFER.clear();
                    PHOTOS_LIST_TRANSFER.addAll(SELECTED_PHOTOS);
                    if (onPhotoSelectedCallback != null) {
                        onPhotoSelectedCallback.onPhotoSelected(PHOTOS_LIST_TRANSFER, IS_SELECTED_ORIGINAL_IMAGE);
                    }
                    finish();
                }
                photoListAdapter.notifyDataSetChanged();
                changeOKButtonStatus();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (binding.rvFolderList.isShown()) {
            toggleFolderList();
        } else {
            super.onBackPressed();
        }
    }

    private void toggleFolderList() {
        Animation animation;
        if (binding.rvFolderList.isShown()) {
            binding.vAlpha.setVisibility(View.INVISIBLE);
            binding.ivAlbumArrow.setImageResource(R.drawable.svg_arrow_down_yellow);
            animation = AnimationUtils.loadAnimation(this, R.anim.top_popup_hidden_anim_fast);
            binding.rvFolderList.setVisibility(View.GONE);
        } else {
            binding.rvFolderList.setVisibility(View.VISIBLE);
            binding.vAlpha.setVisibility(View.VISIBLE);
            binding.ivAlbumArrow.setImageResource(R.drawable.svg_arrow_up_yellow);
            animation = AnimationUtils.loadAnimation(this, R.anim.top_popup_show_anim);
        }
        binding.rvFolderList.setAnimation(animation);
    }

    private void toggleFolderSelected(int position) {
        for (ImageFolderBean photoFolder : photoFolders) {
            photoFolder.setSelected(false);
        }
        photoFolders.get(position).setSelected(true);
    }

    private void changeOKButtonStatus() {
        if (SELECTED_PHOTOS.isEmpty()) {
            binding.btSelectOk.setBackgroundResource(R.drawable.button_unclickable);
            binding.btSelectOk.setTextColor(getResources().getColor(R.color.textSecondColor));
            binding.btSelectOk.setText(getString(R.string.ok));
            binding.btPreviewImage.setTextColor(getResources().getColor(R.color.textSecondColor));
        } else {
            binding.btSelectOk.setBackgroundResource(R.drawable.button_clickable);
            binding.btSelectOk.setTextColor(getResources().getColor(R.color.textWriteColor));
            String string = getResources().getString(R.string.ok_with_number);
            String format = String.format(string, SELECTED_PHOTOS.size());
            binding.btSelectOk.setText(format);
            binding.btPreviewImage.setTextColor(getResources().getColor(R.color.textBlackColor));
        }
        if (IS_SELECTED_ORIGINAL_IMAGE) {
            String string = getString(R.string.original_image_with_size);
            String format = String.format(string, FileUtils.getSizeString(FileUtils.getFileLength(SELECTED_PHOTOS)));
            binding.btSelectOriginalImage.setText(format);
            Drawable drawable = getResources().getDrawable(R.drawable.svg_choose_original_image_checked);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            binding.btSelectOriginalImage.setCompoundDrawables(drawable, null, null, null);
        } else {
            binding.btSelectOriginalImage.setText(getString(R.string.full_image));
            Drawable drawable = getResources().getDrawable(R.drawable.svg_choose_original_image_default);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            binding.btSelectOriginalImage.setCompoundDrawables(drawable, null, null, null);
        }
    }

    private void sendNotifyMsg(int what, int index) {
        if (handler != null) {
            Message message = Message.obtain();
            message.what = what;
            message.arg1 = index;
            handler.sendMessage(message);
        }
    }

    public interface OnPhotoSelectedCallback {
        void onPhotoSelected(List<String> photoList, boolean isSelectOrigin);
    }

    private class MyOnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            switch (newState) {
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    // 拖动时
                    break;
                case RecyclerView.SCROLL_STATE_SETTLING:
                    // 惯性滑动时
                    Glide.with(PhotoSelectorActivity.this).pauseRequests();
                    break;
                case RecyclerView.SCROLL_STATE_IDLE:
                    // 静止时
                    Glide.with(PhotoSelectorActivity.this).resumeRequests();
                    break;
            }
        }
    }

    @Override
    public void finish() {
        if (getImagesThread != null) {
            getImagesThread.stopThread();
        }
        super.finish();
        overridePendingTransition(R.anim.slide_no, R.anim.slide_out_bottom);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onPhotoSelectedCallback = null; // 释放引用
    }
}
