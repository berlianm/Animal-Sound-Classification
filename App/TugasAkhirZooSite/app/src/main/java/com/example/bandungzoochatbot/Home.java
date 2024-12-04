package com.example.bandungzoochatbot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class Home extends AppCompatActivity {

    private LinearLayout classifyContainer, mapsContainer, audioContainer, chatContainer;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Izin diberikan", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Izin ditolak", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize containers
        classifyContainer = findViewById(R.id.clasifyCameraContainer);
        mapsContainer = findViewById(R.id.mapsContainer);
        audioContainer = findViewById(R.id.audioClasifyContainer);
        chatContainer = findViewById(R.id.chatContainer);

        // Set click listeners for the containers
        classifyContainer.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, ClassifyActivity.class);
            startActivity(intent);
        });

        mapsContainer.setOnClickListener(v -> {
//            Intent intent = new Intent(Home.this, MapsActivity.class);
//            startActivity(intent);
            Toast.makeText(this, "Maps coming soon", Toast.LENGTH_SHORT).show();
        });

        audioContainer.setOnClickListener(v -> {
            // Uncomment when AudioClassifyActivity is ready
             Intent intent = new Intent(Home.this, AnimalSoundClassifier.class);
             startActivity(intent);
//            Toast.makeText(this, "Audio classification coming soon", Toast.LENGTH_SHORT).show();
        });

        chatContainer.setOnClickListener(v -> {
            // Uncomment when ChatBotActivity is ready
            // Intent intent = new Intent(Home.this, ChatBotActivity.class);
            // startActivity(intent);
            Toast.makeText(this, "Chat bot coming soon", Toast.LENGTH_SHORT).show();
        });
    }
}