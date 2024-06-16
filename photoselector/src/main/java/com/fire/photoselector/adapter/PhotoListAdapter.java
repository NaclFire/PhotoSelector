package com.fire.photoselector.adapter;

import static com.fire.photoselector.models.PhotoMessage.SELECTED_PHOTOS;

import android.app.Activity;
import android.content.Context;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.fire.photoselector.R;
import com.fire.photoselector.models.PhotoSelectorSetting;
import com.fire.photoselector.view.SquareImageView;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Created by Fire on 2017/4/8.
 */

public class PhotoListAdapter extends RecyclerView.Adapter<PhotoListAdapter.ViewHolder> {
    private static final String TAG = "PhotoListAdapter";
    private final RequestOptions requestOptions;
    private List<String> list;
    private Context context;
    private OnRecyclerViewItemClickListener listener;

    public PhotoListAdapter(Context context, List<String> list) {
        this.context = context;
        this.list = list;
        requestOptions = new RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .override(PhotoSelectorSetting.ITEM_SIZE, PhotoSelectorSetting.ITEM_SIZE);
    }

    public interface OnRecyclerViewItemClickListener {
        void onRecyclerViewItemClick(View v, int position);
    }

    public void setOnRecyclerViewItemClickListener(OnRecyclerViewItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<String> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public void updatePhotoList(RecyclerView recyclerView, List<String> list) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            MyDiffCallback diffCallback = new MyDiffCallback(this.list, list);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
            ((Activity) context).runOnUiThread(() -> {
                this.list = list;
                diffResult.dispatchUpdatesTo(this);
                int firstVisibleItemPosition;
                if (PhotoSelectorSetting.COLUMN_COUNT == 1) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    assert layoutManager != null;
                    firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                } else {
                    GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                    assert layoutManager != null;
                    firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                }
                if (firstVisibleItemPosition == 0) {
                    recyclerView.scrollToPosition(0);
                }
            });
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (list != null) {
            Glide.with(context)
                    .asDrawable()
                    .load(list.get(position))
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.ivPhotoThumb);
            if (list.get(position).toLowerCase().endsWith("gif")) {
                holder.ivGifImage.setVisibility(View.VISIBLE);
            } else {
                holder.ivGifImage.setVisibility(View.GONE);
            }
            if (SELECTED_PHOTOS.contains(list.get(position))) {
                holder.ivPhotoChecked.setImageResource(R.drawable.svg_compose_photo_preview_checked);
            } else {
                holder.ivPhotoChecked.setImageResource(R.drawable.svg_compose_photo_preview_default);
            }
        }
    }


    @Override
    public int getItemCount() {
        return list.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private SquareImageView ivPhotoThumb;
        private ImageView ivPhotoChecked;
        private ImageView ivGifImage;

        ViewHolder(View view) {
            super(view);
            ivPhotoThumb = view.findViewById(R.id.iv_photo_thumb);
            ivPhotoChecked = view.findViewById(R.id.iv_photo_checked);
            ivGifImage = view.findViewById(R.id.iv_gif_image);
            ivPhotoChecked.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (listener != null && position != RecyclerView.NO_POSITION) {
                listener.onRecyclerViewItemClick(v, position);
            }
        }
    }

    private static class MyDiffCallback extends DiffUtil.Callback {
        private List<String> oldPhoto;
        private List<String> newPhoto;

        public MyDiffCallback(List<String> oldPhoto, List<String> newPhoto) {
            this.oldPhoto = oldPhoto;
            this.newPhoto = newPhoto;
        }

        @Override
        public int getOldListSize() {
            return oldPhoto.size();
        }

        @Override
        public int getNewListSize() {
            return newPhoto.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldPhoto.get(oldItemPosition).equals(newPhoto.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldPhoto.get(oldItemPosition).equals(newPhoto.get(newItemPosition));
        }
    }

}
