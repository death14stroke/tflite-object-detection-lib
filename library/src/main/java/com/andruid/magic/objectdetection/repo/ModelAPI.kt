package com.andruid.magic.objectdetection.repo

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import com.andruid.magic.objectdetection.model.Recognition
import com.andruid.magic.objectdetection.util.resize
import com.andruid.magic.objectdetection.util.translate
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

/**
 * Wrapper class for running tflite object detection
 */
class ModelAPI : Classifier {
    companion object {
        // path of model file in assets
        private const val MODEL_FILE = "detect.tflite"
        // path of labels file in assets
        private const val LABEL_FILE = "file:///android_asset/labelmap.txt"

        // max number of detections
        private const val NUM_DETECTIONS = 10

        private const val NO_OF_BYTES_PER_CHANNELS = 1
        private const val NUM_THREADS = 4

        /** minimum confidence level */
        private const val MIN_CONFIDENCE = 0.6f
        /** model input size */
        const val INPUT_SIZE = 300

        /** detect all labels mode */
        const val MODE_ALL = 0
        /** detect all labels excluding [selectLabels] mode */
        const val MODE_EXCLUDE = 1
        /** detect all labels present in [selectLabels] mode */
        const val MODE_INCLUDE = 2

        /**
         * Load tflite model into memory
         * @param assets assetManager from the context
         * @return mapped byte buffer
         */
        @Throws(IOException::class)
        private fun loadModelFile(assets: AssetManager): MappedByteBuffer {
            val fileDescriptor = assets.openFd(MODEL_FILE)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }

        /**
         * Create static instance
         * @param assetManager assetManager from the context
         * @return static instance
         */
        @Throws(IOException::class)
        fun create(assetManager: AssetManager): Classifier {
            val d = ModelAPI()
            val actualFilename = LABEL_FILE.split("file:///android_asset/".toRegex()).toTypedArray()[1]
            val labelsInput = assetManager.open(actualFilename)
            BufferedReader(InputStreamReader(labelsInput)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null)
                    d.labels.add(line)
            }
            try {
                val options = Interpreter.Options()
                        .setNumThreads(NUM_THREADS)
                d.tfLite = Interpreter(loadModelFile(assetManager), options)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            return d.apply {
                imgData = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * NO_OF_BYTES_PER_CHANNELS)
                imgData.order(ByteOrder.nativeOrder())

                intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
            }
        }
    }

    /** labels which model can detect */
    private val labels = Vector<String>()
    /** labels selected for output */
    private val selectLabels = Vector<String>()

    /** mode for object detection
     * @see MODE_ALL
     * @see MODE_EXCLUDE
     * @see MODE_INCLUDE */
    private var mode = MODE_ALL

    private lateinit var intValues: IntArray
    private lateinit var outputLocations: Array<Array<FloatArray>>
    private lateinit var outputClasses: Array<FloatArray>
    private lateinit var outputScores: Array<FloatArray>
    private lateinit var numDetections: FloatArray
    private lateinit var imgData: ByteBuffer
    private lateinit var tfLite: Interpreter

    override fun recognizeImage(src: Bitmap): List<Recognition> {
        val bitmap = src.resize()

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        imgData.rewind()

        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val pixelValue = intValues[i * INPUT_SIZE + j]
                imgData.apply {
                    put((pixelValue shr 16 and 0xFF).toByte())
                    put((pixelValue shr 8 and 0xFF).toByte())
                    put((pixelValue and 0xFF).toByte())
                }
            }
        }

        outputLocations = Array(1) { Array(NUM_DETECTIONS) { FloatArray(4) } }
        outputClasses = Array(1) { FloatArray(NUM_DETECTIONS) }
        outputScores = Array(1) { FloatArray(NUM_DETECTIONS) }
        numDetections = FloatArray(1)

        val inputArray = arrayOf<Any>(imgData)
        val outputMap = mapOf(0 to outputLocations,
                1 to outputClasses,
                2 to outputScores,
                3 to numDetections)
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap)

        val recognitions = ArrayList<Recognition>(NUM_DETECTIONS)
        for (i in 0 until NUM_DETECTIONS) {
            val detection = RectF(outputLocations[0][i][1] * INPUT_SIZE,
                    outputLocations[0][i][0] * INPUT_SIZE,
                    outputLocations[0][i][3] * INPUT_SIZE,
                    outputLocations[0][i][2] * INPUT_SIZE)
            src.translate(detection)
            val labelOffset = 1
            val classNum = outputClasses[0][i].toInt() + labelOffset
            val confidence = outputScores[0][i]
            val label = labels[classNum]

            if (mode == MODE_EXCLUDE && selectLabels.contains(label))
                continue
            else if (mode == MODE_INCLUDE && !selectLabels.contains(label))
                continue

            if (confidence >= MIN_CONFIDENCE)
                recognitions.add(Recognition(id = i,
                        title = label,
                        confidence = confidence,
                        location = detection))
        }
        return recognitions
    }

    override fun close() {
        tfLite.close()
    }

    override fun addSelectedLabels(vararg labels: String) {
        selectLabels.addAll(labels)
    }

    override fun getSelectedLabels() = selectLabels

    override fun clearSelected() {
        selectLabels.clear()
    }

    override fun removeLabel(index: Int) {
        selectLabels.removeAt(index)
    }

    override fun removeLabels(vararg labels: String) {
        selectLabels.removeAll(labels)
    }

    override fun getLabels() = labels

    override fun getMode() = mode

    override fun setMode(mode: Int) {
        this.mode = mode
    }
}