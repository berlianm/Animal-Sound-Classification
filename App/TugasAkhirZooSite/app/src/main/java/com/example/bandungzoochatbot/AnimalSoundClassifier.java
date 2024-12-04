package com.example.bandungzoochatbot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bandungzoochatbot.ml.ModelSoundclassification;
import com.masoudss.lib.WaveformSeekBar;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public class AnimalSoundClassifier extends AppCompatActivity {
    private static final int PICK_AUDIO_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;

    private WaveformSeekBar waveformSeekBar;
    private ImageButton galleryButton, recordButton, doneButton, playButton;
    private TextView timerTextView, statusTextView, resultTextView;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private Uri recordedAudioUri;
    private String audioFilePath;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private Handler timerHandler;
    private long startTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_sound_classifier);

        initializeViews();
        setupClickListeners();
        checkPermissions();

        timerHandler = new Handler();
    }

    private void initializeViews() {
        waveformSeekBar = findViewById(R.id.waveformSeekBar);
        galleryButton = findViewById(R.id.galleryButton);
        recordButton = findViewById(R.id.recordButton);
        doneButton = findViewById(R.id.doneButton);
        playButton = findViewById(R.id.playButton);
        timerTextView = findViewById(R.id.timerTextView);
        statusTextView = findViewById(R.id.statusTextView);
        resultTextView = findViewById(R.id.resultTextView);

        waveformSeekBar.setSampleFrom(new int[]{0}); // Initialize with empty data
    }

    private void setupClickListeners() {
        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        galleryButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            startActivityForResult(intent, PICK_AUDIO_REQUEST);
        });

        doneButton.setOnClickListener(v -> {
            if (recordedAudioUri != null) {
                classifyAudio(recordedAudioUri);
            } else {
                Toast.makeText(this, "Tidak ada audio untuk diklasifikasikan", Toast.LENGTH_SHORT).show();
            }
        });

        playButton.setOnClickListener(v -> {
            if (isPlaying) {
                stopPlaying();
            } else {
                startPlaying();
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void startRecording() {
        try {
            // Inisialisasi MediaRecorder
            File outputFile = new File(getExternalCacheDir(), "audio_record.mp3");
            audioFilePath = outputFile.getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFilePath);

            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncodingBitRate(128000);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            recordButton.setImageResource(R.drawable.ic_stop);

            // Mulai timer saat rekaman dimulai
            startTimer();

            new Handler().postDelayed(this::startWaveformUpdates, 500);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error starting recording: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            stopRecording();
        }
    }

    private void stopRecording() {
        isRecording = false;
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                recordedAudioUri = Uri.fromFile(new File(audioFilePath)); // Simpan URI dari file rekaman
            }
        }
        recordButton.setImageResource(R.drawable.ic_mic);
        stopTimer();
    }

    private void startTimer() {
        startTime = SystemClock.uptimeMillis(); // Setel ulang startTime
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecording || isPlaying) { // Timer hanya berjalan jika sedang merekam atau memutar
                    long timeInMillis = SystemClock.uptimeMillis() - startTime;
                    int seconds = (int) (timeInMillis / 1000);
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    int milliseconds = (int) (timeInMillis % 1000) / 100;

                    timerTextView.setText(String.format(Locale.getDefault(),
                            "%02d:%02d.%d", minutes, seconds, milliseconds));

                    timerHandler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    private void stopTimer() {
        timerHandler.removeCallbacksAndMessages(null); // Hentikan timer
        timerTextView.setText("00:00.0"); // Reset tampilan waktu
    }


    private void startPlaying() {
        if (mediaPlayer != null) {
            stopPlaying(); // Hentikan instance sebelumnya jika ada
        }

        if (recordedAudioUri != null) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(this, recordedAudioUri);
                mediaPlayer.prepare();
                mediaPlayer.start();
                isPlaying = true;
                playButton.setImageResource(R.drawable.ic_pause);

                // Mulai timer saat pemutaran dimulai
                startTimer();

                mediaPlayer.setOnCompletionListener(mp -> stopPlaying());

                // Update waveform as the audio plays
                new Thread(() -> {
                    while (isPlaying) {
                        try {
                            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                int currentPosition = mediaPlayer.getCurrentPosition();
                                int[] samples = generateWaveformSamples(currentPosition, mediaPlayer.getDuration());
                                runOnUiThread(() -> waveformSeekBar.setSampleFrom(samples));
                            }
                            Thread.sleep(100);
                        } catch (IllegalStateException | InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error playing audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No recording to play", Toast.LENGTH_SHORT).show();
        }
    }


    private void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false; // Set isPlaying ke false agar thread berhenti
            playButton.setImageResource(R.drawable.ic_play);
        }
    }

    private void startWaveformUpdates() {
        new Thread(() -> {
            while (isRecording) {
                if (mediaRecorder != null) {
                    try {
                        int amplitude = mediaRecorder.getMaxAmplitude();
                        final int normalizedAmplitude = Math.min(amplitude / 100, 100);

                        runOnUiThread(() -> {
                            try {
                                int[] samples = new int[]{normalizedAmplitude};
                                waveformSeekBar.setSampleFrom(samples);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });

                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }).start();
    }

    private int[] generateWaveformSamples(int currentPosition, int duration) {
        int sampleCount = 100;
        int[] samples = new int[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            int samplePosition = (int) ((float) i / sampleCount * duration);
            if (samplePosition <= currentPosition && mediaPlayer != null && mediaPlayer.isPlaying()) {
                samples[i] = (int) (Math.random() * 100); // Replace with actual waveform data
            } else {
                samples[i] = 0;
            }
        }
        return samples;
    }

    private String getPathFromUri(Uri uri) {
        String filePath = null;
        try {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = {MediaStore.Audio.Media.DATA};
                try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                        filePath = cursor.getString(columnIndex);
                    }
                }
            }
            if (filePath == null) {
                filePath = uri.getPath();
                // Handle case for files from external storage
                if (filePath != null && filePath.startsWith("/external/")) {
                    filePath = getExternalFilesDir(null) + "/" +
                            filePath.substring(filePath.lastIndexOf("/") + 1);
                }
            }
        } catch (Exception e) {
            Log.e("AudioClasify", "Error getting file path: " + e.getMessage());
            e.printStackTrace();
        }
        return filePath;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedAudioUri = data.getData();
            if (selectedAudioUri != null) {
                recordedAudioUri = selectedAudioUri;
                audioFilePath = getPathFromUri(selectedAudioUri);

                if (statusTextView != null) {
                    statusTextView.setText("Audio dipilih dari galeri. Tekan 'Done' untuk mengklasifikasi.");
                }
                if (playButton != null) {
                    playButton.setEnabled(true);
                }
                if (resultTextView != null) {
                    resultTextView.setText(""); // Clear the result text
                }

                loadAudioFromUri(selectedAudioUri);
            } else {
                Toast.makeText(this, "Tidak dapat membaca file audio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadAudioFromUri(Uri uri) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepare();

            int duration = mediaPlayer.getDuration();
            startCountdownTimer(duration); // Start the countdown timer with the audio duration
            generateSimpleWaveform(duration);

            playButton.setEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void startCountdownTimer(int durationInMillis) {
        timerHandler.removeCallbacksAndMessages(null);
        startTime = SystemClock.uptimeMillis() + durationInMillis;

        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long timeRemaining = startTime - SystemClock.uptimeMillis();
                if (timerTextView != null) {
                    timerTextView.setText("00:00.0"); // Countdown complete
                }
//                if (timeRemaining > 0) {
//                    int seconds = (int) (timeRemaining / 1000);
//                    int minutes = seconds / 60;
//                    seconds = seconds % 60;
//                    int milliseconds = (int) (timeRemaining % 1000) / 100;
//
//                    if (timerTextView != null) {
//                        timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, milliseconds));
//                    }
//
//                    timerHandler.postDelayed(this, 100);
//                } else {
//                    if (timerTextView != null) {
//                        timerTextView.setText("00:00.0"); // Countdown complete
//                    }
//                }
            }
        }, 100);
    }

    private void generateSimpleWaveform(int duration) {
        int sampleCount = 100;
        int[] samples = new int[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            samples[i] = (int) (Math.random() * 100);
        }

        waveformSeekBar.setSampleFrom(samples);
    }


    private void classifyAudio(Uri audioUri) {
        try {
            ModelSoundclassification model = ModelSoundclassification.newInstance(this);
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 1024}, DataType.FLOAT32);

            float[] audioData = loadAndPreprocessAudioFromUri(audioUri);

            if (audioData != null && audioData.length >= 1024) {
                float[] processedAudioData = new float[1024];
                System.arraycopy(audioData, 0, processedAudioData, 0, 1024);

                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024 * 4).order(ByteOrder.nativeOrder());
                for (float value : processedAudioData) {
                    byteBuffer.putFloat(value);
                }
                inputFeature0.loadBuffer(byteBuffer);

                ModelSoundclassification.Outputs outputs = model.process(inputFeature0);
                TensorBuffer scoresBuffer = outputs.getOutputFeature0AsTensorBuffer();
                float[] scores = scoresBuffer.getFloatArray();

                if (scores != null && scores.length > 0) {
                    int maxIdx = 0;
                    float maxScore = scores[0];
                    for (int i = 1; i < scores.length; i++) {
                        if (scores[i] > maxScore) {
                            maxIdx = i;
                            maxScore = scores[i];
                        }
                    }

                    final String classifiedAnimalName = getAnimalLabel(maxIdx);

                    Intent intent = new Intent(this, SoundClassificationResults.class);
                    intent.putExtra("animal_name", classifiedAnimalName);
                    stopPlaying();
                    startActivity(intent);
                }

                model.close();
            } else {
                Toast.makeText(this, "Data audio tidak valid untuk klasifikasi.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error dalam klasifikasi audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private float[] loadAndPreprocessAudioFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }

            byte[] audioBytes = byteArrayOutputStream.toByteArray();
            int sampleCount = Math.min(audioBytes.length / 2, 1024);
            float[] floatData = new float[sampleCount];

            ByteBuffer byteBuffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < sampleCount; i++) {
                floatData[i] = byteBuffer.getShort() / 32768.0f;
            }

            return normalizeAudio(floatData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private float[] normalizeAudio(float[] audioData) {
        if (audioData == null || audioData.length == 0) return new float[0];
        float maxAbs = 0;
        for (float value : audioData) maxAbs = Math.max(maxAbs, Math.abs(value));
        if (maxAbs > 0) {
            for (int i = 0; i < audioData.length; i++) {
                audioData[i] = audioData[i] / maxAbs;
            }
        }
        return audioData;
    }

    private String getAnimalLabel(int index) {

        String[] labels = {
                "Elang", "Singa", "Gajah", "Kuda", "Monyet", "Beruang", "Cendrawasih",
                "Sapi", "Lumba-Lumba", "Keledai", "Keledai", "Kasuari", "Katak", "Domba", "Serigala"
        };

        if (index >= 0 && index < labels.length) {
            return labels[index];
        }
        return "Unknown Sound";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        isRecording = false;

        // Hentikan semua Handler dan Runnable
        stopTimer();
        timerHandler.removeCallbacksAndMessages(null);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}