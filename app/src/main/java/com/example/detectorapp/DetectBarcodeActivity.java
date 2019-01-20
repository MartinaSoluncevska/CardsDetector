package com.example.detectorapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.example.detectorapp.CameraClasses.CameraSource;
import com.example.detectorapp.CameraClasses.CameraSourcePreview;
import com.example.detectorapp.CameraClasses.GraphicOverlay;
import com.example.detectorapp.barcodemodel.BarcodeScanningProcessor;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This activity does classification based on barcode model, to predict which type of barcode the card being detected has.
 * Implements an interface declared in its Processor, whose method returns the barcode being detected and the barcode number using
 * the displayData() method.
 **/

public class DetectBarcodeActivity extends AppCompatActivity implements BarcodeScanningProcessor.BarcodeDetectorListener{
    private static final int PERMISSION_REQUESTS = 1;
    private static final String TAG = "Detecting Activity";

    private CameraSource anotherCameraSource = null;
    private CameraSourcePreview anotherPreview;
    private GraphicOverlay anotherGraphicOverlay;

    private String label;
    TextView screenTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        anotherPreview = findViewById(R.id.codePreview);
        anotherGraphicOverlay = findViewById(R.id.fireCodeOverlay);

        screenTitle = (TextView) findViewById(R.id.text2);

        BarcodeScanningProcessor.barcodeDetectorListener = this;

        //Extracting the card label value passed using intent (created in the previous activity)
        Intent thisintent = getIntent();
        label = thisintent.getStringExtra("type");

        if (allPermissionsGranted()){
            createAnotherCameraSource();
            startAnotherCameraSource();
        } else {
            getRuntimePermissions();
        }
    }

    private void createAnotherCameraSource() {
        // If there's no existing cameraSource, create one.
        if (anotherCameraSource == null) {
            anotherCameraSource = new CameraSource(this, anotherGraphicOverlay);
        }
        anotherCameraSource.setMachineLearningFrameProcessor(new BarcodeScanningProcessor());
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     **/
    private void startAnotherCameraSource() {
        if (anotherCameraSource != null) {
            try {
                anotherPreview.start(anotherCameraSource, anotherGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                anotherCameraSource.release();
                anotherCameraSource = null;
            }
        }
    }

    /**
     * Stops the camera.
     **/
    @Override
    protected void onPause() {
        super.onPause();
        anotherPreview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (anotherCameraSource != null) {
            anotherCameraSource.release();
        }
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        createAnotherCameraSource();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    /**
     * The method returns the detected barcode and its number value, using a predefined method. Both values
     * (for card's label with prediction value and for barcode number) will be passed to the next activity using Intent.
     **/
    @Override
    public void onBarcodeDetected(final FirebaseVisionBarcode data) {
        anotherCameraSource.stop();

        Intent myintent = new Intent(DetectBarcodeActivity.this, CreateCardActivity.class);
        myintent.putExtra("label", label);
        myintent.putExtra("code", data.getDisplayValue());
        myintent.putExtra("format", data.getFormat());
        startActivity(myintent);
        finish();
    }
}
