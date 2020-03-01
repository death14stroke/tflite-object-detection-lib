package com.andruid.magic.objectdetectionlib

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.andruid.magic.objectdetection.repo.ModelAPI
import com.andruid.magic.objectdetectionlib.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import splitties.toast.toast

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = "${MainActivity::class.java.simpleName}log"
        private const val PICK_IMAGE = 1
    }

    private lateinit var binding: ActivityMainBinding
    private val classifier by lazy { ModelAPI.create(assets) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.fab.setOnClickListener { pickImage() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null)
                return
            val uri = data.data

            Glide.with(this)
                    .asBitmap()
                    .load(uri)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val drawBitmap = resource.copy(Bitmap.Config.ARGB_8888, true)

                            val canvas = Canvas(drawBitmap)
                            val paint = Paint().apply {
                                color = Color.RED
                                style = Paint.Style.STROKE
                                strokeWidth = 20f
                            }

                            classifier.setMode(ModelAPI.MODE_ALL)

                            val recognitions = classifier.recognizeImage(resource)

                            Log.d(TAG, "onResourceReady: size:" + recognitions.size)
                            for ((_, title, confidence, location) in recognitions) {
                                Log.d(TAG, "label = $title, confidence = $confidence")
                                canvas.drawRect(location, paint)
                            }
                            binding.imageView.setImageBitmap(drawBitmap)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
        }
    }

    private fun pickImage() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                                .setType("image/*")
                        startActivityForResult(Intent.createChooser(intent, getString(R.string.image_chooser)), PICK_IMAGE)
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        toast(getString(R.string.permission_denied, response.permissionName))
                    }

                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()
    }
}