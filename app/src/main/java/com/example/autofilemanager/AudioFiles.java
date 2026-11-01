package com.example.autofilemanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class AudioFiles extends AppCompatActivity {

    // BACK BUTTON
    private TextView backbtn;

    // CONTAINER WHERE AUDIO FILES WILL BE DISPLAYED
    private LinearLayout audioContainer;

    // HEADER TEXTVIEWS
    private TextView audioCount;
    private TextView audioTotalSize;
    private TextView audioFileCountBottom;

    // PERMISSION REQUEST CODE
    private static final int AUDIO_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_audio_files);

        // FIND VIEWS FROM XML
        backbtn = findViewById(R.id.BackButton);
        audioContainer = findViewById(R.id.audioFileContainer);

        audioCount = findViewById(R.id.audioCount);
        audioTotalSize = findViewById(R.id.audioTotalSize);
        audioFileCountBottom = findViewById(R.id.audioFileCountBottom);

        // BACK BUTTON
        backbtn.setOnClickListener(view -> {

            Intent intent = new Intent(AudioFiles.this, MainActivity.class);

            startActivity(intent);

            finish();
        });

        // CHECK AUDIO PERMISSION
        checkAudioPermission();
    }

    // =========================================================
    // CHECK AUDIO PERMISSION
    // =========================================================
    private void checkAudioPermission() {

        String permission;

        /*
         * ANDROID 13 (API 33) AND ABOVE
         * USE READ_MEDIA_AUDIO
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            permission = Manifest.permission.READ_MEDIA_AUDIO;

        } else {

            /*
             * ANDROID 12 AND BELOW
             * USE READ_EXTERNAL_STORAGE
             */
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        // CHECK WHETHER PERMISSION IS ALREADY GRANTED
        if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED) {

            // PERMISSION GRANTED
            loadAudioFiles();

        } else {

            // REQUEST PERMISSION
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{permission},
                    AUDIO_PERMISSION
            );
        }
    }

    // =========================================================
    // PERMISSION RESULT
    // =========================================================
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        // MAKE SURE THIS RESULT BELONGS TO OUR AUDIO REQUEST
        if (requestCode == AUDIO_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // PERMISSION GRANTED
                loadAudioFiles();

            } else {

                // PERMISSION DENIED
                Toast.makeText(
                        this,
                        "Audio permission is required to display audio files.",
                        Toast.LENGTH_LONG
                ).show();

                showNoAudioMessage();
            }
        }
    }

    // =========================================================
    // LOAD AUDIO FILES
    // =========================================================
    private void loadAudioFiles() {

        // CLEAR OLD DATA BEFORE LOADING
        audioContainer.removeAllViews();

        // VARIABLES FOR SUMMARY
        int audioFileCount = 0;
        long totalAudioSize = 0;

        /*
         * COLUMNS WE WANT FROM MEDIastore
         */
        String[] projection = {

                // AUDIO FILE NAME
                MediaStore.Audio.Media.DISPLAY_NAME,

                // AUDIO FILE SIZE
                MediaStore.Audio.Media.SIZE,

                // AUDIO DURATION
                MediaStore.Audio.Media.DURATION
        };

        Cursor cursor = null;

        try {

            /*
             * QUERY ANDROID'S MEDIASTORE
             *
             * EXTERNAL_CONTENT_URI means audio stored
             * in the phone's shared/external storage.
             */
            cursor = getContentResolver().query(

                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,

                    projection,

                    null,

                    null,

                    // SORT BY FILE NAME
                    MediaStore.Audio.Media.DISPLAY_NAME + " ASC"
            );

            // IF CURSOR IS NULL
            if (cursor == null) {

                showNoAudioMessage();

                return;
            }

            // READ EVERY AUDIO FILE
            while (cursor.moveToNext()) {

                // GET FILE NAME
                int nameIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.DISPLAY_NAME
                );

                // GET FILE SIZE
                int sizeIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.SIZE
                );

                // GET DURATION
                int durationIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.DURATION
                );

                // SAFETY CHECK
                if (nameIndex == -1) {
                    continue;
                }

                String audioName = cursor.getString(nameIndex);

                long audioSize = 0;

                long audioDuration = 0;

                if (sizeIndex != -1) {
                    audioSize = cursor.getLong(sizeIndex);
                }

                if (durationIndex != -1) {
                    audioDuration = cursor.getLong(durationIndex);
                }

                // UPDATE SUMMARY
                audioFileCount++;

                totalAudioSize += audioSize;

                // CREATE AUDIO ROW
                TextView audioText = new TextView(this);

                /*
                 * DISPLAY:
                 * AUDIO NAME
                 * SIZE
                 * DURATION
                 */
                audioText.setText(
                        "🎵 " + audioName
                                + "\nSize: " + convertSize(audioSize)
                                + "\nDuration: " + convertDuration(audioDuration)
                );

                audioText.setTextSize(16);

                audioText.setTextColor(0xFF000000);

                audioText.setGravity(Gravity.CENTER_VERTICAL);

                audioText.setPadding(
                        30,
                        25,
                        30,
                        25
                );

                // MAKE ROW FILL WIDTH
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                params.setMargins(
                        10,
                        5,
                        10,
                        5
                );

                audioText.setLayoutParams(params);

                // ADD AUDIO ROW TO CONTAINER
                audioContainer.addView(audioText);
            }

        } catch (Exception e) {

            // SHOW ERROR IF SOMETHING GOES WRONG
            Toast.makeText(
                    this,
                    "Unable to load audio files: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

        } finally {

            // ALWAYS CLOSE CURSOR
            if (cursor != null) {
                cursor.close();
            }
        }

        // UPDATE HEADER
        updateAudioSummary(
                audioFileCount,
                totalAudioSize
        );

        // IF NO AUDIO FILE WAS FOUND
        if (audioFileCount == 0) {

            showNoAudioMessage();
        }
    }

    // =========================================================
    // UPDATE AUDIO SUMMARY
    // =========================================================
    private void updateAudioSummary(
            int count,
            long totalSize) {

        // TOP LEFT COUNT
        audioCount.setText(
                count + (count == 1 ? " File" : " Files")
        );

        // TOTAL SIZE
        audioTotalSize.setText(
                convertSize(totalSize)
        );

        // BOTTOM COUNT
        audioFileCountBottom.setText(
                count + (count == 1 ? " file" : " files")
        );
    }

    // =========================================================
    // SHOW NO AUDIO MESSAGE
    // =========================================================
    private void showNoAudioMessage() {

        // CLEAR EXISTING CONTENT
        audioContainer.removeAllViews();

        TextView message = new TextView(this);

        message.setText("No audio files found");

        message.setTextSize(16);

        message.setTextColor(0xFF000000);

        message.setGravity(Gravity.CENTER);

        message.setPadding(
                20,
                80,
                20,
                80
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        message.setLayoutParams(params);

        audioContainer.addView(message);

        // UPDATE SUMMARY
        audioCount.setText("0 Files");

        audioTotalSize.setText("0 B");

        audioFileCountBottom.setText("0 files");
    }

    // =========================================================
    // CONVERT BYTES TO KB / MB / GB
    // =========================================================
    private String convertSize(long bytes) {

        if (bytes < 1024) {

            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.2f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {

            return String.format(
                    Locale.getDefault(),
                    "%.2f MB",
                    bytes / (1024.0 * 1024.0)
            );
        }

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
        );
    }

    // =========================================================
    // CONVERT MILLISECONDS TO MINUTES : SECONDS
    // =========================================================
    private String convertDuration(long milliseconds) {

        long totalSeconds = milliseconds / 1000;

        long minutes = totalSeconds / 60;

        long seconds = totalSeconds % 60;

        return String.format(
                Locale.getDefault(),
                "%02d : %02d",
                minutes,
                seconds
        );
    }
}