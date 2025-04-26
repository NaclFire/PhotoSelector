package com.fire.photoselectortest;

import android.Manifest;
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
import com.fire.photoselector.bean.ImagePathBean;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION_CODE = 1000;
    private ArrayList<ImagePathBean> result = new ArrayList<>();
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
                if (!TextUtils.isEmpty(tvSelectSum.getText().toString().trim()) && !TextUtils.isEmpty(tvColumnCount.getText().toString().trim())) {
                    selectPhotos(Integer.parseInt(tvSelectSum.getText().toString().trim()), Integer.parseInt(tvColumnCount.getText().toString().trim()));
                }
            }
        });

    }

    private void selectPhotos(int sum, int columnCount) {
        new PhotoSelectorActivity.Builder()
                .setSelectedPhotos(result)
                .setMaxPhotoSum(sum)
                .setColumnCount(columnCount)
                .setShowSelectOrigin(true)
                .setOnPhotoSelectedCallback(new PhotoSelectorActivity.OnPhotoSelectedCallback() {
                    @Override
                    public void onPhotoSelected(List<ImagePathBean> photoList, boolean isSelectOrigin) {
                        result = (ArrayList<ImagePathBean>) photoList;
                        photoRecyclerViewAdapter.setList(result, isSelectOrigin);
                    }
                })
                .build(this);
    }
}
