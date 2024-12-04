package com.example.bandungzoochatbot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bandungzoochatbot.assets.DataAnimal;
import com.example.bandungzoochatbot.assets.DataAnimalSoundClassification;

import java.util.ArrayList;


public class SoundClassificationResults extends AppCompatActivity {
    private Button homeButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_classification_results);

        initializeViews();
        setupClickListeners();

        String animalName = getIntent().getStringExtra("animal_name");
//        String animalName = "Elang";

        DataAnimal selectedAnimal = findAnimalByName(animalName);

        if (selectedAnimal != null) {
            ImageView animalImage = findViewById(R.id.animalImageView);
            TextView animalNameView = findViewById(R.id.animalTypeTextView);
            TextView animalDesc = findViewById(R.id.animalDescriptionTextView);
            TextView animalFact = findViewById(R.id.animalFactTextView);

            animalImage.setImageResource(selectedAnimal.getImageResId());
            animalNameView.setText(selectedAnimal.getName());
            animalDesc.setText(selectedAnimal.getDescription());
            animalFact.setText(selectedAnimal.getUniqueFact());
        } else {
            ImageView animalImage = findViewById(R.id.animalImageView);
            TextView animalNameView = findViewById(R.id.animalTypeTextView);
            TextView animalDesc = findViewById(R.id.animalDescriptionTextView);
            TextView animalFact = findViewById(R.id.animalFactTextView);

            animalNameView.setText("Hewan tidak ditemukan");
            animalDesc.setText("Deskripsi tidak tersedia");
            animalFact.setText("Fakta unik tidak tersedia");
            animalImage.setImageResource(R.drawable.default_image);// Handle case if animal is not found
        }
    }
    private void initializeViews() {
        homeButton = findViewById(R.id.backHome);
        backButton = findViewById(R.id.backClassify);
    }

    private void setupClickListeners() {
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, Home.class);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnimalSoundClassifier.class);
            startActivity(intent);
        });

    }

    private DataAnimal findAnimalByName(String name) {
        ArrayList<DataAnimal> animals = DataAnimalSoundClassification.getAnimalList();
        for (DataAnimal animal : animals) {
            if (animal.getName().equals(name)) {
                return animal;
            } else {
//                Toast.makeText(this, "data namanya gada yang sama jir", Toast.LENGTH_SHORT).show();
//                Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
            }
        }
        return null;
    }
}
