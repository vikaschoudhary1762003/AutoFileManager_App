package com.example.autofilemanager;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UnusedApp extends AppCompatActivity {

    // =========================================================
    // VARIABLES
    // =========================================================

    TextView backbtn;

    LinearLayout unUsedAppContainer;

    TextView totalUnusedAppSize;

    TextView totalUnusedAppCount;

    TextView selectedAppText;


    // =========================================================
    // UNUSED APP LIMIT
    // =========================================================

    // APP NOT USED FOR 30 DAYS = UNUSED

    private static final long UNUSED_DAYS = 30;

    private static final long UNUSED_TIME =
            UNUSED_DAYS
                    * 24L
                    * 60L
                    * 60L
                    * 1000L;


    // =========================================================
    // APP DATA CLASS
    // =========================================================

    static class UnusedApplication {

        String appName;

        String packageName;

        long apkSize;

        long lastUsedTime;


        UnusedApplication(
                String appName,
                String packageName,
                long apkSize,
                long lastUsedTime) {

            this.appName = appName;

            this.packageName = packageName;

            this.apkSize = apkSize;

            this.lastUsedTime = lastUsedTime;
        }
    }


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_unused_app
        );


        // =====================================================
        // CONNECT XML
        // =====================================================

        backbtn =
                findViewById(
                        R.id.BackButton
                );


        unUsedAppContainer =
                findViewById(
                        R.id.unUsedAppContainer
                );


        totalUnusedAppSize =
                findViewById(
                        R.id.totalUnusedAppSize
                );


        totalUnusedAppCount =
                findViewById(
                        R.id.totalUnusedAppCount
                );


        selectedAppText =
                findViewById(
                        R.id.selectedAppText
                );


        // =====================================================
        // BACK BUTTON
        // =====================================================

        backbtn.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            UnusedApp.this,
                            MainActivity.class
                    );

            startActivity(intent);

            finish();
        });


        // =====================================================
        // CHECK USAGE ACCESS
        // =====================================================

        if (hasUsageAccessPermission()) {

            loadUnusedApps();

        } else {

            Toast.makeText(
                    this,
                    "Please allow Usage Access for AutoFileManager",
                    Toast.LENGTH_LONG
            ).show();


            Intent intent =
                    new Intent(
                            Settings.ACTION_USAGE_ACCESS_SETTINGS
                    );


            startActivity(intent);
        }
    }


    // =========================================================
    // ON RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        if (hasUsageAccessPermission()) {

            if (unUsedAppContainer != null) {

                loadUnusedApps();
            }
        }
    }


    // =========================================================
    // CHECK USAGE ACCESS PERMISSION
    // =========================================================

    private boolean hasUsageAccessPermission() {

        AppOpsManager appOps =
                (AppOpsManager)
                        getSystemService(
                                Context.APP_OPS_SERVICE
                        );


        if (appOps == null) {

            return false;
        }


        int mode =
                appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        getPackageName()
                );


        return mode ==
                AppOpsManager.MODE_ALLOWED;
    }


    // =========================================================
    // LOAD UNUSED APPS
    // =========================================================

    private void loadUnusedApps() {

        // CLEAR OLD DATA
        unUsedAppContainer.removeAllViews();


        // =====================================================
        // CURRENT TIME
        // =====================================================

        long currentTime =
                System.currentTimeMillis();


        // =====================================================
        // 30 DAY LIMIT
        // =====================================================

        long unusedLimit =
                currentTime - UNUSED_TIME;


        // =====================================================
        // GET USAGE MANAGER
        // =====================================================

        UsageStatsManager usageStatsManager =
                (UsageStatsManager)
                        getSystemService(
                                Context.USAGE_STATS_SERVICE
                        );


        if (usageStatsManager == null) {

            Toast.makeText(
                    this,
                    "Usage statistics unavailable",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // =====================================================
        // GET USAGE DATA
        // =====================================================

        long oneYearAgo =
                currentTime -
                        (
                                365L
                                        * 24L
                                        * 60L
                                        * 60L
                                        * 1000L
                        );


        List<UsageStats> usageStatsList =
                new ArrayList<>(
                        usageStatsManager
                                .queryAndAggregateUsageStats(
                                        oneYearAgo,
                                        currentTime
                                )
                                .values()
                );


        // =====================================================
        // CREATE LAST USED MAP
        // =====================================================

        Map<String, Long> lastUsedMap =
                new HashMap<>();


        for (UsageStats usageStats :
                usageStatsList) {

            String packageName =
                    usageStats.getPackageName();


            long lastUsed =
                    usageStats.getLastTimeUsed();


            // KEEP THE LATEST TIME
            Long oldTime =
                    lastUsedMap.get(
                            packageName
                    );


            if (oldTime == null ||
                    lastUsed > oldTime) {

                lastUsedMap.put(
                        packageName,
                        lastUsed
                );
            }
        }


        // =====================================================
        // GET PACKAGE MANAGER
        // =====================================================

        PackageManager packageManager =
                getPackageManager();


        // =====================================================
        // GET INSTALLED APPS
        // =====================================================

        List<ApplicationInfo> installedApps =
                packageManager
                        .getInstalledApplications(
                                PackageManager.GET_META_DATA
                        );


        // =====================================================
        // UNUSED APP LIST
        // =====================================================

        ArrayList<UnusedApplication> unusedApps =
                new ArrayList<>();


        // =====================================================
        // CHECK EACH INSTALLED APP
        // =====================================================

        for (ApplicationInfo appInfo :
                installedApps) {


            // =================================================
            // DON'T SHOW AUTOFILLEMANAGER ITSELF
            // =================================================

            if (appInfo.packageName.equals(
                    getPackageName()
            )) {

                continue;
            }


            // =================================================
            // SKIP SYSTEM APPS
            // =================================================

            if ((appInfo.flags
                    & ApplicationInfo.FLAG_SYSTEM) != 0) {

                continue;
            }


            // =================================================
            // SKIP UPDATED SYSTEM APPS
            // =================================================

            if ((appInfo.flags
                    & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
                    != 0) {

                continue;
            }


            // =================================================
            // ONLY SHOW NORMAL LAUNCHABLE APPS
            // =================================================

            Intent launchIntent =
                    packageManager
                            .getLaunchIntentForPackage(
                                    appInfo.packageName
                            );


            if (launchIntent == null) {

                continue;
            }


            // =================================================
            // APP NAME
            // =================================================

            String appName;

            try {

                appName =
                        packageManager
                                .getApplicationLabel(
                                        appInfo
                                )
                                .toString();

            } catch (Exception e) {

                appName =
                        appInfo.packageName;
            }


            // =================================================
            // PACKAGE NAME
            // =================================================

            String packageName =
                    appInfo.packageName;


            // =================================================
            // APK SIZE
            // =================================================

            long apkSize = 0;


            try {

                if (appInfo.sourceDir != null) {

                    File apkFile =
                            new File(
                                    appInfo.sourceDir
                            );


                    if (apkFile.exists()) {

                        apkSize =
                                apkFile.length();
                    }
                }

            } catch (Exception e) {

                apkSize = 0;
            }


            // =================================================
            // LAST USED TIME
            // =================================================

            Long lastUsedObject =
                    lastUsedMap.get(
                            packageName
                    );


            // IMPORTANT:
            //
            // If there is NO usage record,
            // DO NOT automatically call it unused.
            //
            // This prevents many false results.

            if (lastUsedObject == null) {

                continue;
            }


            long lastUsedTime =
                    lastUsedObject;


            // =================================================
            // CHECK 30 DAYS
            // =================================================

            if (lastUsedTime <= unusedLimit) {

                UnusedApplication app =
                        new UnusedApplication(
                                appName,
                                packageName,
                                apkSize,
                                lastUsedTime
                        );


                unusedApps.add(app);
            }
        }


        // =====================================================
        // SORT BY SIZE
        // =====================================================

        unusedApps.sort(
                (app1, app2) ->
                        Long.compare(
                                app2.apkSize,
                                app1.apkSize
                        )
        );


        // =====================================================
        // TOTAL SIZE
        // =====================================================

        long totalSize = 0;


        for (UnusedApplication app :
                unusedApps) {

            totalSize +=
                    app.apkSize;
        }


        // =====================================================
        // UPDATE SUMMARY
        // =====================================================

        totalUnusedAppSize.setText(
                convertSize(totalSize)
        );


        totalUnusedAppCount.setText(
                unusedApps.size()
                        + " Apps Found"
        );


        selectedAppText.setText(
                "0 Apps Selected (0 MB)"
        );


        // =====================================================
        // DISPLAY RESULT
        // =====================================================

        if (unusedApps.isEmpty()) {

            showNoUnusedApps();

        } else {

            for (UnusedApplication app :
                    unusedApps) {

                displayUnusedApp(app);
            }
        }
    }


    // =========================================================
    // DISPLAY UNUSED APP
    // =========================================================

    private void displayUnusedApp(
            UnusedApplication app) {


        // =====================================================
        // MAIN ROW
        // =====================================================

        LinearLayout appRow =
                new LinearLayout(this);


        appRow.setOrientation(
                LinearLayout.HORIZONTAL
        );


        appRow.setGravity(
                Gravity.CENTER_VERTICAL
        );


        appRow.setPadding(
                20,
                20,
                20,
                20
        );


        // =====================================================
        // APP INFORMATION
        // =====================================================

        LinearLayout information =
                new LinearLayout(this);


        information.setOrientation(
                LinearLayout.VERTICAL
        );


        LinearLayout.LayoutParams informationParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );


        // =====================================================
        // APP NAME
        // =====================================================

        TextView appName =
                new TextView(this);


        appName.setText(
                "📱 " + app.appName
        );


        appName.setTextSize(
                17
        );


        appName.setTextColor(
                Color.BLACK
        );


        appName.setTypeface(
                null,
                Typeface.BOLD
        );


        information.addView(
                appName
        );


        // =====================================================
        // PACKAGE
        // =====================================================

        TextView packageText =
                new TextView(this);


        packageText.setText(
                app.packageName
        );


        packageText.setTextSize(
                11
        );


        packageText.setTextColor(
                Color.GRAY
        );


        information.addView(
                packageText
        );


        // =====================================================
        // SIZE
        // =====================================================

        TextView sizeText =
                new TextView(this);


        sizeText.setText(
                "APK Size: "
                        + convertSize(
                        app.apkSize
                )
        );


        sizeText.setTextSize(
                12
        );


        information.addView(
                sizeText
        );


        // =====================================================
        // LAST USED
        // =====================================================

        TextView lastUsedText =
                new TextView(this);


        String lastUsedDate =
                new SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                ).format(
                        new Date(
                                app.lastUsedTime
                        )
                );


        lastUsedText.setText(
                "Last used: "
                        + lastUsedDate
        );


        lastUsedText.setTextSize(
                12
        );


        lastUsedText.setTextColor(
                Color.RED
        );


        information.addView(
                lastUsedText
        );


        // ADD INFORMATION
        appRow.addView(
                information,
                informationParams
        );


        // =====================================================
        // UNINSTALL BUTTON
        // =====================================================

        Button uninstallButton =
                new Button(this);


        uninstallButton.setText(
                "Uninstall"
        );


        uninstallButton.setOnClickListener(
                view -> {

                    uninstallApp(
                            app.packageName
                    );
                }
        );


        appRow.addView(
                uninstallButton
        );


        // =====================================================
        // ADD ROW
        // =====================================================

        unUsedAppContainer.addView(
                appRow
        );


        // =====================================================
        // SEPARATOR
        // =====================================================

        TextView separator =
                new TextView(this);


        separator.setBackgroundColor(
                Color.parseColor(
                        "#EEEEEE"
                )
        );


        LinearLayout.LayoutParams separatorParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                );


        unUsedAppContainer.addView(
                separator,
                separatorParams
        );
    }


    // =========================================================
    // UNINSTALL APP
    // =========================================================

    private void uninstallApp(
            String packageName) {

        try {

            // CREATE PACKAGE URI
            Uri packageUri =
                    Uri.parse(
                            "package:" + packageName
                    );


            // CREATE UNINSTALL INTENT
            Intent uninstallIntent =
                    new Intent(
                            Intent.ACTION_DELETE
                    );


            // SET PACKAGE
            uninstallIntent.setData(
                    packageUri
            );


            // OPEN ANDROID UNINSTALL SCREEN
            startActivity(
                    uninstallIntent
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open uninstall screen",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =========================================================
    // NO UNUSED APPS
    // =========================================================

    private void showNoUnusedApps() {

        TextView noApps =
                new TextView(this);


        noApps.setText(
                "No unused apps found.\n\n"
                        + "No user-installed app has "
                        + "been unused for 30 days."
        );


        noApps.setTextSize(
                16
        );


        noApps.setTextColor(
                Color.GRAY
        );


        noApps.setGravity(
                Gravity.CENTER
        );


        noApps.setPadding(
                20,
                60,
                20,
                60
        );


        unUsedAppContainer.addView(
                noApps
        );
    }


    // =========================================================
    // CONVERT SIZE
    // =========================================================

    private String convertSize(long size) {

        if (size <= 0) {

            return "0 B";
        }


        double value =
                size;


        if (value < 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.0f B",
                    value
            );
        }


        value =
                value / 1024;


        if (value < 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.2f KB",
                    value
            );
        }


        value =
                value / 1024;


        if (value < 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.2f MB",
                    value
            );
        }


        value =
                value / 1024;


        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                value
        );
    }
}