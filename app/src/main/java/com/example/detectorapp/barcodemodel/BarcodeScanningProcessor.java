package com.example.detectorapp.barcodemodel;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.detectorapp.CameraClasses.CameraImageGraphic;
import com.example.detectorapp.CameraClasses.FrameMetadata;
import com.example.detectorapp.CameraClasses.GraphicOverlay;
import com.example.detectorapp.VisionProcessorBase;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BarcodeScanningProcessor extends VisionProcessorBase<List<FirebaseVisionBarcode>>{
    private static final String TAG = "BarcodeScanProc";
    private final FirebaseVisionBarcodeDetector detector;
    public static BarcodeDetectorListener barcodeDetectorListener;

    public BarcodeScanningProcessor() {
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector();
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Barcode Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(@Nullable Bitmap originalCameraImage,
                             @NonNull List<FirebaseVisionBarcode> barcodes,
                             @NonNull FrameMetadata frameMetadata,
                             @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        if (originalCameraImage != null) {
            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
            graphicOverlay.add(imageGraphic);
        }
        for (int i = 0; i <barcodes.size(); ++i) {
            FirebaseVisionBarcode barcode = barcodes.get(i);
            BarcodeGraphic barcodeGraphic = new BarcodeGraphic(graphicOverlay, barcode);
            graphicOverlay.add(barcodeGraphic);
            graphicOverlay.postInvalidate();

            if (barcodeDetectorListener == null) return;
            barcodeDetectorListener.onBarcodeDetected(barcode);
            barcodeDetectorListener = null;
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Barcode detection failed " + e);
    }

    public interface BarcodeDetectorListener {
        void onBarcodeDetected (FirebaseVisionBarcode data);
    }
}
