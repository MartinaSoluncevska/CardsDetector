package com.example.detectorapp;

import android.os.Bundle;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.example.detectorapp.RecyclerView.CardItem;
import com.example.detectorapp.RecyclerView.MyAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<CardItem> itemList = new ArrayList<CardItem>();

        //Creating RecyclerView to display card data, that are being added when clicked fab button
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        //Display items in the RecyclerView using LinearLayout
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        //Populate RecyclerView's adapter with items from the list created above
        MyAdapter mAdapter = new MyAdapter(itemList);
        mRecyclerView.setAdapter(mAdapter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.appname);
        setSupportActionBar(toolbar);

        //Start DetectingActivity onclick, which loads camera and its preview for detecting objects
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DetectingActivity.class);
                startActivity(intent);
            }
        });
    }

}