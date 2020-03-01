package com.andruid.magic.objectdetection.repo

import android.graphics.Bitmap
import com.andruid.magic.objectdetection.model.Recognition
import java.util.*

/**
 * Repository interface for object detection model
 */
interface Classifier {
    /**
     * Find all objects in the source image
     * @param src input bitmap of size [ModelAPI.INPUT_SIZE] * [ModelAPI.INPUT_SIZE]
     * @return list of objects found
     */
    fun recognizeImage(src: Bitmap): List<Recognition>

    /**
     * Close the model after use
     */
    fun close()

    /**
     * Get all labels that can be recognized by the model
     * @return list of labels
     */
    fun getLabels(): Vector<String>

    /**
     * Get all selected labels for detection
     * @return list of selected labels
     */
    fun getSelectedLabels(): Vector<String>

    /**
     * Add labels to list of selected labels for detection
     * @param labels selected labels to be added
     */
    fun addSelectedLabels(vararg labels: String)

    /**
     * Remove label from selected list
     * @param index position of the label
     */
    fun removeLabel(index: Int)

    /**
     * Remove labels from selected list
     * @param labels label(s) to be removed
     */
    fun removeLabels(vararg labels: String)

    /**
     * Remove all selected labels for detection
     */
    fun clearSelected()

    /**
     * Fetch mode for detection: [ModelAPI.MODE_ALL], [ModelAPI.MODE_INCLUDE], [ModelAPI.MODE_EXCLUDE]
     * @return mode
     */
    fun getMode(): Int

    /**
     * Set mode for detection: [ModelAPI.MODE_ALL], [ModelAPI.MODE_INCLUDE], [ModelAPI.MODE_EXCLUDE]
     * @param mode detection mode
     */
    fun setMode(mode: Int)
}