package com.andruid.magic.objectdetection;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ModelAPI implements Classifier {
    private static final String MODEL_FILE = "detect.tflite",
            LABEL_FILE = "file:///android_asset/labelmap.txt";
    private static final int NUM_DETECTIONS = 10, NO_OF_BYTES_PER_CHANNELS = 1,
            NUM_THREADS = 4;
    static final int INPUT_SIZE = 300;
    private static final float MIN_CONFIDENCE = 0.6f;
    private Vector<String> labels = new Vector<>(), selectLabels = new Vector<>();
    public static final int MODE_ALL = 0, MODE_EXCLUDE = 1, MODE_INCLUDE = 2;
    private int mode = MODE_ALL;
    private int[] intValues;
    private float[][][] outputLocations;
    private float[][] outputClasses, outputScores;
    private float[] numDetections;
    private ByteBuffer imgData;
    private Interpreter tfLite;

    private ModelAPI() {}

    private static MappedByteBuffer loadModelFile(AssetManager assets)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public static Classifier create(final AssetManager assetManager) throws IOException {
        final ModelAPI d = new ModelAPI();
        String actualFilename = LABEL_FILE.split("file:///android_asset/")[1];
        InputStream labelsInput = assetManager.open(actualFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null)
            d.labels.add(line);
        br.close();
        try {
            d.tfLite = new Interpreter(loadModelFile(assetManager));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        d.imgData = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * NO_OF_BYTES_PER_CHANNELS);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[INPUT_SIZE * INPUT_SIZE];
        d.tfLite.setNumThreads(NUM_THREADS);
        d.outputLocations = new float[1][NUM_DETECTIONS][4];
        d.outputClasses = new float[1][NUM_DETECTIONS];
        d.outputScores = new float[1][NUM_DETECTIONS];
        d.numDetections = new float[1];
        return d;
    }

    @SuppressLint("UseSparseArrays")
    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imgData.rewind();
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                int pixelValue = intValues[i * INPUT_SIZE + j];
                imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                imgData.put((byte) (pixelValue & 0xFF));
            }
        }
        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];
        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        final ArrayList<Recognition> recognitions = new ArrayList<>(NUM_DETECTIONS);
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            RectF detection = new RectF(outputLocations[0][i][1] * INPUT_SIZE,
                    outputLocations[0][i][0] * INPUT_SIZE,
                    outputLocations[0][i][3] * INPUT_SIZE,
                    outputLocations[0][i][2] * INPUT_SIZE);
            BitmapUtils.translate(detection);
            int labelOffset = 1;
            int classNum = (int) outputClasses[0][i] + labelOffset;
            float confidence = outputScores[0][i];
            String label = labels.get(classNum);
            if(mode==MODE_EXCLUDE && selectLabels.contains(label))
                continue;
            else if(mode==MODE_INCLUDE && !selectLabels.contains(label))
                continue;
            if(confidence>=MIN_CONFIDENCE)
                recognitions.add(new Recognition(i, label, confidence, detection));
        }
        return recognitions;
    }

    @Override
    public void close() {
        tfLite.close();
    }

    @Override
    public Vector<String> getLabels() {
        return labels;
    }

    @Override
    public void addSelectedLabel(String label) {
        selectLabels.add(label);
    }

    @Override
    public void addSelectedLabels(Collection<String> labels) {
        selectLabels.addAll(labels);
    }

    @Override
    public Vector<String> getSelectedLabels() {
        return selectLabels;
    }

    @Override
    public void clearSelected() {
        selectLabels.clear();
    }

    @Override
    public void removeLabel(int index) {
        selectLabels.remove(index);
    }

    @Override
    public void removeLabel(String label) {
        selectLabels.remove(label);
    }

    @Override
    public void removeLabels(Collection<String> labels) {
        selectLabels.removeAll(labels);
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public void setMode(int mode) {
        this.mode = mode;
    }
}