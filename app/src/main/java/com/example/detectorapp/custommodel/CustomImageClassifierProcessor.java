package com.example.detectorapp.custommodel;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.example.detectorapp.CameraClasses.BitmapUtils;
import com.example.detectorapp.CameraClasses.CameraImageGraphic;
import com.example.detectorapp.CameraClasses.FrameMetadata;
import com.example.detectorapp.CameraClasses.GraphicOverlay;
import com.example.detectorapp.CameraClasses.VisionImageProcessor;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CustomImageClassifierProcessor implements VisionImageProcessor {
    private static final String TAG = "Custom";
    private final CustomImageClassifier classifier;
    private final Activity activity;
    public static CardDetectorListener cardDetectorListener;

    long a = System.currentTimeMillis();
    long b = System.currentTimeMillis();

    private List<String> list = new ArrayList<>();

    public CustomImageClassifierProcessor(Activity activity) throws FirebaseMLException{
        this.activity = activity;
        classifier = new CustomImageClassifier(activity);
    }

    @Override
    public void process(final ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) throws FirebaseMLException {
        classifier.classifyFrame(data, frameMetadata.getWidth(), frameMetadata.getHeight())
                .addOnSuccessListener(
                        activity,
                        new OnSuccessListener<List<String>>() {
                            @Override
                            public void onSuccess(final List<String> result) {
                                LabelGraphic labelGraphic = new LabelGraphic(graphicOverlay, result);
                                Bitmap bitmap = BitmapUtils.getBitmap(data, frameMetadata);
                                CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, bitmap);
                                graphicOverlay.clear();
                                graphicOverlay.add(imageGraphic);
                                graphicOverlay.add(labelGraphic);
                                graphicOverlay.postInvalidate();

                                while (true){
                                    list.addAll(result);
                                    for (int i = 0; i < list.size(); i++) {
                                        if (cardDetectorListener == null) return;
                                        cardDetectorListener.onTypeDetected(list);
                                        cardDetectorListener = null;

                                        if(b-a >=5000) break;
                                    }
                                }

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "Custom classifier failed: " + e);
                                e.printStackTrace();
                            }
                        });
    }

    @Override
    public void process(Bitmap bitmap, GraphicOverlay graphicOverlay) {
        //nothing
    }

    @Override
    public void stop() {
    }

    public interface CardDetectorListener {
        void onTypeDetected (List<String> labels);
    }
}
