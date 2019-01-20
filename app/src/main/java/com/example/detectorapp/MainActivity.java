package com.example.detectorapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.detectorapp.recyclerview.CardItem;
import com.example.detectorapp.recyclerview.LocationItem;
import com.example.detectorapp.recyclerview.MyAdapter;
import com.example.detectorapp.recyclerview.MyTouchListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "Main Activity";
    ImageView mImage, cardimg, cardcode;
    TextView mTitle, mNumber, mFormat, cardnumber;
    List<CardItem> itemList;
    List<LocationItem> locations;
    List<Float> distances;

    RecyclerView mRecyclerView;
    MyAdapter mAdapter;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest settingsRequest;
    private LocationCallback locationCallback;
    private Location location;
    private Boolean requestingUpdates;
    private static final int REQUEST_CHECK_SETTINGS = 100;

    private DatabaseReference databaseReference;
    private DatabaseReference coordsReference;
    private FirebaseDatabase database;

    private Bitmap bitmap;
    private BitMatrix bitMatrix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTitle = (TextView) findViewById(R.id.txtlabel);
        mNumber = (TextView) findViewById(R.id.txtnumber);
        mFormat = (TextView) findViewById(R.id.txtformat);
        mImage = (ImageView) findViewById(R.id.imageview);

        itemList = new ArrayList<CardItem>();
        locations = new ArrayList<LocationItem>();
        distances = new ArrayList<>();

        Toolbar toolbar = (Toolbar) findViewById(R.id.appname);
        setSupportActionBar(toolbar);

        //Creating RecyclerView to display card data, that are being added when clicked fab button
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.addOnItemTouchListener(new MyTouchListener(getApplicationContext(), mRecyclerView, new MyTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                CardItem item = itemList.get(position);

                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.dialog_item);
                dialog.setCancelable(true);

                cardimg = (ImageView) dialog.findViewById(R.id.cardimg);
                cardcode = (ImageView) dialog.findViewById(R.id.cardcode);
                cardnumber = (TextView) dialog.findViewById(R.id.cardnumber);

                Glide.with(getApplicationContext()).load(item.getImageUrl()).into(cardimg);
                cardnumber.setText(item.getCodenumber());
                createBarcode(item.getFormat(), item.getCodenumber());

                dialog.show();
            }
        }));

        //Display items in the RecyclerView using LinearLayout
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter(MainActivity.this, itemList);

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("cards");
        coordsReference = database.getReference("coordinates");

        //Populate recycler view from database, then retrieve latitude and longitude
        retrieveData();
        retrieveLocations();

        //Initializing libraries needed to get current location and granting permissions
        init();
        restoreValuesFromBundle(savedInstanceState);

        //Start DetectCard Activity onclick, which loads camera and its preview for detecting objects
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DetectCardActivity.class);
                startActivity(intent);
            }
        });
    }

    private void init(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //get current location as result
                location = locationResult.getLastLocation();
                updateUI();
            }
        };
        requestingUpdates = false;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        settingsRequest = builder.build();
    }

    //restore the values from saved instance state
    private void restoreValuesFromBundle(Bundle savedInstanceState){
        if(savedInstanceState != null){
            if(savedInstanceState.containsKey("is_requesting_updates")){
                requestingUpdates = savedInstanceState.getBoolean("is_requesting_updates");
            }
        }
        updateUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_requesting_updates", requestingUpdates);
    }

    //update recyclerview order of elements
    private void updateUI(){
        if(location != null){
            //current location data
            Log.e(TAG, "Location: " + location.getLatitude() + " ," + location.getLongitude());

            for(int i=0; i<locations.size(); i++){
                //create Location instances for each element in the list
                Location location1 = new Location("");
                location1.setLatitude(locations.get(i).getLatitude());
                location1.setLongitude(locations.get(i).getLongitude());

                //calculate distance between current location and each location populated from the database
                float distance = location.distanceTo(location1);
                distances.add(distance);
            }

            //find the element with minimal distance in the array
            int minIndex = distances.indexOf(Collections.min(distances));
            int fromPosition;
            int toPosition = 0;
            for(int k=0; k < itemList.size(); k++){
                //if the position of the item is 0, do nothing
                //if it's not 0, find the item in the list and move it to position 0
                if(itemList.get(k).getTitle().equals(locations.get(minIndex).getItemTitle()) && k != 0){
                    fromPosition = k;
                    CardItem wantedItem = itemList.get(fromPosition);
                    itemList.remove(fromPosition);
                    itemList.add(toPosition, wantedItem);
                    mAdapter.notifyItemMoved(fromPosition, toPosition);
                }
            }
            stopLocationUpdates();
            Toast.makeText(MainActivity.this, "Nearest location: " + locations.get(minIndex).getItemTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    //request updates for location from provider using Fused Location Provider Api
    private void startLocationUpdates(){
        settingsClient.checkLocationSettings(settingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        //if settings are satisfied
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        updateUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //if settings aren't satisfied, display a dialog and check result in onActivityResult()
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                Log.e(TAG,"Location settings cannot be fixed here. Fix in Settings.");
                        }
                        updateUI();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Request for ACCESS_FINE_LOCATION with Dexter library
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        requestingUpdates = true;
                        startLocationUpdates();;
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            //calling method to open device settings
                            openSettings();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            //check for the integer request code
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.e(TAG, "User agreed to make required location settings changes.");
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.e(TAG, "User chose not to make required location settings changes.");
                        requestingUpdates = false;
                        break;
                }
                break;
        }
    }

    private void retrieveLocations(){
        coordsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                locations.clear();
                for(DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                    LocationItem i = new LocationItem();
                    i.setItemTitle(dataSnapshot1.getValue(LocationItem.class).getItemTitle());
                    i.setLatitude((Double) dataSnapshot1.child("lat").getValue());
                    i.setLongitude((Double) dataSnapshot1.child("lng").getValue());
                    locations.add(i);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    //Retrieve all cards from Firebase database
    private void retrieveData(){
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                CardItem item = dataSnapshot.getValue(CardItem.class);
                itemList.add(item);

                //Populate RecyclerView's adapter with items from the list created above
                mRecyclerView.setAdapter(mAdapter);
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void createBarcode (int format, String codenumber){
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            switch (format){
                case FirebaseVisionBarcode.FORMAT_EAN_13:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.EAN_13, 400, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_EAN_8:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.EAN_8, 400, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_CODABAR:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.CODABAR, 400, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_CODE_39:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.CODE_39, 400, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_CODE_93:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.CODE_93, 400, 200);
                    break;
                case FirebaseVisionBarcode.FORMAT_CODE_128:
                    bitMatrix = multiFormatWriter.encode(codenumber, BarcodeFormat.CODE_128, 400, 200);
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
        cardcode.setImageBitmap(bitmap);
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    //remove updates for location when user closes the app
    public void stopLocationUpdates(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.e(TAG, "Location updates stopped!");
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        requestingUpdates = false;
        stopLocationUpdates();
    }

    private void openSettings(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requestingUpdates && checkPermissions()) {
            startLocationUpdates();
        }
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (requestingUpdates) {
            //pausing location updates to save battery power
            stopLocationUpdates();
        }
    }


}