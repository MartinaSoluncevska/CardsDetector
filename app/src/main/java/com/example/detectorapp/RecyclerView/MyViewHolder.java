package com.example.detectorapp.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.detectorapp.R;

import org.w3c.dom.Text;

public class MyViewHolder extends RecyclerView.ViewHolder {
    private ImageView imageView = null;
    private TextView title = null;
    private TextView barcode = null;

    public MyViewHolder(View itemView) {
        super(itemView);
        if(itemView != null)
        {
            imageView = (ImageView)itemView.findViewById(R.id.recycler_view_item_image);
            title = (TextView) itemView.findViewById(R.id.titleTxt);
            barcode = (TextView) itemView.findViewById(R.id.barcodeTxt);
        }
    }

    public ImageView getImageView() {
        return imageView;
    }
}
