package com.example.autofilemanager;

// ============================================================
// IMPORTS
// ============================================================

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;


// ============================================================
// MAIN ACTIVITY
// ============================================================

public class MainActivity extends AppCompatActivity {

    // ============================================================
    // QUICK ACTIONS
    // ============================================================

    private LinearLayout duplicateFiles;
    private LinearLayout largeFiles;
    private LinearLayout oldFiles;
    private LinearLayout unusedApps;
    private LinearLayout compressFiles;


    // ============================================================
    // FILE CATEGORY BUTTONS
    // ============================================================

    private LinearLayout downloadedFiles;
    private LinearLayout audioFiles;
    private LinearLayout imagesFiles;
    private LinearLayout videosFiles;
    private LinearLayout documentsFiles;
    private LinearLayout appsFiles;


    // ============================================================
    // STORAGE VIEWS
    // ============================================================

    private TextView storagePercentage;
    private TextView usedStorage;
    private TextView totalStorage;
    private TextView freeStorage;
    private TextView storageAlert;


    // STORAGE PROGRESS BAR
    private ProgressBar storageProgress;


    // ============================================================
    // MENU / DRAWER
    // ============================================================

    private DrawerLayout drawerLayout;
    private TextView menuButton;
    private TextView navScanStorage;
    private TextView navDuplicateFile;
    private TextView navLargeFile;
    private TextView navUnusedApps;

    // ============================================================
    // AUTO MANAGE
    // ============================================================

    // Auto Manage ON/OFF switch.
    private Switch autoManageSwitch;

    // SharedPreferences stores Auto Manage state.
    private SharedPreferences preferences;

    // Object of AutoManage.java.
    private AutoManage autoManage;


    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Connect Java with activity_main.xml.
        setContentView(R.layout.activity_main);


        // ========================================================
        // CONNECT QUICK ACTION IDs
        // ========================================================


        duplicateFiles = findViewById(R.id.duplicateFiles);

        largeFiles = findViewById(R.id.largeFiles);


        unusedApps = findViewById(R.id.unusedApps);



        // ========================================================
        // CONNECT FILE CATEGORY IDs
        // ========================================================

        downloadedFiles =
                findViewById(R.id.downloadedFiles);

        audioFiles =
                findViewById(R.id.audioFiles);

        imagesFiles =
                findViewById(R.id.imagesFiles);

        videosFiles =
                findViewById(R.id.videoFiles);

        documentsFiles =
                findViewById(R.id.documentFiles);

        appsFiles =
                findViewById(R.id.appsFiles);


        // ========================================================
        // CONNECT STORAGE VIEWS
        // ========================================================

        storagePercentage =
                findViewById(R.id.storagePercentage);

        usedStorage =
                findViewById(R.id.usedStorage);

        totalStorage =
                findViewById(R.id.totalStorage);

        freeStorage =
                findViewById(R.id.freeStorage);

        storageProgress =
                findViewById(R.id.storageProgress);

        storageAlert =
                findViewById(R.id.storageAlert);


        //CONNECT NAV MENU PAGES TO ACTUAL PAGES
        navScanStorage = findViewById(R.id.navScan);
        navDuplicateFile = findViewById(R.id.navDuplicate);
        navLargeFile = findViewById(R.id.navLarge);
        navUnusedApps = findViewById(R.id.navUnused);


        // ========================================================
        // CONNECT AUTO MANAGE SWITCH
        // ========================================================

        autoManageSwitch =
                findViewById(R.id.autoManageSwitch);


        // ========================================================
        // CONNECT DRAWER
        // ========================================================

        drawerLayout =
                findViewById(R.id.drawerLayout);

        menuButton =
                findViewById(R.id.menuButton);


        // ========================================================
        // CREATE AUTOMANAGE OBJECT
        // ========================================================

        /*
         * AutoManage.java contains:
         *
         * 1. Duplicate scanning
         * 2. Junk scanning
         * 3. Duplicate deletion
         * 4. Junk deletion
         */

        autoManage =
                new AutoManage(this);


        // CREATE / OPEN SHARED PREFERENCES

        preferences =
                getSharedPreferences(
                        "AutoFileManagerPrefs",
                        MODE_PRIVATE
                );


        // ========================================================
        // RESTORE PREVIOUS AUTO MANAGE STATE
        // ========================================================

        boolean autoManageEnabled =
                preferences.getBoolean(
                        "auto_manage",
                        false
                );


        /*
         * IMPORTANT:
         *
         * We only restore the switch here.
         *
         * We don't start scanning automatically here.
         */
        autoManageSwitch.setChecked(
                autoManageEnabled
        );


        // ========================================================
        // AUTO MANAGE SWITCH
        // ========================================================

        autoManageSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    // =================================================
                    // USER TURNED AUTO MANAGE ON
                    // =================================================

                    if (isChecked) {

                        /*
                         * Do NOT immediately save TRUE.
                         *
                         * First ask the user for confirmation.
                         */
                        showAutoManagePermissionDialog();

                    }


                    // =================================================
                    // USER TURNED AUTO MANAGE OFF
                    // =================================================

                    else {

                        /*
                         * Save OFF state.
                         */
                        preferences.edit()
                                .putBoolean(
                                        "auto_manage",
                                        false
                                )
                                .apply();


                        Toast.makeText(
                                MainActivity.this,
                                "Auto Manage Disabled",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );


        // ========================================================
        // LOAD REAL DEVICE STORAGE
        // ========================================================

        loadStorageData();

        // DUPLICATE FILES BUTTON

        duplicateFiles.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            DuplicateFiles.class
                    );

            startActivity(intent);
        });


        // ========================================================
        // LARGE FILES BUTTON
        // ========================================================

        largeFiles.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            LargeFiles.class
                    );

            startActivity(intent);
        });





        // ========================================================
        // UNUSED APPS BUTTON
        // ========================================================

        unusedApps.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            UnusedApp.class
                    );

            startActivity(intent);
        });




        // ========================================================
        // DOWNLOADED FILES
        // ========================================================

        downloadedFiles.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            DownloadFiles.class
                    );

            startActivity(intent);
        });


        // ========================================================
        // AUDIO FILES
        // ========================================================

        audioFiles.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            AudioFiles.class
                    );

            startActivity(intent);
        });


        // ========================================================
        // IMAGE FILES
        // ========================================================

        imagesFiles.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            ImagesFile.class
                    );

            startActivity(intent);
        });


        // ========================================================
        // VIDEO FILES
        // ========================================================

        videosFiles.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            VideosFile.class
                    );

            startActivity(intent);
        });


        // ========================================================
        // DOCUMENT FILES
        // ========================================================

        documentsFiles.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            DocumentFiles.class
                    );

            startActivity(intent);
        });


        // ========================================================
        // APPS FILES
        // ========================================================

        appsFiles.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            AppsFile.class
                    );

            startActivity(intent);
        });


        // ========================================================
        // OPEN NAVIGATION DRAWER
        // ========================================================

        menuButton.setOnClickListener(v -> {

            drawerLayout.openDrawer(
                    GravityCompat.START
            );
        });

        //NAV MENU PAGES HANDLER
        navScanStorage.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this , ScanStorage.class);
            startActivity(intent);
        });

        navDuplicateFile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DuplicateFiles.class);
            startActivity(intent);
        });

        navLargeFile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LargeFiles.class);
            startActivity(intent);
        });

        navUnusedApps.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UnusedApp.class);
            startActivity(intent);
        });

    }



    // ============================================================
    // AUTO MANAGE CONFIRMATION DIALOG
    // ============================================================

    private void showAutoManagePermissionDialog() {

        new AlertDialog.Builder(
                MainActivity.this
        )

                // Dialog title.
                .setTitle(
                        "Enable Auto Manage"
                )

                // Explanation.
                .setMessage(
                        "By enabling Auto Manage, AutoFileManager " +
                                "will scan your device storage and automatically " +
                                "remove detected duplicate files and safe junk " +
                                "files such as temporary files.\n\n" +

                                "Your normal photos, videos, documents and " +
                                "other personal files will not be deleted " +
                                "unless they are detected as duplicates " +
                                "according to the duplicate detection rules.\n\n" +

                                "Do you want to allow Auto Manage?"
                )

                // =================================================
                // ALLOW
                // =================================================

                .setPositiveButton(
                        "Allow",
                        (dialog, which) -> {

                            /*
                             * Mark that we are waiting for the user
                             * to return from the Android permission screen.
                             */
                            preferences.edit()
                                    .putBoolean(
                                            "waiting_for_auto_manage_permission",
                                            true
                                    )
                                    .apply();

                            /*
                             * Open storage permission.
                             */
                            requestAutoManagePermission();
                        }
                )

                // =================================================
                // DECLINE
                // =================================================

                .setNegativeButton(
                        "Decline",
                        (dialog, which) -> {

                            /*
                             * Turn switch OFF.
                             */
                            autoManageSwitch.setChecked(false);


                            /*
                             * Save OFF.
                             */
                            preferences.edit()
                                    .putBoolean(
                                            "auto_manage",
                                            false
                                    )
                                    .apply();


                            Toast.makeText(
                                    MainActivity.this,
                                    "Auto Manage Not Enabled",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )

                // User must choose Allow or Decline.
                .setCancelable(false)

                .show();
    }


    // ============================================================
    // REQUEST AUTO MANAGE STORAGE PERMISSION
    // ============================================================

    private void requestAutoManagePermission() {

        /*
         * Android 11 / API 30 and above.
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R) {


            /*
             * Check whether All Files Access is already granted.
             */
            if (Environment.isExternalStorageManager()) {

                /*
                 * Permission already exists.
                 */
                enableAutoManage();

            } else {

                /*
                 * Open Android's special permission screen.
                 */
                try {

                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                            );


                    /*
                     * Tell Android which application needs
                     * the permission.
                     */
                    intent.setData(
                            Uri.parse(
                                    "package:"
                                            + getPackageName()
                            )
                    );


                    startActivity(intent);


                } catch (Exception e) {

                    /*
                     * Fallback for devices that don't support
                     * the application-specific page.
                     */
                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            );

                    startActivity(intent);
                }
            }


        } else {

            /*
             * Android versions older than Android 11.
             */
            enableAutoManage();
        }
    }


    // ============================================================
    // ENABLE AUTO MANAGE AFTER PERMISSION
    // ============================================================

    private void enableAutoManage() {

        /*
         * Save Auto Manage as enabled.
         */
        preferences.edit()
                .putBoolean(
                        "auto_manage",
                        true
                )
                .putBoolean(
                        "waiting_for_auto_manage_permission",
                        false
                )
                .apply();


        /*
         * Make sure switch is ON.
         */
        autoManageSwitch.setChecked(true);


        Toast.makeText(
                MainActivity.this,
                "Auto Manage Enabled. Scanning storage...",
                Toast.LENGTH_SHORT
        ).show();


        /*
         * Start Auto Manage.
         *
         * AutoManage.java will:
         *
         * 1. Scan duplicates.
         * 2. Scan safe junk.
         * 3. Delete duplicates.
         * 4. Delete junk.
         */
        autoManage.startAutoScan();
    }


    // ============================================================
    // ON RESUME
    // ============================================================

    @Override
    protected void onResume() {

        super.onResume();


        /*
         * Android 11+ uses the special "All Files Access"
         * settings screen.
         *
         * We check the result when the user comes back.
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R) {


            /*
             * Did we send the user to the permission screen?
             */
            boolean waitingForPermission =
                    preferences.getBoolean(
                            "waiting_for_auto_manage_permission",
                            false
                    );


            if (waitingForPermission) {


                /*
                 * User has returned from Android settings.
                 *
                 * Now check whether permission was granted.
                 */
                if (Environment.isExternalStorageManager()) {

                    // Permission granted.
                    enableAutoManage();

                } else {

                    // Permission denied.
                    preferences.edit()
                            .putBoolean(
                                    "waiting_for_auto_manage_permission",
                                    false
                            )
                            .putBoolean(
                                    "auto_manage",
                                    false
                            )
                            .apply();


                    /*
                     * Turn switch OFF.
                     */
                    autoManageSwitch.setChecked(false);


                    Toast.makeText(
                            MainActivity.this,
                            "Storage permission denied. Auto Manage remains disabled.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        }
    }


    // ============================================================
    // LOAD REAL DEVICE STORAGE
    // ============================================================

    private void loadStorageData() {

        try {

            /*
             * Get primary device data storage.
             */
            StatFs statFs =
                    new StatFs(
                            Environment
                                    .getDataDirectory()
                                    .getPath()
                    );


            // Block size.
            long blockSize =
                    statFs.getBlockSizeLong();


            // Total blocks.
            long totalBlocks =
                    statFs.getBlockCountLong();


            // Available blocks.
            long availableBlocks =
                    statFs.getAvailableBlocksLong();


            // ====================================================
            // CALCULATE BYTES
            // ====================================================

            long totalBytes =
                    totalBlocks * blockSize;


            long freeBytes =
                    availableBlocks * blockSize;


            long usedBytes =
                    totalBytes - freeBytes;


            // ====================================================
            // CALCULATE PERCENTAGE
            // ====================================================

            int percentage = 0;


            if (totalBytes > 0) {

                percentage =
                        (int) (
                                (usedBytes * 100L)
                                        / totalBytes
                        );
            }


            // ====================================================
            // CONVERT TO GB
            // ====================================================

            double totalGB =
                    totalBytes
                            / (
                            1024.0
                                    * 1024.0
                                    * 1024.0
                    );


            double freeGB =
                    freeBytes
                            / (
                            1024.0
                                    * 1024.0
                                    * 1024.0
                    );


            double usedGB =
                    usedBytes
                            / (
                            1024.0
                                    * 1024.0
                                    * 1024.0
                    );


            // ====================================================
            // UPDATE UI
            // ====================================================

            storagePercentage.setText(
                    percentage + "%"
            );


            usedStorage.setText(
                    String.format(
                            "%.1f GB",
                            usedGB
                    )
            );


            totalStorage.setText(
                    String.format(
                            "%.1f GB",
                            totalGB
                    )
            );


            freeStorage.setText(
                    String.format(
                            "%.1f GB",
                            freeGB
                    )
            );


            storageProgress.setProgress(
                    percentage
            );


            // Check storage health.
            checkStorageHealth(
                    percentage
            );


        } catch (Exception e) {

            e.printStackTrace();


            storagePercentage.setText("--");

            usedStorage.setText("--");

            totalStorage.setText("--");

            freeStorage.setText("--");

            storageProgress.setProgress(0);
        }
    }


    // ============================================================
    // CHECK STORAGE HEALTH
    // ============================================================

    private void checkStorageHealth(
            int usagePercent
    ) {

        String status;

        String message;


        // ========================================================
        // LESS THAN 55%
        // ========================================================

        if (usagePercent < 55) {

            status = "Healthy";

            message =
                    "Your Storage is Healthy !";
        }


        // ========================================================
        // 55% - 64%
        // ========================================================

        else if (usagePercent < 65) {

            status = "Good";

            message =
                    "Storage Usage is Increasing !";
        }


        // ========================================================
        // 65% - 84%
        // ========================================================

        else if (usagePercent < 85) {

            status = "Warning";

            message =
                    "Your Storage is Getting Low !";
        }


        // ========================================================
        // 85% - 94%
        // ========================================================

        else if (usagePercent < 95) {

            status = "Critical";

            message =
                    "Storage is Almost Full, Cleanup Recommended !";
        }


        // ========================================================
        // 95% OR MORE
        // ========================================================

        else {

            status = "Almost Full";

            message =
                    "Storage is Almost Full. Free up Space Now !";
        }


        // Display message.
        storageAlert.setText(
                message
        );
    }
}