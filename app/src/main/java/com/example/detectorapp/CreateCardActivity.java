package com.example.detectorapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.detectorapp.recyclerview.CardItem;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * In this activity, values for card label and barcode are being extracted and displayed on the screen. Also based on the card label,
 * a suitable visual representation of the card is being loaded in the top of the screen.
 **/
public class CreateCardActivity extends AppCompatActivity{
    private static final String TAG = "Results Activity";
    private static final String LABEL_PATH = "retrained_labels.txt";

    private ImageView imageView, imageCover;
    private Button btnSave;
    private TextView txtBarcode;
    private String cardId, title, codenumber;
    private int cardformat;

    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;

    private Bitmap bitmap;
    private BitMatrix bitMatrix;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.results_view);
        Toolbar thistoolbar = (Toolbar) findViewById(R.id.cardtoolbar);
        setSupportActionBar(thistoolbar);

        Intent myintent = getIntent();
        title = myintent.getStringExtra("label");
        codenumber = myintent.getStringExtra("code");
        cardformat = myintent.getIntExtra("format", 0);

        imageCover = findViewById(R.id.cover);
        imageView = findViewById(R.id.barcodeimg);
        txtBarcode = findViewById(R.id.codenumber);
        txtBarcode.setText(codenumber);
        btnSave = (Button) findViewById(R.id.btnSave);

        //Generate image dynamically, based on the card label that the card we are detecting with the camera has.
        //createCover(cardId);
        //Generate barcode image based on the barcode format value that the card we are detecting with the camera has.
        createBarcode(cardformat);
        imageView.setImageBitmap(bitmap);

        mFirebaseInstance = FirebaseDatabase.getInstance();
        mFirebaseInstance.setPersistenceEnabled(true);
        // get reference to 'cards' node
        mFirebaseDatabase = mFirebaseInstance.getReference("cards");
        // store app title to 'app_title' node
        mFirebaseInstance.getReference("app_title").setValue("Realtime Database");

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createCard(title, codenumber);
            }
        });
    }

    /**
     * Creating new card item node under 'cards'
     */
    private void createCard(String title, String code){
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(LABEL_PATH)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if(title.contains(line)){
                    cardId = line;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read label list.", e);
        }

        CardItem cardItem = new CardItem(title, code);
        mFirebaseDatabase.child(cardId).setValue(cardItem);

        Toast.makeText(this, "Card with id: " + cardId + " has been added", Toast.LENGTH_SHORT).show();
    }

    //Optimize generating barcode image for different barcode formats
    private void createBarcode (int format){
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            switch (format){
                case FirebaseVisionBarcode.FORMAT_EAN_13:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.EAN_13, 300, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_EAN_8:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.EAN_8, 300, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_CODABAR:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.CODABAR, 300, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_CODE_39:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.CODE_39, 300, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_CODE_93:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.CODE_93, 300, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_CODE_128:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.CODE_128, 300, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_QR_CODE:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.QR_CODE, 200, 200);
                    break;
                default:
                    Log.i(TAG, "Unknown format" + format);
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }

        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        bitmap = barcodeEncoder.createBitmap(bitMatrix);
    }

    /*private void createCover(String id){
        switch(id){
            case "vero":
                imageCover.setImageResource(R.drawable.vero);
                break;
            case "eurofarm":
                imageCover.setImageResource(R.drawable.eurofarm);
                break;
            case "sikkomerc":
                imageCover.setImageResource(R.drawable.sikkomerc);
                break;
            case "zegin":
                imageCover.setImageResource(R.drawable.zegin);
                break;
            case "tinex":
                imageCover.setImageResource(R.drawable.tinex);
                break;
            case "lyoness":
                imageCover.setImageResource(R.drawable.lyoness);
            default:
                Log.i(TAG, "Unknown label");
        }
    }*/

    /* Card data change listener
    private void addCardChangeListener(){
        mFirebaseDatabase.child(cardId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                CardItem cardItem = dataSnapshot.getValue(CardItem.class);
                if(cardItem == null){
                    Log.e(TAG, "Card data is null");
                    return;
                }

                //Display newly updated title and barcode
                //txtuser.setText(cardItem.title + "," + cardItem.codenumber);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read user", error.toException());
            }
        });
    }**/
}
