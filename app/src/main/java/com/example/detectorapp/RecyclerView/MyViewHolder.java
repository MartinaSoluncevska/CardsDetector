package com.example.detectorapp.recyclerview;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.detectorapp.R;

import org.w3c.dom.Text;

public class MyViewHolder extends RecyclerView.ViewHolder {
    ImageView card;
    TextView label;
    TextView number;
    TextView format;

    public MyViewHolder(View itemView) {
        super(itemView);
        label = (TextView) itemView.findViewById(R.id.txtlabel);
        number = (TextView) itemView.findViewById(R.id.txtnumber);
        format = (TextView) itemView.findViewById(R.id.txtformat);
        card = (ImageView) itemView.findViewById(R.id.imageview);
    }
}
