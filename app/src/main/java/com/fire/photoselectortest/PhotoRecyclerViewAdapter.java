package com.fire.photoselectortest;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fire.photoselector.bean.ImagePathBean;

import java.util.List;

/**
 * Created by Fire on 2017/4/13.
 */

public class PhotoRecyclerViewAdapter extends RecyclerView.Adapter<PhotoRecyclerViewAdapter.ViewHolder> {
    private Context context;
    private List<ImagePathBean> list;
    private boolean isFullImage;

    public PhotoRecyclerViewAdapter(Context context, List<ImagePathBean> list, boolean isFullImage) {
        this.context = context;
        this.list = list;
        this.isFullImage = isFullImage;
    }

    public void setList(List<ImagePathBean> list, boolean isFullImage) {
        this.list = list;
        this.isFullImage = isFullImage;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recycler_view_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Glide.with(context).load(list.get(position).getUri()).into(holder.photo);
        holder.fullImage.setVisibility(isFullImage ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        SquareImageView photo;
        ImageView fullImage;

        ViewHolder(View view) {
            super(view);
            photo = (SquareImageView) view.findViewById(R.id.photo);
            fullImage = (ImageView) view.findViewById(R.id.full_image);
        }
    }
}
