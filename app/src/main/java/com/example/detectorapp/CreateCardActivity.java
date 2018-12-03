package com.example.detectorapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

/**
 * In this activity, values for card label and barcode are being extracted and displayed on the screen. Also based on the card label,
 * a suitable visual representation of the card is being loaded in the top of the screen.
 **/
public class CreateCardActivity extends AppCompatActivity{
    private static final String TAG = "Results Activity";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.results_view);

        Toolbar thistoolbar = (Toolbar) findViewById(R.id.cardtoolbar);
        setSupportActionBar(thistoolbar);

        Intent myintent = getIntent();
        String code = myintent.getStringExtra("code");
        String title = myintent.getStringExtra("label");

        TextView txtBarcode = findViewById(R.id.barcodeTxt);
        txtBarcode.setText(code);

        TextView txtTitle = findViewById(R.id.titleTxt);
        txtTitle.setText(title);
    }
}
