package com.fire.photoselectortest;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fire.photoselector.activity.PhotoSelectorActivity;
import com.fire.photoselector.bean.ImagePathBean;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ArrayList<ImagePathBean> result = new ArrayList<>();
    private Button btSelectPhoto;
    private Button btSelectOriginImage;
    private RecyclerView rvList;
    private PhotoRecyclerViewAdapter photoRecyclerViewAdapter;
    private TextView tvSelectSum;
    private TextView tvColumnCount;
    private boolean isShowSelectOrigin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        btSelectPhoto = findViewById(R.id.bt_select_photo);
        rvList = findViewById(R.id.rv_list);
        tvSelectSum = findViewById(R.id.tv_select_sum);
        tvColumnCount = findViewById(R.id.tv_column_count);
        btSelectOriginImage = findViewById(R.id.bt_select_original_image);
        btSelectOriginImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Drawable drawable;
                if (isShowSelectOrigin) {
                    drawable = getResources().getDrawable(com.fire.photoselector.R.drawable.svg_choose_original_image_default);
                } else {
                    drawable = getResources().getDrawable(com.fire.photoselector.R.drawable.svg_choose_original_image_checked);
                }
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                btSelectOriginImage.setCompoundDrawables(drawable, null, null, null);
                isShowSelectOrigin = !isShowSelectOrigin;
            }
        });
        rvList.setLayoutManager(new GridLayoutManager(this, 3));
        photoRecyclerViewAdapter = new PhotoRecyclerViewAdapter(this, result, false);
        rvList.setAdapter(photoRecyclerViewAdapter);
        btSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (PermissionHelper.hasStoragePermissions(MainActivity.this)) {
                    if (!TextUtils.isEmpty(tvSelectSum.getText().toString().trim()) && !TextUtils.isEmpty(tvColumnCount.getText().toString().trim())) {
                        selectPhotos(Integer.parseInt(tvSelectSum.getText().toString().trim()), Integer.parseInt(tvColumnCount.getText().toString().trim()));
                    }
                } else {
                    PermissionHelper.requestStoragePermissions(MainActivity.this);
                }

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_STORAGE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                // 权限已授予
                if (!TextUtils.isEmpty(tvSelectSum.getText().toString().trim()) && !TextUtils.isEmpty(tvColumnCount.getText().toString().trim())) {
                    selectPhotos(Integer.parseInt(tvSelectSum.getText().toString().trim()), Integer.parseInt(tvColumnCount.getText().toString().trim()));
                }
            } else {
                // 拒绝授权，开弹窗跳询问是否跳设置-权限管理界面
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setMessage("应用需要文件操作权限，请到设置-权限管理中授权。")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                startActivity(intent);
                            }
                        }).setCancelable(false)
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this, "您没有允许权限，此功能不能正常使用", Toast.LENGTH_SHORT).show();
                            }
                        });
                builder.create().show();
            }
        }
    }

    private void selectPhotos(int sum, int columnCount) {
        new PhotoSelectorActivity.Builder()
                .setSelectedPhotos(result)
                .setMaxPhotoSum(sum)
                .setColumnCount(columnCount)
                .setShowSelectOrigin(isShowSelectOrigin)
                .setOnPhotoSelectedCallback(new PhotoSelectorActivity.OnPhotoSelectedCallback() {
                    @Override
                    public void onPhotoSelected(List<ImagePathBean> photoList, boolean isSelectOrigin) {
                        result.clear();
                        result.addAll(photoList);
                        photoRecyclerViewAdapter.setList(result, isSelectOrigin);
                    }
                })
                .build(this);
    }
}
