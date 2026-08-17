package com.example.autofilemanager;

import android.content.Intent; // INTENT IS USED TO MOVE FROM ONE ACTIVITY/PAGE TO ANOTHER
import android.os.Bundle; // IT USED TO PASS OR RESTORE THE SET OF DATA BETWEEN ANDROID LIFECYCLE STATES
import android.os.StatFs; // IT USED TO GET INFORMATION ABOUT FILE SYSTEM / STORAGE OF MOBILE DEVICE like TOTAL STORAGE OF MOBILE IS 16GB USED 8GB FREE 8GB ETC
import android.widget.LinearLayout; // IT REPRESENTS LINEAR LAYOUT FROM XML FRONTEND LIKE FOR ID .
import android.widget.TextView;
import android.widget.ProgressBar; // PROGRESS BAR


import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private LinearLayout scanStorage;
    private LinearLayout duplicateFiles;
    private LinearLayout largeFiles;
    private LinearLayout oldFiles;
    private LinearLayout unusedApps;
    private LinearLayout compressFiles;

    private LinearLayout downloadedFiles;
    private LinearLayout audioFiles ;
    private LinearLayout imagesFiles ;
    private LinearLayout videosFiles ;
    private LinearLayout documentsFiles ;
    private LinearLayout appsFiles;



    //STORAGE VIEWS
    private TextView storagePercentage;
    private TextView usedStorage;
    private TextView totalStorage;
    private TextView freeStorage;
    private TextView storageAlert;

    //PROGRESS BAR
    private ProgressBar storageProgress ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // CONNECT OT  ID
        scanStorage = findViewById(R.id.scanStorage);
        duplicateFiles = findViewById(R.id.duplicateFiles);
        largeFiles = findViewById(R.id.largeFiles);
        oldFiles = findViewById(R.id.oldFiles);
        unusedApps = findViewById(R.id.unusedApps);
        compressFiles = findViewById(R.id.compressFiles);
        downloadedFiles = findViewById(R.id.downloadedFiles);
        audioFiles = findViewById(R.id.audioFiles);
        imagesFiles= findViewById(R.id.imagesFiles);
        videosFiles = findViewById(R.id.videoFiles);
        documentsFiles = findViewById(R.id.documentFiles);
        appsFiles = findViewById(R.id.appsFiles);

        //CONNECT STORAGE VIEWS ID
        storagePercentage = findViewById(R.id.storagePercentage);
        usedStorage = findViewById(R.id.usedStorage);
        totalStorage = findViewById(R.id.totalStorage);
        freeStorage = findViewById(R.id.freeStorage);
        storageProgress = findViewById(R.id.storageProgress);
        storageAlert = findViewById(R.id.storageAlert);

        //LOAD THE REAL MOBILE DEVICE STORAGE like totalstorage 128GB used 29GB etc.
        loadStorageData();



        // SCAN STORAGE
        scanStorage.setOnClickListener(v -> {

            Intent intent = new  Intent(MainActivity.this ,ScanStorage.class);
            startActivity(intent);

        });

        // DUPLICATE FILES  ON CLICK OF THIS IT WILL OPEN DUPLICATE FILE PAGE FROM MainActivity.java to Duplicateiles.java
        duplicateFiles.setOnClickListener(v -> {

            Intent intent = new Intent(MainActivity.this , DuplicateFiles.class);
            startActivity(intent);

        });

        // LARGE FILES
        largeFiles.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this , LargeFiles.class);
            startActivity(intent);
        });

        // OLD FILES
        oldFiles.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this , OldFiles.class);
            startActivity(intent);

        });

        // UNUSED APPS
        unusedApps.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this , UnusedApp.class);
            startActivity(intent);

        });

        // COMPRESS FILES
        compressFiles.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this , CompressedFile.class);
            startActivity(intent);

        });

        // OPEN DOWNLOADED FILES PAGE
        downloadedFiles.setOnClickListener(view->{
            Intent intent = new Intent(MainActivity.this , DownloadFiles.class);
            startActivity(intent);
        });

        // OPEN AUDIO PAGE
        audioFiles.setOnClickListener(view->{
            Intent intent = new Intent(MainActivity.this , AudioFiles.class);
            startActivity(intent);
        });

        //OPEN IMAGES PAGE
        imagesFiles.setOnClickListener(view->{
            Intent intent = new Intent(MainActivity.this , ImagesFile.class);
            startActivity(intent);
        });

        //OPEN VIDEOS PAGE
        videosFiles.setOnClickListener(view->{
            Intent intent = new Intent(MainActivity.this , VideosFile.class);
            startActivity(intent);
        });

        //OPEN DOCUMENT PAGE
        documentsFiles.setOnClickListener(view->{
            Intent intent = new Intent(MainActivity.this , DocumentFiles.class);
            startActivity(intent);
        });

        //OPEN APPS PAGE
        appsFiles.setOnClickListener(view->{
            Intent intent = new Intent(MainActivity.this , AppsFile.class);
            startActivity(intent);
        });
    }

    //FUNCTION TO LOAD REAL STORAGE OF MOBILE DEVICE
    private void  loadStorageData(){
        try {
            //GET PRIMARY STORAGE LOCATION
            StatFs statFs = new StatFs(android.os.Environment.getDataDirectory().getPath());

            //BLOCK SIZE
            long blockSize = statFs.getBlockSizeLong();

            //TOTAL BLOCKS
            long totalBlocks = statFs.getBlockCountLong();

            //AVAILABLE BLOCKS
            long availableBlocks = statFs.getAvailableBlocksLong();


            //CALCULATE BYTES
            long totalBytes = totalBlocks * blockSize;
            long freeBytes = availableBlocks * blockSize;
            long usedBytes = totalBytes - freeBytes;


            //CALCULATE PERCENTAGE
            int percentage = 0;
            if(totalBytes > 0 ){
                percentage = (int) ((usedBytes * 100L)/ totalBytes);
            }

            //CONVERT TO GB
            double totalGB = totalBytes / (1024.0 *1024.0 *1024.0);
            double freeGB = freeBytes / (1024.0 * 1024.0 * 1024.0);
            double usedGB = usedBytes / (1024.0 * 1024.0 * 1024.0);

            //UPDATE UI
            storagePercentage.setText(percentage + "%");
            usedStorage.setText(String.format("%.1f GB " , usedGB));
            totalStorage.setText(String.format("%.1f GB" , totalGB));
            freeStorage.setText(String.format("%.1f GB" , freeGB));
            storageProgress.setProgress(percentage);

            //CHECK STORAGE HEALTH AND ALERT . "CALLING FUNCITON"
            checkStorageHealth(percentage);
        }catch (Exception e){
            e.printStackTrace();
            storagePercentage.setText("--");
            usedStorage.setText("--");
            totalStorage.setText("--");
            freeStorage.setText("--");
            storageProgress.setProgress(0);
        }
    }


    //CREATE FUNCTION TO ALERT OR CHECK HEALTH OF STORAGE ACCORDING TO PERCENTAGE
    private void checkStorageHealth(int usagePercent){
        String status;
        String message;

        if (usagePercent < 55){
            status = "Healty";
            message = "Your Storage is Healthy !";
        } else if (usagePercent <65) {
            status = "Good";
            message = "Storage Usage is Increasing !";
        } else if (usagePercent < 85) {
            status ="Warning";
            message = "Your Storage is Getting Low !";
        } else if (usagePercent < 95) {
            status = "Critical";
            message = "Storage is Almost Full , Cleanup Recommended !";
        }else {
            status ="Almost Full";
            message = "Storage is Almost Full. Free up Space Now !";
        }
        storageAlert.setText(message);
    }
}