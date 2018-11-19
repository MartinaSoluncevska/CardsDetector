package com.example.detectorapp.RecyclerView;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.detectorapp.R;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private List<CardItem> viewItemList;

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(List<CardItem> viewItemList) {
        this.viewItemList = viewItemList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        // Inflate the RecyclerView item layout xml.
        final View itemView = layoutInflater.inflate(R.layout.recycler_view_item, parent, false);
        final ImageView imageView = (ImageView)itemView.findViewById(R.id.recycler_view_item_image);

        MyViewHolder ret = new MyViewHolder(itemView);
        return ret;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        if(viewItemList!=null) {
            // Get item dto in list.
            CardItem viewItem = viewItemList.get(position);
            if(viewItem != null) {
                // Set car image resource id.
                holder.getImageView().setImageResource(viewItem.getImageId());
            }
        }
    }

    @Override
    public int getItemCount() {
        int ret = 0;
        if(viewItemList!=null)
        {
            ret = viewItemList.size();
        }
        return ret;
    }
}
