package com.example.bandungzoochatbot;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bandungzoochatbot.ml.FinalModel;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class ClassifyActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> requestPermissionLauncher;
    Button camera, gallery;
    ImageView imageView;
    TextView result, facts_sub, description_sub;
    TextView facts1, facts2;
    TextView description;
    int imageSize = 224;

    Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify);

        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);

        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);
        facts_sub = findViewById(R.id.facts_sub);
        facts1 = findViewById(R.id.facts1);
        facts2 = findViewById(R.id.facts2);
        description_sub = findViewById(R.id.description_sub);
        description = findViewById(R.id.description);

        camera.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
        gallery.setOnClickListener(view -> {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });
    }

    private void openCamera() {
        try {
            File imageFile = File.createTempFile(
                    "IMG_",
                    ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            );

            imageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    imageFile
            );

            takePicture.launch(imageUri);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result && imageUri != null){
                        try {
                            Bitmap image = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            imageView.setImageBitmap(image);

                            imageView.setImageBitmap(image);
                            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                            classifyImage(image);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }else {
                        Toast.makeText(getApplicationContext(), "Gagal menambahkan foto", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void openGallery() {
        PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();
        imageCapture.launch(request);
    }

    ActivityResultLauncher<PickVisualMediaRequest> imageCapture = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null){
                        try {
                            Bitmap image = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            imageView.setImageBitmap(image);

                            imageView.setImageBitmap(image);
                            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                            classifyImage(image);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    public void classifyImage(Bitmap image) {
        try {
            long startTestTime = SystemClock.elapsedRealtime();
            FinalModel model = FinalModel.newInstance(getApplicationContext());

            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;

            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            FinalModel.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();

            int maxPos = 0;
            float maxConfidence = 0;
            String[] classes = {"ELAND", "JULANG SULAWESI", "KAMBING GUNUNG", "KANGKARENG PERUT PUTIH", "KASUARI", "KIJANG", "SITATUNGA", "WILDEBEEST BIRU"};
            for (int i = 0; i < confidences.length; i++) {
                Log.i("Checkpoint", classes[i]);
                Log.i("Checkscore", String.valueOf(confidences[i]));
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            long endTestTime = SystemClock.elapsedRealtime();
            long duration = endTestTime - startTestTime;

            Log.i("Classification Time", "Time taken for classification: " + duration + " ms");
            Log.i("classified", classes[maxPos]);
            Log.i("maxConf", String.valueOf(maxConfidence));

            AnimalsData newObj = new AnimalsData();

            String[] fact1 = newObj.getFact1();
            String[] fact2 = newObj.getFact2();
            String[] desc = newObj.getDescription();

            facts_sub.setText(R.string.facts_sub);
            description_sub.setText(R.string.description_sub);

            result.setText(classes[maxPos]);
            facts1.setText(fact1[maxPos]);
            facts2.setText(fact2[maxPos]);
            description.setText(desc[maxPos]);

            model.close();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}