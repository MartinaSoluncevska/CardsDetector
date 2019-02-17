package com.example.detectorapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import com.example.detectorapp.custommodel.CustomImageClassifierProcessor.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.Environment.getExternalStoragePublicDirectory;
import static java.io.File.separator;

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

    private File imageFile;
    private StorageReference uploadsStorage;

    private ArrayList<List<String>> finalArray = new ArrayList<>();

    TextView title;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_detection);

        preview = findViewById(R.id.firePreview);
        graphicOverlay = findViewById(R.id.fireFaceOverlay);
        title = (TextView) findViewById(R.id.text);

        CustomImageClassifierProcessor.cardDetectorListener = this;

        uploadsStorage = FirebaseStorage.getInstance().getReference().child("uploaded_photos/");

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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(allPermissionsGranted()){
            Log.i(TAG, "Permission granted!");
            createCameraSource();
        }
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

    private String getPhotoTime(){
        SimpleDateFormat sdf=new SimpleDateFormat("ddMMyy_hhmmss");
        return sdf.format(new Date());
    }

    /**
     * Takes a picture using the existing camera source and saves it locally and on cloud. If the folder
     * doesn't exist yet on the device, it will be created as a subfolder in the gallery.
     **/
    private void takeSnapshot(byte[] data){
        try {
            // convert byte array into bitmap
            Bitmap loadedImage = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0, loadedImage.getWidth(), loadedImage.getHeight(),
                    new Matrix(), false);

            File dir = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Downloads");
            boolean success = true;
            if (!dir.exists())
                success = dir.mkdirs();

            if (success) {
                imageFile = new File(dir.getAbsolutePath() + separator + getPhotoTime() + "Image.jpg");
                imageFile.createNewFile();
            } else {
                Toast.makeText(getBaseContext(), "Image Not saved", Toast.LENGTH_SHORT).show();
                return;
            }

            // save image into gallery
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(ostream.toByteArray());
            fos.close();

            //upload image to firebase storage
            Uri uploadUri = Uri.fromFile(imageFile);
            final String cloudFilePath = uploadUri.getLastPathSegment();
            uploadsStorage.child(cloudFilePath).putFile(uploadUri).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Failed to upload");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(DetectCardActivity.this, "Image has been uploaded", Toast.LENGTH_SHORT).show();
                }
            });

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.MediaColumns.DATA, imageFile.getAbsolutePath());
            DetectCardActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * After 5 seconds, the method returns the label with highest prediction value (the last element in the label list,
     * as the labels are sorted) and the same value is passed to the next activity using Intent.
     **/
    @Override
    public void onTypeDetected(final List<String> elements) {
        final Timer timer = new Timer("Timer");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent thisintent = new Intent(DetectCardActivity.this, DetectBarcodeActivity.class);
                thisintent.putExtra("type", elements.get(elements.size() - 1));
                thisintent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                thisintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(thisintent);
                overridePendingTransition(0, 0);
                finish();
            }
        }, 5000);
    }
        /*List<Float> values = new ArrayList();
        Pattern pattern = Pattern.compile("([0-9]+[.][0-9]+)");
        for(int i = 0; i < results.size(); i++){
            List<String> sample = results.get(i);
            Matcher matcher = pattern.matcher(sample.get(sample.size()-1));
            while(matcher.find()){
                String s = matcher.group();
                float number = Float.parseFloat(s);
                values.add(number);
            }
        }*/
}
