package com.example.detectorapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.detectorapp.CameraClasses.CameraSource;
import com.example.detectorapp.CameraClasses.CameraSourcePreview;
import com.example.detectorapp.CameraClasses.GraphicOverlay;
import com.example.detectorapp.custommodel.CustomImageClassifierProcessor;
import com.google.firebase.ml.common.FirebaseMLException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
  * This activity does classification based on custom tensorflow-lite model, to predict which type of card is being detected.
  * Loads on click on fab button in Main Activity. Implements an interface declared in its Processor, whose method returns the label with
  * highest prediction value in the labels list.
 **/
public class DetectCardActivity extends AppCompatActivity implements CustomImageClassifierProcessor.CardDetectorListener{
    private static final String TAG = "Detecting Activity";
    private static final int PERMISSION_REQUESTS = 1;

    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;

    TextView title;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_detection);

        preview = findViewById(R.id.firePreview);
        graphicOverlay = findViewById(R.id.fireFaceOverlay);

        title = (TextView) findViewById(R.id.text);

        CustomImageClassifierProcessor.cardDetectorListener = this;

        if(allPermissionsGranted()){
            createCameraSource();
        } else {
            getRuntimePermissions();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startCameraSource();
        Toast.makeText(DetectCardActivity.this, "Number:" + Camera.getNumberOfCameras(), Toast.LENGTH_SHORT).show();
    }

    private void createCameraSource() {
        if(cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        try {
            cameraSource.setMachineLearningFrameProcessor(new CustomImageClassifierProcessor(this));
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     **/
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startCameraSource();
    }

    /**
     * Stops the camera.
     **/
    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
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
        if(allPermissionsGranted()){
            createCameraSource();
        }
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
     * After 5 seconds, the method returns the label with highest prediction value (the last element in the label list, as the labels are sorted)
     * and the same value is passed to the next activity using Intent.
     **/
    @Override
    public void onTypeDetected(final List<String> labels) {
        final Timer timer = new Timer("Timer");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent thisintent = new Intent(DetectCardActivity.this, DetectBarcodeActivity.class);
                thisintent.putExtra("type", labels.get(labels.size() - 1));
                thisintent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                thisintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(thisintent);
                overridePendingTransition(0, 0);
                finish();
            }
        }, 5000);
    }
}
