package com.example.autofilemanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class VideosFile extends AppCompatActivity {

    // BACK BUTTON
    TextView backbtn;

    // CONTAINER WHERE VIDEO FILES WILL BE ADDED
    private LinearLayout video_Container;

    // TEXTVIEW FOR VIDEO COUNT
    private TextView videoCount;

    // TEXTVIEW FOR TOTAL VIDEO SIZE
    private TextView videoTotalSize;

    // TEXTVIEW FOR BOTTOM VIDEO COUNT
    private TextView videoFileCountBottom;

    // UNIQUE PERMISSION REQUEST CODE
    private static final int VIDEO_PERMISSION = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_videos_file);


        // CONNECT JAVA WITH XML
        backbtn = findViewById(R.id.BackButton);

        video_Container = findViewById(R.id.videoFileContainer);

        videoCount = findViewById(R.id.videoCount);

        videoTotalSize = findViewById(R.id.videoTotalSize);

        videoFileCountBottom = findViewById(R.id.videoFileCountBottom);


        // BACK TO MAIN DASHBOARD
        backbtn.setOnClickListener(view -> {

            Intent intent = new Intent(
                    VideosFile.this,
                    MainActivity.class
            );

            startActivity(intent);

            finish();
        });


        // CHECK VIDEO PERMISSION
        CheckPermission();
    }


    // ============================================================
    // CHECK WHETHER VIDEO PERMISSION IS GRANTED
    // ============================================================
    private void CheckPermission() {

        String permission;

        /*
         * ANDROID 13 / API 33+
         *
         * Android introduced separate permissions for
         * images and videos.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            permission = Manifest.permission.READ_MEDIA_VIDEO;

        } else {

            /*
             * Android 12 and below
             */
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }


        // CHECK WHETHER PERMISSION IS ALREADY GRANTED
        if (ContextCompat.checkSelfPermission(
                this,
                permission
        ) == PackageManager.PERMISSION_GRANTED) {

            // PERMISSION GRANTED
            loadVideoFile();

        } else {

            // PERMISSION NOT GRANTED
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{permission},
                    VIDEO_PERMISSION
            );
        }
    }


    // ============================================================
    // PERMISSION RESULT
    // ============================================================
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );


        // MAKE SURE THIS RESULT BELONGS TO OUR VIDEO PERMISSION
        if (requestCode == VIDEO_PERMISSION) {

            // CHECK WHETHER USER GRANTED PERMISSION
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // PERMISSION GRANTED
                loadVideoFile();

            } else {

                // PERMISSION DENIED
                Toast.makeText(
                        this,
                        "Video Permission Denied",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }


    // ============================================================
    // LOAD ALL VIDEO FILES FROM DEVICE
    // ============================================================
    private void loadVideoFile() {

        /*
         * IMPORTANT:
         *
         * We are using MediaStore.Video.Media
         * NOT MediaStore.Images.Media
         *
         * because this page is for VIDEOS.
         */
        String[] configurations = {

                // VIDEO FILE NAME
                MediaStore.Video.Media.DISPLAY_NAME,

                // VIDEO FILE SIZE
                MediaStore.Video.Media.SIZE,

                // VIDEO DURATION
                MediaStore.Video.Media.DURATION
        };


        // QUERY VIDEO FILES
        Cursor cursor = getContentResolver().query(

                /*
                 * VERY IMPORTANT
                 *
                 * This tells Android that we want
                 * VIDEO files.
                 */
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,

                // INFORMATION WE WANT
                configurations,

                // NO FILTER
                null,

                // NO SELECTION ARGUMENT
                null,

                // SORT VIDEOS BY NAME
                MediaStore.Video.Media.DISPLAY_NAME + " ASC"
        );


        // IF CURSOR IS NULL, STOP TO AVOID CRASH
        if (cursor == null) {

            Toast.makeText(
                    this,
                    "Unable to load videos",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // WHETHER AT LEAST ONE VIDEO WAS FOUND
        boolean hasVideo = false;


        // NUMBER OF VIDEOS
        int videoCounter = 0;


        // TOTAL SIZE OF ALL VIDEOS
        long totalVideoSize = 0;


        // MOVE THROUGH EVERY VIDEO
        while (cursor.moveToNext()) {

            hasVideo = true;

            videoCounter++;


            // ====================================================
            // GET VIDEO NAME
            // ====================================================
            String name = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Video.Media.DISPLAY_NAME
                    )
            );


            // ====================================================
            // GET VIDEO SIZE
            // ====================================================
            long size = cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Video.Media.SIZE
                    )
            );


            // ADD SIZE TO TOTAL
            totalVideoSize += size;


            // ====================================================
            // GET VIDEO DURATION
            // ====================================================
            long duration = cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Video.Media.DURATION
                    )
            );


            // CONVERT DURATION TO MINUTES AND SECONDS
            String formattedDuration = ConvertDuration(duration);


            // ====================================================
            // CREATE TEXTVIEW FOR VIDEO
            // ====================================================
            TextView videoText = new TextView(this);


            // ====================================================
            // DISPLAY VIDEO INFORMATION
            // ====================================================
            videoText.setText(
                    "▶  " + name
                            + "\nSize: " + ConvertSize(size)
                            + "\nDuration: " + formattedDuration
            );


            // TEXT SIZE
            videoText.setTextSize(16);


            // TEXT COLOR
            videoText.setTextColor(0xFF171717);


            // PADDING
            videoText.setPadding(
                    20,
                    25,
                    20,
                    25
            );


            // MAKE TEXTVIEW CLICKABLE
            videoText.setClickable(true);


            // ADD VIDEO TO CONTAINER
            video_Container.addView(videoText);


            // ====================================================
            // ADD SEPARATOR
            // ====================================================
            TextView separator = new TextView(this);

            separator.setBackgroundColor(0xFFEEEEEE);

            LinearLayout.LayoutParams separatorParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                    );

            video_Container.addView(
                    separator,
                    separatorParams
            );
        }


        // CLOSE CURSOR
        cursor.close();


        // ========================================================
        // UPDATE VIDEO COUNT
        // ========================================================
        videoCount.setText(
                videoCounter + " Files"
        );


        // ========================================================
        // UPDATE TOTAL SIZE
        // ========================================================
        videoTotalSize.setText(
                ConvertSize(totalVideoSize)
        );


        // ========================================================
        // UPDATE BOTTOM COUNT
        // ========================================================
        videoFileCountBottom.setText(
                videoCounter + " files"
        );


        // ========================================================
        // IF NO VIDEO FOUND
        // ========================================================
        if (!hasVideo) {

            showMessage();
        }
    }


    // ============================================================
    // DISPLAY MESSAGE WHEN NO VIDEO EXISTS
    // ============================================================
    private void showMessage() {

        TextView message = new TextView(this);


        message.setText(
                "No Video File Found!"
        );


        message.setTextSize(16);


        message.setTextColor(
                0xFF000000
        );


        message.setGravity(
                Gravity.CENTER
        );


        message.setPadding(
                20,
                50,
                20,
                50
        );


        // ADD MESSAGE TO VIDEO CONTAINER
        video_Container.addView(message);
    }


    // ============================================================
    // CONVERT BYTES INTO KB / MB / GB
    // ============================================================
    private String ConvertSize(long bytes) {

        // LESS THAN 1 KB
        if (bytes < 1024) {

            return bytes + " B";
        }


        // LESS THAN 1 MB
        if (bytes < 1024 * 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.2f KB",
                    bytes / 1024.0
            );
        }


        // LESS THAN 1 GB
        if (bytes < 1024L * 1024L * 1024L) {

            return String.format(
                    Locale.getDefault(),
                    "%.2f MB",
                    bytes / (1024.0 * 1024.0)
            );
        }


        // 1 GB OR MORE
        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
        );
    }


    // ============================================================
    // CONVERT VIDEO DURATION
    // ============================================================
    private String ConvertDuration(long durationMillis) {

        // CONVERT MILLISECONDS TO SECONDS
        long totalSeconds = durationMillis / 1000;


        // GET HOURS
        long hours = totalSeconds / 3600;


        // GET MINUTES
        long minutes = (totalSeconds % 3600) / 60;


        // GET SECONDS
        long seconds = totalSeconds % 60;


        // IF VIDEO IS MORE THAN 1 HOUR
        if (hours > 0) {

            return String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
            );
        }


        // NORMAL VIDEO
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds
        );
    }
}