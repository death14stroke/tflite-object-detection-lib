package com.andruid.magic.objectdetectionlib;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.andruid.magic.objectdetection.BitmapUtils;
import com.andruid.magic.objectdetection.Classifier;
import com.andruid.magic.objectdetection.ModelAPI;
import com.andruid.magic.objectdetection.Recognition;
import com.andruid.magic.objectdetectionlib.databinding.ActivityMainBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "mylog";
    private ActivityMainBinding binding;
    private Classifier classifier;
    private static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.fab.setOnClickListener(v -> pickImage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            classifier = ModelAPI.create(getAssets());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        classifier.close();
    }

    private void pickImage() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(getApplicationContext(), response.getPermissionName()+" denied", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==PICK_IMAGE && resultCode==RESULT_OK){
            if(data==null)
                return;
            Uri uri = data.getData();
            Glide.with(this)
                    .asBitmap()
                    .load(uri)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Bitmap bitmap = BitmapUtils.resize(resource);
                            Bitmap drawBitmap = resource.copy(Bitmap.Config.ARGB_8888, true);
                            Canvas canvas = new Canvas(drawBitmap);
                            Paint paint = new Paint();
                            paint.setColor(Color.RED);
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(20f);

                            classifier.setMode(ModelAPI.MODE_EXCLUDE);
                            List<String> list = new ArrayList<>();
                            list.add("person");
                            list.add("donut");
                            classifier.addSelectedLabels(list);
                            for(String s : classifier.getSelectedLabels())
                                Log.d(TAG, "onResourceReady: sellabels:"+s);
                            List<Recognition> recognitions = classifier.recognizeImage(bitmap);
                            Log.d(TAG, "onResourceReady: size:"+recognitions.size());
                            for(Recognition r : recognitions){
                                Log.d(TAG, "label:"+r.getTitle()+":conf:"+r.getConfidence());
                                canvas.drawRect(r.getLocation(), paint);
                            }
                            binding.imageView.setImageBitmap(drawBitmap);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }
    }
}