package com.fire.photoselector.view;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fire.photoselector.models.PhotoSelectorSetting;

public class PreloadLinearLayoutManager extends LinearLayoutManager {

    private static final int DEFAULT_EXTRA_LAYOUT_SPACE = PhotoSelectorSetting.ITEM_SIZE * 6; // 默认的额外空间

    public PreloadLinearLayoutManager(Context context) {
        super(context);
    }

    public PreloadLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override
    protected int getExtraLayoutSpace(RecyclerView.State state) {
        // 增加额外布局空间
        return DEFAULT_EXTRA_LAYOUT_SPACE;
    }

    @Override
    public void collectAdjacentPrefetchPositions(int dx, int dy, RecyclerView.State state, LayoutPrefetchRegistry layoutPrefetchRegistry) {
        // 获取当前可见的最后一个位置和第一个位置
        int lastVisiblePosition = findLastVisibleItemPosition();
        int firstVisiblePosition = findFirstVisibleItemPosition();

        if (dy > 0) { // 用户向上滚动
            for (int i = lastVisiblePosition + 1; i <= lastVisiblePosition + 10 && i < state.getItemCount(); i++) {
                layoutPrefetchRegistry.addPosition(i, 0);
            }
        } else if (dy < 0) { // 用户向下滚动
            for (int i = firstVisiblePosition - 1; i >= firstVisiblePosition - 10 && i >= 0; i--) {
                layoutPrefetchRegistry.addPosition(i, 0);
            }
        }
    }
}

