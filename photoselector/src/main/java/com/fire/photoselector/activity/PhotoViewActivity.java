package com.fire.photoselector.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.fire.photoselector.R;
import com.fire.photoselector.adapter.PhotoViewAdapter;
import com.fire.photoselector.bean.ImagePathBean;
import com.fire.photoselector.databinding.ActivityPhotoViewBinding;
import com.fire.photoselector.PhotoSelectorSetting;
import com.fire.photoselector.utils.FileUtils;
import com.fire.photoselector.utils.ScreenUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fire on 2017/4/11.
 */

public class PhotoViewActivity extends AppCompatActivity implements OnClickListener {

    private static final String TAG = "PhotoViewActivity";
    private static final int REQUEST_PREVIEW_PHOTO = 100;
    private PhotoViewAdapter photoViewAdapter;
    private ActivityPhotoViewBinding binding;
    private boolean isShowPreview;
    //    private List<ImagePathBean> currentList = new ArrayList<>();
    private List<ImagePathBean> selectedPhotos = new ArrayList<>();
    private List<ImagePathBean> currentPhotoFolder = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotoViewBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_no);
        fullScreen();
        binding.ivSelectCancel.setOnClickListener(this);
        binding.btSelectOk.setOnClickListener(this);
        binding.ivPhotoSelected.setOnClickListener(this);
        binding.btPreviewImage.setOnClickListener(this);
        binding.btSelectOriginalImage.setOnClickListener(this);
        binding.btSelectOriginalImage.setVisibility(PhotoSelectorSetting.IS_SHOW_SELECTED_ORIGINAL_IMAGE ? View.VISIBLE : View.GONE);
        Intent intent = getIntent();
        int index = intent.getIntExtra("Index", 0);
        selectedPhotos.addAll((List<ImagePathBean>) intent.getSerializableExtra("selectedPhotos"));
        isShowPreview = intent.getBooleanExtra("isShowPreview", true);
        if (isShowPreview) {
            currentPhotoFolder.addAll((List<ImagePathBean>) intent.getSerializableExtra("currentPhotos"));
        } else {
            currentPhotoFolder.addAll(selectedPhotos);
        }
        photoViewAdapter = new PhotoViewAdapter(this, currentPhotoFolder);
        photoViewAdapter.setOnPhotoViewClickListener((view, position) -> {
            toggleTitleBar();
        });
        binding.vpPhotoView.setAdapter(photoViewAdapter);
        binding.vpPhotoView.setCurrentItem(index, false);
        binding.vpPhotoView.addOnPageChangeListener(new PageChangeListener());
        changePhotoSelectStatus(binding.vpPhotoView.getCurrentItem());
        changeOKButtonStatus();
    }

    @Override
    public void onClick(View v) {
        if (v == binding.ivSelectCancel) {
            finish();
        } else if (v == binding.btSelectOk) {
            if (selectedPhotos.size() != 0) {
//                ArrayList<ImagePathBean> image = new ArrayList<>(SELECTED_PHOTOS);
                Intent intent = new Intent();
                intent.putExtra("selectedPhotos", (Serializable) selectedPhotos);
                setResult(RESULT_OK, intent);
                finish();
            }
        } else if (v == binding.ivPhotoSelected) {
            // 当前ViewPager页面脚标
            int position = binding.vpPhotoView.getCurrentItem();
            // 添加/删除当前页面照片
            boolean result = togglePhotoSelected(currentPhotoFolder.get(position));
            if (result) {
                // 添加/删除成功
                changePhotoSelectStatus(position);
            } else {
                // 添加失败,超出可选照片上限
                binding.ivPhotoSelected.setImageResource(R.drawable.svg_compose_photo_preview_default);
                String string = getString(R.string.photo_sum_max);
                String format = String.format(string, PhotoSelectorSetting.MAX_PHOTO_SUM);
                Toast.makeText(this, format, Toast.LENGTH_SHORT).show();
            }
            // 更改确定按钮文字
            changeOKButtonStatus();
        } else if (v == binding.btPreviewImage) {// 预览照片
            if (selectedPhotos.size() != 0) {
                Intent intent = new Intent(this, PhotoViewActivity.class);
                intent.putExtra("isShowPreview", false);
                intent.putExtra("selectedPhotos", (Serializable) selectedPhotos);
                intent.putExtra("currentPhotos", (Serializable) currentPhotoFolder);
                startActivityForResult(intent, REQUEST_PREVIEW_PHOTO);
            }
        } else if (v == binding.btSelectOriginalImage) {// 选择原图
            PhotoSelectorSetting.IS_SELECTED_ORIGINAL_IMAGE = !PhotoSelectorSetting.IS_SELECTED_ORIGINAL_IMAGE;
            changeOKButtonStatus();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PREVIEW_PHOTO:
                if (resultCode == RESULT_OK) {
//                    PHOTOS_LIST_TRANSFER.clear();
//                    PHOTOS_LIST_TRANSFER.addAll(SELECTED_PHOTOS);
                    Intent intent = new Intent();
                    intent.putExtra("selectedPhotos", (Serializable) selectedPhotos);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    changePhotoSelectStatus(binding.vpPhotoView.getCurrentItem());
                    changeOKButtonStatus();
                }
                break;
        }
    }

    /**
     * ViewPager滑动监听
     */
    private class PageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            // 判断当前页面照片是否在已选集合中
            changePhotoSelectStatus(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    /**
     * 更改勾选按钮状态
     *
     * @param position
     */
    private void changePhotoSelectStatus(int position) {
        if (selectedPhotos.contains(currentPhotoFolder.get(position))) {
            binding.ivPhotoSelected.setImageResource(R.drawable.svg_compose_photo_preview_checked);
        } else {
            binding.ivPhotoSelected.setImageResource(R.drawable.svg_compose_photo_preview_default);
        }
    }

    /**
     * 更改确定按钮状态
     */
    private void changeOKButtonStatus() {
        binding.btPreviewImage.setVisibility(isShowPreview ? View.VISIBLE : View.GONE);
        if (selectedPhotos.size() == 0) {
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
        if (PhotoSelectorSetting.IS_SELECTED_ORIGINAL_IMAGE) {
            String string = getString(R.string.original_image_with_size);
            String format = String.format(string, FileUtils.getSizeString(FileUtils.getFileLength(selectedPhotos)));
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

    private void toggleTitleBar() {
        int visibility = binding.rlPhotoViewButton.getVisibility();
        Animation animationTop = AnimationUtils.loadAnimation(this, visibility == View.VISIBLE ? R.anim.top_popup_hidden_anim : R.anim.top_popup_show_anim);
        Animation animationBottom = AnimationUtils.loadAnimation(this, visibility == View.VISIBLE ? R.anim.bottom_popup_hidden_anim : R.anim.bottom_popup_show_anim);
        binding.rlPhotoViewButton.setAnimation(animationTop);
        binding.llPhotoViewButton.setAnimation(animationBottom);
        binding.rlPhotoViewButton.setVisibility(visibility == View.VISIBLE ? View.GONE : View.VISIBLE);
        binding.llPhotoViewButton.setVisibility(visibility == View.VISIBLE ? View.GONE : View.VISIBLE);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (visibility == View.VISIBLE) {//设置为全屏
            PhotoSelectorSetting.SCREEN_RATIO = (float) binding.vpPhotoView.getWidth() / binding.vpPhotoView.getHeight();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(lp);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {//设置为非全屏
            PhotoSelectorSetting.SCREEN_RATIO = (float) binding.vpPhotoView.getWidth() / binding.vpPhotoView.getHeight();
            lp.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(lp);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    /**
     * 通过设置全屏，设置状态栏透明
     */
    private void fullScreen() {
        // 5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
        Window window = getWindow();
        View decorView = window.getDecorView();
        // 两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) binding.rlPhotoViewButton.getLayoutParams();
        layoutParams.height = ScreenUtil.dp2px(this, 45) + PhotoSelectorSetting.STATUS_BAR_HEIGHT;
    }

    public boolean togglePhotoSelected(ImagePathBean imagePathBean) {
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
        super.finish();
        overridePendingTransition(R.anim.slide_no, R.anim.slide_out_bottom);
    }
}
