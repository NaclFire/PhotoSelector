package com.fire.photoselector.activity;

import static com.fire.photoselector.models.PhotoMessage.PHOTOS_LIST_TRANSFER;
import static com.fire.photoselector.models.PhotoMessage.SELECTED_PHOTOS;
import static com.fire.photoselector.models.PhotoSelectorSetting.COLUMN_COUNT;
import static com.fire.photoselector.models.PhotoSelectorSetting.IS_SELECTED_FULL_IMAGE;
import static com.fire.photoselector.models.PhotoSelectorSetting.LAST_MODIFIED_LIST;
import static com.fire.photoselector.models.PhotoSelectorSetting.MAX_PHOTO_SUM;
import static com.fire.photoselector.models.PhotoSelectorSetting.SELECTED_FULL_IMAGE;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.fire.photoselector.R;
import com.fire.photoselector.adapter.FolderListAdapter;
import com.fire.photoselector.adapter.PhotoListAdapter;
import com.fire.photoselector.bean.ImageFolderBean;
import com.fire.photoselector.databinding.ActivityPhotoSelectorBinding;
import com.fire.photoselector.models.PhotoMessage;
import com.fire.photoselector.models.PhotoSelectorSetting;
import com.fire.photoselector.utils.FileUtils;
import com.fire.photoselector.utils.ScreenUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Fire on 2017/4/8.
 */

public class PhotoSelectorActivity extends AppCompatActivity implements OnClickListener {
    private static final String TAG = "PhotoSelectorActivity";
    private static final int REQUEST_PREVIEW_PHOTO = 100;
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
    private List<String> chileList;
    private List<String> value;
    private List<String> photoFolder;
    private ActivityPhotoSelectorBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotoSelectorBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(R.color.textWriteColor));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        binding.tvSelectCancel.setOnClickListener(this);
        binding.tvAlbumName.setOnClickListener(this);
        binding.ivAlbumArrow.setOnClickListener(this);
        binding.btSelectOk.setOnClickListener(this);
        binding.btPreviewImage.setOnClickListener(this);
        binding.btSelectOriginalImage.setOnClickListener(this);
        binding.vAlpha.setOnClickListener(this);
        Intent intent = getIntent();
        SELECTED_PHOTOS = intent.getStringArrayListExtra(LAST_MODIFIED_LIST);
        if (SELECTED_PHOTOS == null || SELECTED_PHOTOS.size() == 0) {
            IS_SELECTED_FULL_IMAGE = false;
        }
        photoFolder = new ArrayList<>();
        photoGroupMap.put(getString(R.string.all_photos), photoFolder);
        photoListAdapter = new PhotoListAdapter(this, photoGroupMap.get(getString(R.string.all_photos)));
        if (COLUMN_COUNT <= 1) {
            binding.rvPhotoList.setLayoutManager(new LinearLayoutManager(this));
        } else {
            binding.rvPhotoList.setLayoutManager(new GridLayoutManager(this, COLUMN_COUNT));
        }
        binding.rvPhotoList.setAdapter(photoListAdapter);
        photoListAdapter.setOnRecyclerViewItemClickListener(new OnPhotoListClick());
        binding.rvFolderList.setLayoutManager(new LinearLayoutManager(this));
        ViewGroup.LayoutParams lp = binding.rvFolderList.getLayoutParams();
        lp.height = (int) (ScreenUtil.getScreenHeight(this) * 0.618);
        binding.rvFolderList.setLayoutParams(lp);
        folderListAdapter = new FolderListAdapter(this, photoFolders);
        folderListAdapter.setOnRecyclerViewItemClickListener(new OnFolderListClick());
        binding.rvFolderList.setAdapter(folderListAdapter);
        PhotoSelectorSetting.STATUS_BAR_HEIGHT = ScreenUtil.getStatusBarHeight(this);
        getImages();
        changeOKButtonStatus();
    }

    /**
     * 扫描手机中所有图片
     */
    private void getImages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(imageUri, null, null, null, sortOrder);
                if (cursor == null) {
                    return;
                }

                while (cursor.moveToNext()) {
                    //获取图片的路径
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    List<String> allPhotos = photoGroupMap.get(getString(R.string.all_photos));
                    if (allPhotos != null) {
                        allPhotos.add(path);
                    }
                    //获取该图片的父路径名
                    File file = new File(path).getParentFile();
                    if (file != null) {
                        String parentName = file.getName();
                        //根据父路径名将图片放入到mGroupMap中
                        if (photoGroupMap.containsKey(parentName)) {
                            List<String> key = photoGroupMap.get(parentName);
                            if (key == null) {
                                chileList = new ArrayList<>();
                                chileList.add(path);
                                photoGroupMap.put(parentName, chileList);
                            } else {
                                key.add(path);
                            }
                        } else {
                            chileList = new ArrayList<>();
                            chileList.add(path);
                            photoGroupMap.put(parentName, chileList);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (photoListAdapter != null) {
                                    List<String> list = photoGroupMap.get(getString(R.string.all_photos));
                                    if (list != null) {
                                        photoListAdapter.notifyItemChanged(list.size());
                                    }
                                }
                                if (folderListAdapter != null) {
                                    folderListAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
                //扫描图片完成
                cursor.close();
                photoFolders.addAll(subGroupOfImage(photoGroupMap));
            }
        }).start();
    }

    private List<ImageFolderBean> subGroupOfImage(ConcurrentHashMap<String, List<String>> mGroupMap) {
        List<ImageFolderBean> list = new ArrayList<>();
        ImageFolderBean imageFolderBean;
        for (Map.Entry<String, List<String>> entry : mGroupMap.entrySet()) {
            imageFolderBean = new ImageFolderBean();
            String key = entry.getKey();
            if (key.equals(getString(R.string.all_photos))) {
                imageFolderBean.setSelected(true);
            } else {
                imageFolderBean.setSelected(false);
            }
            value = entry.getValue();
            imageFolderBean.setFolderName(key);
            imageFolderBean.setImageCounts(value.size());
            imageFolderBean.setImagePaths(value);
            if (key.equals(getString(R.string.all_photos))) {
                list.add(0, imageFolderBean);
            } else {
                list.add(imageFolderBean);
            }
        }
        return list;
    }

    @Override
    public void onClick(View v) {
        if (v == binding.tvSelectCancel) {// 取消
            setResult(RESULT_CANCELED);
            finish();
        } else if (v == binding.tvAlbumName) {// 选择相册
            toggleFolderList();
        } else if (v == binding.btSelectOk) {// 确定按钮
            if (SELECTED_PHOTOS.size() != 0) {
                ArrayList<String> image = new ArrayList<>();
                image.addAll(SELECTED_PHOTOS);
                Intent intent = new Intent();
                intent.putExtra(LAST_MODIFIED_LIST, image);
                intent.putExtra(SELECTED_FULL_IMAGE, PhotoSelectorSetting.IS_SELECTED_FULL_IMAGE);
                setResult(RESULT_OK, intent);
                finish();
            }
        } else if (v == binding.btPreviewImage) {// 预览照片
            if (SELECTED_PHOTOS.size() != 0) {
                Intent intent = new Intent(this, PhotoViewActivity.class);
                PHOTOS_LIST_TRANSFER.clear();
                PHOTOS_LIST_TRANSFER.addAll(SELECTED_PHOTOS);
                startActivityForResult(intent, REQUEST_PREVIEW_PHOTO);
            }
        } else if (v == binding.btSelectOriginalImage) {// 选择原图
            PhotoSelectorSetting.IS_SELECTED_FULL_IMAGE = !PhotoSelectorSetting.IS_SELECTED_FULL_IMAGE;
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
            Log.e(TAG, "onWindowFocusChanged: PhotoSelectorSetting.SCREEN_RATIO = " + PhotoSelectorSetting.SCREEN_RATIO);
        }
    }

    private class OnFolderListClick implements FolderListAdapter.OnRecyclerViewItemClickListener {

        @Override
        public void onRecyclerViewItemClick(View v, int position) {
            toggleFolderSelected(position);
            photoFolder = photoGroupMap.get(photoFolders.get(position).getFolderName());
            photoListAdapter.setData(photoFolder);
            folderListAdapter.notifyDataSetChanged();
            binding.tvAlbumName.setText(photoFolders.get(position).getFolderName());
            toggleFolderList();
            binding.rvPhotoList.smoothScrollToPosition(0);
        }
    }

    private class OnPhotoListClick implements PhotoListAdapter.OnRecyclerViewItemClickListener {

        @Override
        public void onRecyclerViewItemClick(View v, int position) {
            if (v.getId() == R.id.iv_photo_checked) {
                boolean photoSelected = PhotoMessage.togglePhotoSelected(photoFolder.get(position));
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
                PHOTOS_LIST_TRANSFER.addAll(photoFolder);
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
                    Intent intent = new Intent();
                    intent.putExtra(LAST_MODIFIED_LIST, PHOTOS_LIST_TRANSFER);
                    setResult(RESULT_OK, intent);
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
            binding.rvFolderList.setVisibility(View.GONE);
            binding.vAlpha.setVisibility(View.INVISIBLE);
            binding.ivAlbumArrow.setImageResource(R.drawable.ic_arrow_down_yellow);
            animation = AnimationUtils.loadAnimation(this, R.anim.top_popup_hidden_anim);
        } else {
            binding.rvFolderList.setVisibility(View.VISIBLE);
            binding.vAlpha.setVisibility(View.VISIBLE);
            binding.ivAlbumArrow.setImageResource(R.drawable.ic_arrow_up_yellow);
            animation = AnimationUtils.loadAnimation(this, R.anim.top_popup_show_anim);
        }
        binding.rvFolderList.setAnimation(animation);
        folderListAdapter.notifyDataSetChanged();
    }

    private void toggleFolderSelected(int position) {
        for (ImageFolderBean photoFolder : photoFolders) {
            photoFolder.setSelected(false);
        }
        photoFolders.get(position).setSelected(true);
    }

    private void changeOKButtonStatus() {
        if (SELECTED_PHOTOS.size() == 0) {
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
        if (PhotoSelectorSetting.IS_SELECTED_FULL_IMAGE) {
            String string = getString(R.string.full_image_with_size);
            String format = String.format(string, FileUtils.getSizeString(FileUtils.getFileLength(SELECTED_PHOTOS)));
            binding.btSelectOriginalImage.setText(format);
            Drawable drawable = getResources().getDrawable(R.drawable.svg_choose_original_image_checked);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            binding.btSelectOriginalImage.setCompoundDrawables(drawable, null, null, null);
        } else {
            binding.btSelectOriginalImage.setText(getString(R.string.full_image));
            Drawable drawable = getResources().getDrawable(R.drawable.svg_choose_original_image_unchecked);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            binding.btSelectOriginalImage.setCompoundDrawables(drawable, null, null, null);
        }
    }
}
