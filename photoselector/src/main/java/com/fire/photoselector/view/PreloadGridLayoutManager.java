package com.fire.photoselector.view;

import android.content.Context;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fire.photoselector.PhotoSelectorSetting;

public class PreloadGridLayoutManager extends GridLayoutManager {

    private static final int DEFAULT_EXTRA_LAYOUT_SPACE = PhotoSelectorSetting.ITEM_SIZE * 3; // 默认的额外空间

    public PreloadGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public PreloadGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
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

        int direction = Integer.compare(dy, 0); // 判断滚动方向

        // 预取下一个和前一个可见项的位置
        if (direction >= 1) { // 用户向上滚动
            for (int i = lastVisiblePosition + 1; i <= lastVisiblePosition + 30 && i < state.getItemCount(); i++) {
                layoutPrefetchRegistry.addPosition(i, 0);
            }
        } else if (direction <= -1) { // 用户向下滚动
            for (int i = firstVisiblePosition - 1; i >= firstVisiblePosition - 30 && i >= 0; i--) {
                layoutPrefetchRegistry.addPosition(i, 0);
            }
        }
    }
}