package com.example.detectorapp.RecyclerView;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.example.detectorapp.R;

public class MyViewHolder extends RecyclerView.ViewHolder {
    private ImageView imageView = null;

    public MyViewHolder(View itemView) {
        super(itemView);
        if(itemView != null)
        {
            imageView = (ImageView)itemView.findViewById(R.id.recycler_view_item_image);
        }
    }

    public ImageView getImageView() {
        return imageView;
    }
}
