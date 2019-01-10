package com.example.detectorapp.recyclerview;

import android.app.Dialog;
import android.content.Context;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.detectorapp.R;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private Context context;
    private List<CardItem> viewItemList;

    public MyAdapter(Context c, List<CardItem> viewItemList) {
        this.context = c;
        this.viewItemList = viewItemList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final MyViewHolder vHolder = new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.recycler_view_item, parent, false));
        return vHolder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final CardItem viewItem = viewItemList.get(position);
        holder.label.setText(viewItem.getTitle());
        holder.number.setText(viewItem.getCodenumber());
        holder.format.setText(String.valueOf(viewItem.getFormat()));

        // loading card image using Glide library
        Glide.with(context).load(viewItem.getImageUrl()).into(holder.card);
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
