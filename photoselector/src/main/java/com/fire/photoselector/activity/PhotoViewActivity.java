package com.fire.photoselector.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fire.photoselector.R;
import com.fire.photoselector.adapter.PhotoViewAdapter;
import com.fire.photoselector.databinding.ActivityPhotoViewBinding;
import com.fire.photoselector.models.PhotoMessage;

import java.util.ArrayList;

import static com.fire.photoselector.models.PhotoMessage.PHOTOS_LIST_TRANSFER;
import static com.fire.photoselector.models.PhotoMessage.SELECTED_PHOTOS;
import static com.fire.photoselector.models.PhotoSelectorSetting.LAST_MODIFIED_LIST;
import static com.fire.photoselector.models.PhotoSelectorSetting.MAX_PHOTO_SUM;

/**
 * Created by Fire on 2017/4/11.
 */

public class PhotoViewActivity extends AppCompatActivity implements OnClickListener {

    private static final String TAG = "PhotoViewActivity";
    private PhotoViewAdapter photoViewAdapter;
    private com.fire.photoselector.databinding.ActivityPhotoViewBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotoViewBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        binding.ivSelectCancel.setOnClickListener(this);
        binding.btSelectOk.setOnClickListener(this);
        binding.ivPhotoSelected.setOnClickListener(this);
        Intent intent = getIntent();
        int index = intent.getIntExtra("Index", 0);
        photoViewAdapter = new PhotoViewAdapter(this, PHOTOS_LIST_TRANSFER);
        binding.vpPhotoView.setAdapter(photoViewAdapter);
        binding.vpPhotoView.setCurrentItem(index, false);
        binding.vpPhotoView.addOnPageChangeListener(new PageChangeListener());
        changePhotoSelectStatus(binding.vpPhotoView.getCurrentItem());
        changeOKButtonStatus();
        changePhotoIndicator(index + 1);
    }

    @Override
    public void onClick(View v) {
        if (v == binding.ivSelectCancel) {
            finish();
        } else if (v == binding.btSelectOk) {
            if (SELECTED_PHOTOS.size() != 0) {
                ArrayList<String> image = new ArrayList<>();
                image.addAll(SELECTED_PHOTOS);
                Intent intent = new Intent();
                intent.putExtra(LAST_MODIFIED_LIST, image);
                setResult(RESULT_OK, intent);
                finish();
            }
        } else if (v == binding.ivPhotoSelected) {
            // 当前ViewPager页面脚标
            int position = binding.vpPhotoView.getCurrentItem();
            // 添加/删除当前页面照片
            boolean result = PhotoMessage.togglePhotoSelected(PHOTOS_LIST_TRANSFER.get(position));
            if (result) {
                // 添加/删除成功
                changePhotoSelectStatus(position);
            } else {
                // 添加失败,超出可选照片上限
                binding.ivPhotoSelected.setImageResource(R.drawable.compose_photo_preview_default);
                String string = getString(R.string.photo_sum_max);
                String format = String.format(string, MAX_PHOTO_SUM);
                Toast.makeText(this, format, Toast.LENGTH_SHORT).show();
            }
            // 更改确定按钮文字
            changeOKButtonStatus();
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
            // 变更指示器状态
            changePhotoIndicator(position + 1);
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
        if (PhotoMessage.isPhotoSelected(PHOTOS_LIST_TRANSFER.get(position))) {
            binding.ivPhotoSelected.setImageResource(R.drawable.compose_photo_preview_right);
        } else {
            binding.ivPhotoSelected.setImageResource(R.drawable.compose_photo_preview_default);
        }
    }

    /**
     * 更改确定按钮状态
     */
    private void changeOKButtonStatus() {
        if (SELECTED_PHOTOS.size() == 0) {
            binding.btSelectOk.setBackgroundResource(R.drawable.button_unclickable);
            binding.btSelectOk.setTextColor(getResources().getColor(R.color.textSecondColor));
            binding.btSelectOk.setText(getString(R.string.ok));
        } else {
            binding.btSelectOk.setBackgroundResource(R.drawable.button_clickable);
            binding.btSelectOk.setTextColor(getResources().getColor(R.color.textWriteColor));
            String string = getResources().getString(R.string.ok_with_number);
            String format = String.format(string, SELECTED_PHOTOS.size());
            binding.btSelectOk.setText(format);
        }
    }

    /**
     * 更改相册指示器
     *
     * @param position
     */
    private void changePhotoIndicator(int position) {
        String string = getString(R.string.photo_sum_indicator);
        String format = String.format(string, position, PHOTOS_LIST_TRANSFER.size());
        binding.tvPhotoIndicator.setText(format);
    }
}
