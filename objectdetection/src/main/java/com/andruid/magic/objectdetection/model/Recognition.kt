package com.andruid.magic.objectdetection.model

import android.graphics.RectF

/**
 * Model class for all results returned from the object detection model
 * @property id recognition result id
 * @property title label of the object
 * @property confidence confidence returned by the model
 * @property location rectangular coordinates of object in the image
 */
data class Recognition(
        val id: Int,
        val title: String,
        val confidence: Float,
        val location: RectF
)