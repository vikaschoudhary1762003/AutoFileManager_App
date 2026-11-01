package com.example.autofilemanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LargeFiles extends AppCompatActivity {

    // BACK BUTTON
    TextView backbtn;

    // CONTAINER WHERE LARGE FILES WILL BE DISPLAYED
    LinearLayout largeFileContainer;

    // SUMMARY TEXT
    TextView totalLargeFileSize;
    TextView totalLargeFileCount;

    // BOTTOM SELECTED FILES TEXT
    TextView selectedFileText;

    // DELETE BUTTON
    Button deleteButton;

    // FILES LARGER THAN 100 MB
    private static final long LARGE_FILE_SIZE =
            100L * 1024L * 1024L;

    // PERMISSION REQUEST CODE
    private static final int STORAGE_PERMISSION_CODE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_large_files);


        // ========================================================
        // CONNECT JAVA WITH XML
        // ========================================================

        backbtn = findViewById(R.id.BackButton);

        largeFileContainer = findViewById(R.id.largeFileContainer);

        totalLargeFileSize =
                findViewById(R.id.totalLargeFileSize);

        totalLargeFileCount =
                findViewById(R.id.totalLargeFileCount);

        selectedFileText =
                findViewById(R.id.selectedFileText);

        deleteButton =
                findViewById(R.id.deleteButton);


        // ========================================================
        // BACK TO HOME
        // ========================================================

        backbtn.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            LargeFiles.this,
                            MainActivity.class
                    );

            startActivity(intent);

            finish();
        });


        // ========================================================
        // DELETE BUTTON
        // ========================================================

        deleteButton.setOnClickListener(view -> {

            Toast.makeText(
                    LargeFiles.this,
                    "Select files first",
                    Toast.LENGTH_SHORT
            ).show();

        });


        // ========================================================
        // CHECK STORAGE PERMISSION
        // ========================================================

        checkStoragePermission();
    }


    // ============================================================
    // CHECK STORAGE PERMISSION
    // ============================================================

    private void checkStoragePermission() {

        // ANDROID 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            // Android 13 uses scoped storage.
            // READ_MEDIA_* permissions are for media,
            // but not for general documents/files.
            loadLargeFiles();

        }

        // ANDROID 10 - ANDROID 12
        else {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        STORAGE_PERMISSION_CODE
                );

            } else {

                loadLargeFiles();
            }
        }
    }


    // ============================================================
    // HANDLE PERMISSION RESULT
    // ============================================================

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


        if (requestCode == STORAGE_PERMISSION_CODE) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                loadLargeFiles();

            } else {

                Toast.makeText(
                        this,
                        "Storage permission denied",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }


    // ============================================================
    // LOAD LARGE FILES
    // ============================================================

    private void loadLargeFiles() {

        // CLEAR OLD FILES
        largeFileContainer.removeAllViews();


        // LIST TO STORE LARGE FILES
        List<File> largeFiles =
                new ArrayList<>();


        // GET MAIN SHARED STORAGE
        File storage =
                Environment.getExternalStorageDirectory();


        // FIND LARGE FILES
        scanDirectory(
                storage,
                largeFiles
        );


        // ========================================================
        // SORT FILES
        // LARGEST FILE FIRST
        // ========================================================

        Collections.sort(
                largeFiles,
                new Comparator<File>() {

                    @Override
                    public int compare(
                            File file1,
                            File file2) {

                        return Long.compare(
                                file2.length(),
                                file1.length()
                        );
                    }
                }
        );


        // ========================================================
        // CALCULATE TOTAL SIZE
        // ========================================================

        long totalSize = 0;


        for (File file : largeFiles) {

            totalSize =
                    totalSize + file.length();
        }


        // ========================================================
        // DISPLAY SUMMARY
        // ========================================================

        totalLargeFileSize.setText(
                convertSize(totalSize)
        );


        totalLargeFileCount.setText(
                largeFiles.size() + " Files Found"
        );


        // ========================================================
        // DISPLAY LARGE FILES
        // ========================================================

        if (largeFiles.isEmpty()) {

            showNoFilesMessage();

            return;
        }


        for (File file : largeFiles) {

            displayFile(file);
        }
    }


    // ============================================================
    // SCAN DIRECTORY
    // ============================================================

    private void scanDirectory(
            File directory,
            List<File> largeFiles) {

        // CHECK DIRECTORY
        if (directory == null
                || !directory.exists()
                || !directory.isDirectory()) {

            return;
        }


        File[] files;

        try {

            files = directory.listFiles();

        } catch (Exception e) {

            return;
        }


        // IF DIRECTORY CANNOT BE READ
        if (files == null) {

            return;
        }


        // ========================================================
        // LOOP THROUGH FILES
        // ========================================================

        for (File file : files) {

            try {

                // IF IT IS A DIRECTORY
                if (file.isDirectory()) {

                    // IGNORE SOME SYSTEM DIRECTORIES
                    String path =
                            file.getAbsolutePath();

                    if (path.contains(
                            "/Android/data"
                    )) {

                        continue;
                    }

                    if (path.contains(
                            "/Android/obb"
                    )) {

                        continue;
                    }


                    // SCAN SUBDIRECTORY
                    scanDirectory(
                            file,
                            largeFiles
                    );

                }

                // IF IT IS A FILE
                else {

                    // GET FILE SIZE
                    long fileSize =
                            file.length();


                    // CHECK WHETHER FILE IS
                    // LARGER THAN 100 MB
                    if (fileSize >= LARGE_FILE_SIZE) {

                        largeFiles.add(file);
                    }
                }

            } catch (Exception e) {

                // IGNORE FILES THAT CANNOT BE READ
                e.printStackTrace();
            }
        }
    }


    // ============================================================
    // DISPLAY ONE LARGE FILE
    // ============================================================

    private void displayFile(File file) {

        // MAIN FILE ROW
        LinearLayout fileRow =
                new LinearLayout(this);


        fileRow.setOrientation(
                LinearLayout.HORIZONTAL
        );


        fileRow.setGravity(
                Gravity.CENTER_VERTICAL
        );


        fileRow.setPadding(
                10,
                18,
                10,
                18
        );


        // ========================================================
        // FILE ICON
        // ========================================================

        TextView icon =
                new TextView(this);

        icon.setText("📁");

        icon.setTextSize(32);

        icon.setGravity(
                Gravity.CENTER
        );


        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        55,
                        70
                );


        fileRow.addView(
                icon,
                iconParams
        );


        // ========================================================
        // FILE INFORMATION
        // ========================================================

        LinearLayout information =
                new LinearLayout(this);


        information.setOrientation(
                LinearLayout.VERTICAL
        );


        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );


        // ========================================================
        // FILE NAME
        // ========================================================

        TextView fileName =
                new TextView(this);


        fileName.setText(
                file.getName()
        );


        fileName.setTextSize(
                16
        );


        fileName.setTextColor(
                0xFF111111
        );


        // ========================================================
        // FILE SIZE
        // ========================================================

        TextView fileSize =
                new TextView(this);


        fileSize.setText(
                convertSize(file.length())
        );


        fileSize.setTextSize(
                14
        );


        fileSize.setTextColor(
                0xFF444444
        );


        // ========================================================
        // FILE PATH
        // ========================================================

        TextView filePath =
                new TextView(this);


        filePath.setText(
                file.getAbsolutePath()
        );


        filePath.setTextSize(
                11
        );


        filePath.setTextColor(
                0xFF888888
        );


        information.addView(
                fileName
        );


        information.addView(
                fileSize
        );


        information.addView(
                filePath
        );


        fileRow.addView(
                information,
                infoParams
        );


        // ========================================================
        // ADD FILE TO SCREEN
        // ========================================================

        largeFileContainer.addView(
                fileRow
        );


        // ========================================================
        // SEPARATOR
        // ========================================================

        TextView separator =
                new TextView(this);


        separator.setBackgroundColor(
                0xFFEEEEEE
        );


        LinearLayout.LayoutParams separatorParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                );


        largeFileContainer.addView(
                separator,
                separatorParams
        );
    }


    // ============================================================
    // NO FILE MESSAGE
    // ============================================================

    private void showNoFilesMessage() {

        TextView message =
                new TextView(this);


        message.setText(
                "No files larger than 100 MB found"
        );


        message.setTextSize(
                16
        );


        message.setTextColor(
                0xFF555555
        );


        message.setGravity(
                Gravity.CENTER
        );


        message.setPadding(
                20,
                60,
                20,
                60
        );


        largeFileContainer.addView(
                message
        );
    }


    // ============================================================
    // CONVERT BYTES TO KB / MB / GB
    // ============================================================

    private String convertSize(long bytes) {

        if (bytes < 1024) {

            return bytes + " B";
        }


        if (bytes < 1024L * 1024L) {

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
                    bytes /
                            (1024.0 * 1024.0)
            );
        }


        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                bytes /
                        (1024.0 *
                                1024.0 *
                                1024.0)
        );
    }
}