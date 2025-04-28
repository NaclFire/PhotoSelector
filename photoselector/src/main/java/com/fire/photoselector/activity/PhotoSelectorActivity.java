package com.fire.photoselector.activity;

import android.content.ContentResolver;
import android.content.ContentUris;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fire.photoselector.R;
import com.fire.photoselector.adapter.FolderListAdapter;
import com.fire.photoselector.adapter.PhotoListAdapter;
import com.fire.photoselector.bean.ImageFolderBean;
import com.fire.photoselector.bean.ImagePathBean;
import com.fire.photoselector.databinding.ActivityPhotoSelectorBinding;
import com.fire.photoselector.PhotoSelectorSetting;
import com.fire.photoselector.utils.ACache;
import com.fire.photoselector.utils.FileUtils;
import com.fire.photoselector.utils.ScreenUtil;
import com.fire.photoselector.view.PreloadGridLayoutManager;
import com.fire.photoselector.view.PreloadLinearLayoutManager;

import java.io.File;
import java.io.Serializable;
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
    private ConcurrentHashMap<String, List<ImagePathBean>> photoGroupMap = new ConcurrentHashMap<>();
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
     * 所有图片目录
     */
    private List<ImageFolderBean> imageFolderBeans = new ArrayList<>();
    private List<ImagePathBean> chileList;
    private List<ImagePathBean> value;
    private List<ImagePathBean> currentPhotoFolder;
    private ActivityPhotoSelectorBinding binding;
    private MyHandler handler;
    private GetImagesThread getImagesThread;
    /**
     * 已选照图片
     */
    private List<ImagePathBean> selectedPhotos = new ArrayList<>();

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
        private List<ImagePathBean> selectedPhotos;

        public Builder setMaxPhotoSum(int maxPhotoSum) {
            PhotoSelectorSetting.MAX_PHOTO_SUM = maxPhotoSum;
            return this;
        }


        public Builder setColumnCount(int columnCount) {
            PhotoSelectorSetting.COLUMN_COUNT = columnCount;
            return this;
        }

        public Builder setShowSelectOrigin(boolean showSelectOrigin) {
            PhotoSelectorSetting.IS_SHOW_SELECTED_ORIGINAL_IMAGE = showSelectOrigin;
            return this;
        }

        public Builder setSelectedPhotos(List<ImagePathBean> selectedPhotos) {
//            selectedPhotosString = Tools.getInstance().convertListToJson(selectedPhotos);
            this.selectedPhotos = selectedPhotos;
            return this;
        }

        public Builder setOnPhotoSelectedCallback(OnPhotoSelectedCallback onPhotoSelectedCallback) {
            PhotoSelectorActivity.onPhotoSelectedCallback = onPhotoSelectedCallback;
            return this;
        }

        public void build(Context context) {
            if (context != null) {
                Intent intent = new Intent(context, PhotoSelectorActivity.class);
                intent.putExtra("selectedPhotos", (Serializable) selectedPhotos);
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
        PhotoSelectorSetting.ITEM_SIZE = (ScreenUtil.getScreenWidth(this) -
                ScreenUtil.dp2px(this, PhotoSelectorSetting.COLUMN_COUNT * 2)) / PhotoSelectorSetting.COLUMN_COUNT;
        PhotoSelectorSetting.STATUS_BAR_HEIGHT = ScreenUtil.getStatusBarHeight(this);
        handler = new MyHandler(this);
        binding.tvSelectCancel.setOnClickListener(this::onClick);
        binding.tvAlbumName.setOnClickListener(this);
        binding.ivAlbumArrow.setOnClickListener(this);
        binding.btSelectOk.setOnClickListener(this);
        binding.btPreviewImage.setOnClickListener(this);
        binding.btSelectOriginalImage.setOnClickListener(this);
        binding.vAlpha.setOnClickListener(this);
        Intent intent = getIntent();
        selectedPhotos.addAll((List<ImagePathBean>) intent.getSerializableExtra("selectedPhotos"));
        if (selectedPhotos == null || selectedPhotos.isEmpty()) {
            PhotoSelectorSetting.IS_SELECTED_ORIGINAL_IMAGE = false;
        }
        binding.btSelectOriginalImage.setVisibility(PhotoSelectorSetting.IS_SHOW_SELECTED_ORIGINAL_IMAGE ? View.VISIBLE : View.GONE);
        List<ImagePathBean> allPhoto = new ArrayList<>();
        photoGroupMap.put(getString(R.string.all_photos), new ArrayList<>());
        photoListAdapter = new PhotoListAdapter(this, allPhoto);
        if (PhotoSelectorSetting.COLUMN_COUNT <= 1) {
            binding.rvPhotoList.setLayoutManager(new PreloadLinearLayoutManager(this));
        } else {
            binding.rvPhotoList.setLayoutManager(new PreloadGridLayoutManager(this, PhotoSelectorSetting.COLUMN_COUNT));
        }
        RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();
        viewPool.setMaxRecycledViews(0, 200);
        binding.rvPhotoList.setItemViewCacheSize(200);
        binding.rvPhotoList.setDrawingCacheEnabled(true);
        binding.rvPhotoList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        binding.rvPhotoList.setRecycledViewPool(viewPool);
        binding.rvPhotoList.setHasFixedSize(true);
        binding.rvPhotoList.setAdapter(photoListAdapter);
//        binding.rvPhotoList.addOnScrollListener(new MyOnScrollListener());
        photoListAdapter.setSelectedPhotos(selectedPhotos);
        photoListAdapter.setOnRecyclerViewItemClickListener(new OnPhotoListClick());
        binding.rvFolderList.setLayoutManager(new LinearLayoutManager(this));
        ViewGroup.LayoutParams lp = binding.rvFolderList.getLayoutParams();
        lp.height = (int) (ScreenUtil.getScreenHeight(this) * 0.618);
        binding.rvFolderList.setLayoutParams(lp);
        folderListAdapter = new FolderListAdapter(this, photoFolders);
        folderListAdapter.setOnRecyclerViewItemClickListener(new OnFolderListClick());
        binding.rvFolderList.setAdapter(folderListAdapter);
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
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long id = cursor.getLong(idColumn);
                Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                //获取图片的路径
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                Objects.requireNonNull(photoGroupMap.get(getString(R.string.all_photos))).add(new ImagePathBean(path, uri));
                //获取该图片的父路径名
                File file = new File(path).getParentFile();
                if (file != null) {
                    String parentName = file.getName();
                    //根据父路径名将图片放入到mGroupMap中
                    List<ImagePathBean> key = photoGroupMap.get(parentName);
                    if (key == null) {
                        chileList = new ArrayList<>();
                        chileList.add(new ImagePathBean(path, uri));
                        photoGroupMap.put(parentName, chileList);
                    } else {
                        key.add(new ImagePathBean(path, uri));
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

    private List<ImageFolderBean> subGroupOfImage(ConcurrentHashMap<String, List<ImagePathBean>> mGroupMap) {
        ImageFolderBean imageFolderBean;
        for (Map.Entry<String, List<ImagePathBean>> entry : mGroupMap.entrySet()) {
            imageFolderBean = new ImageFolderBean();
            String key = entry.getKey();
            imageFolderBean.setSelected(key.equals(getString(R.string.all_photos)));
            value = entry.getValue();
            imageFolderBean.setFolderName(key);
            imageFolderBean.setImageCounts(value.size());
            imageFolderBean.setImagePaths(value);
            if (key.equals(getString(R.string.all_photos))) {
                imageFolderBeans.add(0, imageFolderBean);
            } else {
                imageFolderBeans.add(imageFolderBean);
            }
        }
        return imageFolderBeans;
    }

    @Override
    public void onClick(View v) {
        if (v == binding.tvSelectCancel) {// 取消
            finish();
        } else if (v == binding.tvAlbumName || v == binding.ivAlbumArrow) {// 选择相册
            toggleFolderList();
        } else if (v == binding.btSelectOk) {// 确定按钮
            if (!selectedPhotos.isEmpty()) {
                if (onPhotoSelectedCallback != null) {
                    onPhotoSelectedCallback.onPhotoSelected(selectedPhotos, PhotoSelectorSetting.IS_SELECTED_ORIGINAL_IMAGE);
                }
                finish();
            }
        } else if (v == binding.btPreviewImage) {// 预览照片
            if (!selectedPhotos.isEmpty()) {
                Intent intent = new Intent(this, PhotoViewActivity.class);
                intent.putExtra("selectedPhotos", PhotoSelectorSetting.save(selectedPhotos));
                intent.putExtra("isShowPreview", false);
                startActivityForResult(intent, REQUEST_PREVIEW_PHOTO);
            }
        } else if (v == binding.btSelectOriginalImage) {// 选择原图
            PhotoSelectorSetting.IS_SELECTED_ORIGINAL_IMAGE = !PhotoSelectorSetting.IS_SELECTED_ORIGINAL_IMAGE;
            changeOKButtonStatus();
        } else if (v == binding.vAlpha) {// 点击相册列表外部
            toggleFolderList();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            PhotoSelectorSetting.SCREEN_RATIO = (float) binding.vAlpha.getWidth() / binding.vAlpha.getHeight();
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
                    photoListAdapter.setSelectedPhotos(selectedPhotos);
                    photoListAdapter.updatePhotoList(binding.rvPhotoList, currentPhotoFolder);
                }
            }, 100);
        }
    }

    private class OnPhotoListClick implements PhotoListAdapter.OnRecyclerViewItemClickListener {

        @Override
        public void onRecyclerViewItemClick(View v, int position) {
            if (v.getId() == R.id.iv_photo_checked) {
                boolean photoSelected = togglePhotoSelected(currentPhotoFolder.get(position));
                if (photoSelected) {
                    changeOKButtonStatus();
                } else {
                    String string = getString(R.string.photo_sum_max);
                    String format = String.format(string, PhotoSelectorSetting.MAX_PHOTO_SUM);
                    Toast.makeText(PhotoSelectorActivity.this, format, Toast.LENGTH_SHORT).show();
                }
                photoListAdapter.notifyItemChanged(position);
            } else {
                Intent intent = new Intent(PhotoSelectorActivity.this, PhotoViewActivity.class);
                intent.putExtra("selectedPhotos", PhotoSelectorSetting.save(selectedPhotos));
                intent.putExtra("currentPhotos", PhotoSelectorSetting.save(currentPhotoFolder));
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
                    if (data != null) {
                        selectedPhotos.clear();
                        selectedPhotos.addAll((List<ImagePathBean>) data.getSerializableExtra("selectedPhotos"));
                        if (onPhotoSelectedCallback != null) {
                            onPhotoSelectedCallback.onPhotoSelected(selectedPhotos, PhotoSelectorSetting.IS_SELECTED_ORIGINAL_IMAGE);
                        }
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
            folderListAdapter.setSelectedPhotos(selectedPhotos);
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
        if (selectedPhotos.isEmpty()) {
            binding.btSelectOk.setBackgroundResource(R.drawable.button_unclickable);
            binding.btSelectOk.setTextColor(getResources().getColor(R.color.textSecondColor));
            binding.btSelectOk.setText(getString(R.string.ok));
            binding.btPreviewImage.setTextColor(getResources().getColor(R.color.textSecondColor));
        } else {
            binding.btSelectOk.setBackgroundResource(R.drawable.button_clickable);
            binding.btSelectOk.setTextColor(getResources().getColor(R.color.textWriteColor));
            String string = getResources().getString(R.string.ok_with_number);
            String format = String.format(string, selectedPhotos.size());
            binding.btSelectOk.setText(format);
            binding.btPreviewImage.setTextColor(getResources().getColor(R.color.textBlackColor));
        }
        Drawable drawable;
        if (PhotoSelectorSetting.IS_SELECTED_ORIGINAL_IMAGE) {
            String string = getString(R.string.original_image_with_size);
            String format = String.format(string, FileUtils.getSizeString(FileUtils.getFileLength(selectedPhotos)));
            binding.btSelectOriginalImage.setText(format);
            drawable = getResources().getDrawable(R.drawable.svg_choose_original_image_checked);
        } else {
            binding.btSelectOriginalImage.setText(getString(R.string.full_image));
            drawable = getResources().getDrawable(R.drawable.svg_choose_original_image_default);
        }
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        binding.btSelectOriginalImage.setCompoundDrawables(drawable, null, null, null);
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
        void onPhotoSelected(List<ImagePathBean> photoList, boolean isSelectOrigin);
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

    private boolean togglePhotoSelected(ImagePathBean imagePathBean) {
        if (selectedPhotos.contains(imagePathBean)) {
            selectedPhotos.remove(imagePathBean);
            return true;
        } else {
            if (selectedPhotos.size() == PhotoSelectorSetting.MAX_PHOTO_SUM) {
                return false;
            } else {
                selectedPhotos.add(imagePathBean);
                return true;
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
        selectedPhotos.clear();
        onPhotoSelectedCallback = null; // 释放引用
    }
}
