package com.fire.photoselectortest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fire.photoselector.activity.PhotoSelectorActivity;
import com.fire.photoselector.models.PhotoSelectorSetting;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_SELECT_PHOTO = 100;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION_CODE = 1000;
    private ArrayList<String> result = new ArrayList<>();
    private Button btSelectPhoto;
    private RecyclerView rvList;
    private PhotoRecyclerViewAdapter photoRecyclerViewAdapter;
    private TextView tvSelectSum;
    private TextView tvColumnCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        btSelectPhoto = (Button) findViewById(R.id.bt_select_photo);
        rvList = (RecyclerView) findViewById(R.id.rv_list);
        tvSelectSum = (TextView) findViewById(R.id.tv_select_sum);
        tvColumnCount = (TextView) findViewById(R.id.tv_column_count);
        rvList.setLayoutManager(new GridLayoutManager(this, 3));
        photoRecyclerViewAdapter = new PhotoRecyclerViewAdapter(this, result, false);
        rvList.setAdapter(photoRecyclerViewAdapter);
        btSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
                } else {
                    if (!TextUtils.isEmpty(tvSelectSum.getText().toString().trim()) && !TextUtils.isEmpty(tvColumnCount.getText().toString().trim())) {
                        selectPhotos(Integer.parseInt(tvSelectSum.getText().toString().trim()), Integer.parseInt(tvColumnCount.getText().toString().trim()));
                    }
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    result = data.getStringArrayListExtra(PhotoSelectorSetting.LAST_MODIFIED_LIST);
                    boolean isSelectedFullImage = data.getBooleanExtra(PhotoSelectorSetting.SELECTED_ORIGINAL_IMAGE, false);
                    photoRecyclerViewAdapter.setList(result, isSelectedFullImage);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "未获取权限", Toast.LENGTH_SHORT).show();
                } else {
                    if (!TextUtils.isEmpty(tvSelectSum.getText().toString().trim()) && !TextUtils.isEmpty(tvColumnCount.getText().toString().trim())) {
                        selectPhotos(Integer.parseInt(tvSelectSum.getText().toString().trim()), Integer.parseInt(tvColumnCount.getText().toString().trim()));
                    }
                }
                break;
        }
    }

    private void selectPhotos(int sum, int columnCount) {
        PhotoSelectorSetting.MAX_PHOTO_SUM = sum;
        PhotoSelectorSetting.COLUMN_COUNT = columnCount;
        PhotoSelectorSetting.IS_SHOW_SELECTED_ORIGINAL_IMAGE = true;
        PhotoSelectorSetting.SELECTED_PHOTOS = result;
        PhotoSelectorActivity.startMe(this, REQUEST_SELECT_PHOTO);
//        startActivityForResult(new Intent(MainActivity.this, PhotoSelectorActivity.class), REQUEST_SELECT_PHOTO);
    }
}
