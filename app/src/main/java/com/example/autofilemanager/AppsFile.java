package com.example.autofilemanager;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class AppsFile extends AppCompatActivity {

    TextView backbtn;

    private LinearLayout appContainer;
    private TextView appCount;
    private TextView appTotalSize;
    private TextView appCountBottom;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_apps_file);


        // CONNECT XML
        backbtn = findViewById(R.id.BackButton);
        appContainer = findViewById(R.id.appContainer);
        appCount = findViewById(R.id.appCount);
        appTotalSize = findViewById(R.id.appTotalSize);
        appCountBottom = findViewById(R.id.appCountBottom);


        // BACK BUTTON
        backbtn.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            AppsFile.this,
                            MainActivity.class
                    );

            startActivity(intent);

            finish();
        });


        // LOAD APPS
        loadApps();
    }


    // ============================================================
    // LOAD USER INSTALLED APPS
    // ============================================================

    private void loadApps() {

        // CLEAR OLD LIST
        appContainer.removeAllViews();


        // PACKAGE MANAGER
        PackageManager packageManager =
                getPackageManager();


        // GET ALL INSTALLED APPLICATIONS
        List<ApplicationInfo> apps =
                packageManager.getInstalledApplications(
                        PackageManager.GET_META_DATA
                );


        int totalApps = 0;

        long totalAppSize = 0;


        // LOOP THROUGH ALL APPLICATIONS
        for (ApplicationInfo appInfo : apps) {


            // SKIP SYSTEM APPS
            if ((appInfo.flags
                    & ApplicationInfo.FLAG_SYSTEM) != 0) {

                continue;
            }


            // CHECK IF APP HAS LAUNCHER
            Intent launchIntent =
                    packageManager.getLaunchIntentForPackage(
                            appInfo.packageName
                    );


            // SKIP NON-LAUNCHABLE APPS
            if (launchIntent == null) {

                continue;
            }


            // GET APP NAME
            String appName =
                    packageManager
                            .getApplicationLabel(appInfo)
                            .toString();


            // GET PACKAGE NAME
            String packageName =
                    appInfo.packageName;


            // GET APK SIZE
            long appSize = 0;

            try {

                if (appInfo.sourceDir != null) {

                    File apkFile =
                            new File(appInfo.sourceDir);

                    if (apkFile.exists()) {

                        appSize =
                                apkFile.length();
                    }
                }

            } catch (Exception e) {

                e.printStackTrace();
            }


            // ADD SIZE
            totalAppSize += appSize;


            // INCREASE COUNT
            totalApps++;


            // ====================================================
            // APP CARD
            // ====================================================

            LinearLayout appCard =
                    new LinearLayout(this);

            appCard.setOrientation(
                    LinearLayout.VERTICAL
            );

            appCard.setPadding(
                    30,
                    25,
                    30,
                    25
            );


            // APP NAME
            TextView appNameText =
                    new TextView(this);

            appNameText.setText(
                    "📱 " + appName
            );

            appNameText.setTextSize(18);

            appNameText.setTextColor(
                    0xFF171717
            );

            appCard.addView(
                    appNameText
            );


            // PACKAGE NAME
            TextView packageText =
                    new TextView(this);

            packageText.setText(
                    "Package: " + packageName
            );

            packageText.setTextSize(14);

            packageText.setTextColor(
                    0xFF555555
            );

            appCard.addView(
                    packageText
            );


            // APP TYPE
            TextView typeText =
                    new TextView(this);

            typeText.setText(
                    "Type: Downloaded App"
            );

            typeText.setTextSize(14);

            typeText.setTextColor(
                    0xFF555555
            );

            appCard.addView(
                    typeText
            );


            // APK SIZE
            TextView sizeText =
                    new TextView(this);

            sizeText.setText(
                    "APK Size: " +
                            convertSize(appSize)
            );

            sizeText.setTextSize(14);

            sizeText.setTextColor(
                    0xFF555555
            );

            appCard.addView(
                    sizeText
            );


            // ====================================================
            // UNINSTALL BUTTON
            // ====================================================

            Button uninstallButton =
                    new Button(this);

            uninstallButton.setText(
                    "Uninstall"
            );


            uninstallButton.setOnClickListener(view -> {

                uninstallApp(packageName);

            });


            appCard.addView(
                    uninstallButton
            );


            // ADD CARD
            appContainer.addView(
                    appCard
            );


            // ====================================================
            // SEPARATOR
            // ====================================================

            TextView separator =
                    new TextView(this);

            separator.setBackgroundColor(
                    0xFFEEEEEE
            );


            LinearLayout.LayoutParams separatorParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            2
                    );


            separatorParams.setMargins(
                    0,
                    10,
                    0,
                    10
            );


            appContainer.addView(
                    separator,
                    separatorParams
            );
        }


        // DISPLAY COUNT
        appCount.setText(
                totalApps + " Apps"
        );


        appCountBottom.setText(
                totalApps + " apps"
        );


        // DISPLAY TOTAL APK SIZE
        appTotalSize.setText(
                convertSize(totalAppSize)
        );


        // NO APPS
        if (totalApps == 0) {

            showMessage();
        }
    }


    // ============================================================
    // UNINSTALL APP
    // ============================================================

    private void uninstallApp(String packageName) {

        try {

            // CREATE PACKAGE URI
            Uri packageUri =
                    Uri.parse("package:" + packageName);

            // CREATE UNINSTALL INTENT
            Intent uninstallIntent =
                    new Intent(Intent.ACTION_DELETE);

            // SET APP PACKAGE
            uninstallIntent.setData(packageUri);

            // MAKE SURE ANDROID HANDLES THE REQUEST
            uninstallIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            // OPEN ANDROID UNINSTALL SCREEN
            startActivity(uninstallIntent);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // ============================================================
    // REFRESH AFTER UNINSTALL
    // ============================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (appContainer != null) {

            loadApps();
        }
    }


    // ============================================================
    // NO APPS MESSAGE
    // ============================================================

    private void showMessage() {

        TextView message =
                new TextView(this);

        message.setText(
                "No downloaded apps found"
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

        appContainer.addView(
                message
        );
    }


    // ============================================================
    // CONVERT BYTES
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
                    bytes / (1024.0 * 1024.0)
            );
        }


        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                bytes /
                        (1024.0 * 1024.0 * 1024.0)
        );
    }
}