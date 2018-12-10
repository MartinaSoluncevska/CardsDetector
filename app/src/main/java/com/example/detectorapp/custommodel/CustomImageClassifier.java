package com.example.detectorapp.custommodel;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.ml.custom.model.FirebaseCloudModelSource;
import com.google.firebase.ml.custom.model.FirebaseLocalModelSource;
import com.google.firebase.ml.custom.model.FirebaseModelDownloadConditions;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class CustomImageClassifier {
    private static final String TAG = "DetectorApp:Classifier";

    // Name of the floating point model file.
    private static final String LOCAL_FLOAT_MODEL_NAME = "optimized_graph";

    //Path of the floating point model file stored in Assets.
    private static final String LOCAL_FLOAT_MODEL_PATH = "optimized_graph.lite";

    //Name of the floating point model uploaded to the Firebase console
    private static final String HOSTED_FLOAT_MODEL_NAME = "optimized_graph";

    //Name of the label file stored in Assets.
    private static final String LABEL_PATH = "retrained_labels.txt";

    // Number of results to show in the UI.
    private static final int RESULTS_TO_SHOW = 4;

    // Dimensions of inputs.
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int DIM_IMG_SIZE_X = 224;
    private static final int DIM_IMG_SIZE_Y = 224;
    //private static final int QUANT_NUM_OF_BYTES_PER_CHANNEL = 1;
    private static final int FLOAT_NUM_OF_BYTES_PER_CHANNEL = 4;
    //private Boolean mUseQuantizedModel;

    //Preallocated buffers for storing image data in.
    private final int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

    //An instance of the driver class to run model inference with Firebase.
    private final FirebaseModelInterpreter interpreter;

    //Data configuration of input & output data of model.
    private final FirebaseModelInputOutputOptions dataOptions;

    //Labels corresponding to the output of the vision model.
    private final List<String> labelList;
    public List<String> outputs;

    private final PriorityQueue<Map.Entry<String, Float>> sortedLabels = new PriorityQueue<>(RESULTS_TO_SHOW, new Comparator<Map.Entry<String, Float>>() {
        @Override
        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
            return (o1.getValue()).compareTo(o2.getValue());
        }
    });

    CustomImageClassifier(Activity activity) throws FirebaseMLException {
        String localModelName = LOCAL_FLOAT_MODEL_NAME;
        String hostedModelName = HOSTED_FLOAT_MODEL_NAME;
        String localModelPath = LOCAL_FLOAT_MODEL_PATH;

        FirebaseModelOptions modelOptions = new FirebaseModelOptions.Builder()
                .setCloudModelName(hostedModelName)
                .setLocalModelName(localModelName)
                .build();
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions
                .Builder()
                .requireWifi()
                .build();
        FirebaseLocalModelSource localModelSource = new FirebaseLocalModelSource.Builder(localModelName)
                .setAssetFilePath(localModelPath).build();
        FirebaseCloudModelSource cloudSource = new FirebaseCloudModelSource.Builder(hostedModelName)
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build();
        FirebaseModelManager manager = FirebaseModelManager.getInstance();
        manager.registerLocalModelSource(localModelSource);
        manager.registerCloudModelSource(cloudSource);
        interpreter = FirebaseModelInterpreter.getInstance(modelOptions);
        labelList = loadLabelList(activity);
        Log.d(TAG, "Created a Custom Image Classifier.");
        int[] inputDims = {DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE};
        int[] outputDims = {1, labelList.size()};

        int dataType = FirebaseModelDataType.FLOAT32;
        dataOptions = new FirebaseModelInputOutputOptions.Builder()
                .setInputFormat(0, dataType, inputDims)
                .setOutputFormat(0, dataType, outputDims)
                .build();
        Log.d(TAG, "Configured input & output data for the custom image classifier.");
    }

    /**
     * Classifies a frame from the preview stream.
     */
    Task<List<String>> classifyFrame(ByteBuffer buffer, int width, int height) throws FirebaseMLException {
        if (interpreter == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
            List<String> uninitialized = new ArrayList<>();
            uninitialized.add("Uninitialized Classifier.");
            Tasks.forResult(uninitialized);
        }
        // Create input data.
        ByteBuffer imgData = convertBitmapToByteBuffer(buffer, width, height);

        FirebaseModelInputs inputs = new FirebaseModelInputs.Builder().add(imgData).build();
        // Here's where the magic happens!!
        return interpreter.run(inputs, dataOptions)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to get labels array: " + e.getMessage());
                        e.printStackTrace();
                    }
                })
                .continueWith(
                        new Continuation<FirebaseModelOutputs, List<String>>() {
                            @Override
                            public List<String> then(Task<FirebaseModelOutputs> task) throws Exception {
                                float[][] labelProbArray = task.getResult().<float[][]>getOutput(0);
                                outputs = getTopLabels(labelProbArray);
                                return outputs;
                            }
                        });

    }

    /**
     * Reads label list from Assets.
     */
    private List<String> loadLabelList(Activity activity) {
        List<String> labelList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                labelList.add(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read label list.", e);
        }
        return labelList;
    }

    /**
     * Writes Image data into a {@code ByteBuffer}.
     */
    private synchronized ByteBuffer convertBitmapToByteBuffer(ByteBuffer buffer, int width, int height) {
        int bytesPerChannel = FLOAT_NUM_OF_BYTES_PER_CHANNEL;
        ByteBuffer imgData = ByteBuffer.allocateDirect(bytesPerChannel * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        Bitmap bitmap = createResizedBitmap(buffer, width, height);
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to int points.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                // Normalize the values according to the model used:
                imgData.putFloat(((val >> 16) & 0xFF) / 255.0f);
                imgData.putFloat(((val >> 8) & 0xFF) / 255.0f);
                imgData.putFloat((val & 0xFF) / 255.0f);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to put values into ByteBuffer: " + (endTime - startTime));
        return imgData;
    }

    /**
     * Resizes image data from {@code ByteBuffer}.
     */
    private Bitmap createResizedBitmap(ByteBuffer buffer, int width, int height) {
        YuvImage img = new YuvImage(buffer.array(), ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        img.compressToJpeg(new Rect(0, 0, img.getWidth(), img.getHeight()), 50, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        return Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true);
    }

    private synchronized List<String> getTopLabels(float[][] labelProbArray) {
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(new AbstractMap.SimpleEntry<>(labelList.get(i), labelProbArray[0][i]));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }
        return getTopKLabels();
    }

    /**
     * Gets the top-K labels, to be shown in UI as the results.
     */
    private synchronized List<String> getTopKLabels() {
        List<String> result = new ArrayList<>();
        final int size = sortedLabels.size();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            result.add(label.getKey() + ":" + label.getValue());
        }
        return result;
    }

}
