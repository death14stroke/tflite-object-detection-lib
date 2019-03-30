package com.andruid.magic.objectdetection;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class BitmapUtils {
    private static float ratioX, ratioY;

    public static Bitmap resize(Bitmap src){
        int originalX = src.getWidth();
        int originalY = src.getHeight();
        ratioX = originalX / (float) ModelAPI.INPUT_SIZE;
        ratioY = originalY / (float) ModelAPI.INPUT_SIZE;
        return Bitmap.createScaledBitmap(src, ModelAPI.INPUT_SIZE, ModelAPI.INPUT_SIZE,
                false);
    }

    static void translate(RectF detection) {
        detection.top = detection.top * ratioY;
        detection.bottom = detection.bottom * ratioY;
        detection.left = detection.left * ratioX;
        detection.right = detection.right * ratioX;
    }
}