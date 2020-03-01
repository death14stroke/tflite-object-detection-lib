package com.andruid.magic.objectdetection.util

import android.graphics.Bitmap
import android.graphics.RectF
import com.andruid.magic.objectdetection.repo.ModelAPI

/**
 * Resize the bitmap as input for model
 * @return resized bitmap
 * @receiver input bitmap
 */
fun Bitmap.resize(): Bitmap =
        Bitmap.createScaledBitmap(this, ModelAPI.INPUT_SIZE, ModelAPI.INPUT_SIZE, false)

/**
 * Translate the object location in detection as per original image
 * @param detection object location as per model input size
 * @receiver original bitmap
 */
fun Bitmap.translate(detection: RectF) {
    val originalX = width
    val originalY = height

    val ratioX = originalX / ModelAPI.INPUT_SIZE.toFloat()
    val ratioY = originalY / ModelAPI.INPUT_SIZE.toFloat()

    detection.apply {
        top *= ratioY
        bottom *= ratioY
        left *= ratioX
        right *= ratioX
    }
}