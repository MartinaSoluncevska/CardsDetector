package com.example.detectorapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.detectorapp.recyclerview.CardItem;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

/**
 * In this activity, values for card label and barcode are being extracted and displayed on the screen. Also based on the card label,
 * a suitable visual representation of the card is being loaded in the top of the screen.
 **/
public class CreateCardActivity extends AppCompatActivity{
    private static final String TAG = "Results Activity";
    private static final String LABEL_PATH = "retrained_labels.txt";

    private ImageView imageView, imageCover;
    private TextView txtBarcode;
    private String cardId, title, codenumber, imglink;
    private String uniqueID;
    private int cardformat;

    private DatabaseReference mDatabase;
    private FirebaseDatabase mRef;
    private StorageReference mStorage;

    private Bitmap bitmap;
    private BitMatrix bitMatrix;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.results_view);

        Toolbar thistoolbar = (Toolbar) findViewById(R.id.cardtoolbar);
        setSupportActionBar(thistoolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        Intent myintent = getIntent();
        title = myintent.getStringExtra("label");
        codenumber = myintent.getStringExtra("code");
        cardformat = myintent.getIntExtra("format", 0);
        uniqueID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

       //extracting the type of card only, since the title variable contains both type and prediction value
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

        imageCover = findViewById(R.id.cover);
        imageView = findViewById(R.id.barcodeimg);
        txtBarcode = findViewById(R.id.codenumber);
        txtBarcode.setText(codenumber);

        mStorage = FirebaseStorage.getInstance().getReference();
        mRef = FirebaseDatabase.getInstance();
        mRef.setPersistenceEnabled(true);

        mRef.getReference("app_title").setValue("Realtime Database");
        mDatabase = mRef.getReference("cards");
        mDatabase.keepSynced(true);
        mDatabase.onDisconnect().setValue("I disconnected!");

        //Generate image dynamically, based on the cardId that the card we are detecting has.
        createCover(cardId);
        //Generate barcode image based on the barcode format value that the card we are detecting has.
        createBarcode(cardformat, codenumber, imageView);
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return false;
    }

    //Store new card object node in the database, using the button in the appbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //Handle click on button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.savebutton) {
            createCard(cardId, codenumber, imglink, cardformat);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    //Create new card object
    private void createCard(String title, String code, String link, int format){
        CardItem cardItem = new CardItem(title, code, link, format);
        mDatabase.child(uniqueID).child(cardId).setValue(cardItem);
    }

    //Optimize generating barcode image for different barcode formats
    private void createBarcode (int format, String codenumber, ImageView imageview){
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
        imageview.setImageBitmap(bitmap);
    }

    //Display image in the top of the layout
    private void createCover(String id){
        /* Complete the url based on cardId
        (images previously stored in the storage should have only .png extension) */
        String url = "card_photos/" + id + ".png";

        //Create preview, using the downloadUrl link from Firebase Storage
        mStorage.child(url).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getApplicationContext())
                        .load(uri)
                        .into(imageCover);

                //Store the url in a variable, later passed to the createCard()
                imglink = uri.toString();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

}
