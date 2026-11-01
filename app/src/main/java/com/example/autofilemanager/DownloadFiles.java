package com.example.autofilemanager;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DownloadFiles extends AppCompatActivity {

    // BACK BUTTON
    private TextView backbtn;

    // MAIN CONTAINER WHERE DOWNLOADED FILES WILL BE DISPLAYED
    private LinearLayout download_Container;

    // TEXT SHOWN WHEN NO FILES ARE FOUND
    private TextView noDownloadsText;

    // STORE ALL DOWNLOADED FILES
    private final List<File> downloadFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ENABLE EDGE TO EDGE
        EdgeToEdge.enable(this);

        // CONNECT XML LAYOUT
        setContentView(R.layout.activity_download_files);

        // CONNECT VIEWS
        backbtn = findViewById(R.id.BackButton);
        download_Container = findViewById(R.id.downloadFileContainer);
        noDownloadsText = findViewById(R.id.noDownloadsText);

        // BACK TO MAIN DASHBOARD
        backbtn.setOnClickListener(view -> {

            Intent intent = new Intent(
                    DownloadFiles.this,
                    MainActivity.class
            );

            // PREVENT MULTIPLE MAIN ACTIVITIES
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);

            // CLOSE CURRENT PAGE
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // LOAD DOWNLOAD FILES EVERY TIME PAGE BECOMES VISIBLE
        loadDownloadFiles();
    }

    /**
     * LOAD ALL FILES FROM:
     *
     * Internal Storage / Download
     *
     * This does NOT limit files by extension.
     *
     * Therefore it can find:
     * PDF
     * DOC
     * DOCX
     * PPT
     * PPTX
     * XLS
     * XLSX
     * JPG
     * PNG
     * MP4
     * MP3
     * ZIP
     * APK
     * TXT
     * and other files.
     */
    private void loadDownloadFiles() {

        // CLEAR OLD DATA
        downloadFiles.clear();

        // CLEAR OLD UI FILE ITEMS
        download_Container.removeAllViews();

        // ANDROID 11 AND ABOVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            // CHECK WHETHER ALL FILES ACCESS IS GRANTED
            if (!Environment.isExternalStorageManager()) {

                showPermissionMessage();
                return;
            }
        }

        // GET DOWNLOAD DIRECTORY
        File downloadDirectory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
        );

        // CHECK WHETHER DOWNLOAD DIRECTORY EXISTS
        if (downloadDirectory == null || !downloadDirectory.exists()) {

            showNoFilesMessage();

            return;
        }

        // CHECK WHETHER IT IS ACTUALLY A DIRECTORY
        if (!downloadDirectory.isDirectory()) {

            showNoFilesMessage();

            return;
        }

        // SCAN DOWNLOAD DIRECTORY
        scanDownloadDirectory(downloadDirectory);

        // SORT FILES
        // NEWEST FILES WILL APPEAR FIRST
        Collections.sort(
                downloadFiles,
                new Comparator<File>() {
                    @Override
                    public int compare(File file1, File file2) {

                        return Long.compare(
                                file2.lastModified(),
                                file1.lastModified()
                        );
                    }
                }
        );

        // UPDATE TOTAL FILE COUNT AND SIZE
        updateDownloadSummary();

        // IF NO FILES WERE FOUND
        if (downloadFiles.isEmpty()) {

            showNoFilesMessage();

        } else {

            // HIDE "NO FILES" MESSAGE
            noDownloadsText.setVisibility(View.GONE);

            // DISPLAY FILES
            displayFiles();
        }
    }

    /**
     * SCAN DOWNLOAD DIRECTORY RECURSIVELY.
     *
     * This also checks subfolders inside Download.
     */
    private void scanDownloadDirectory(File directory) {

        File[] files;

        try {

            files = directory.listFiles();

        } catch (SecurityException e) {

            return;
        }

        // DIRECTORY COULD NOT BE READ
        if (files == null) {
            return;
        }

        for (File file : files) {

            try {

                // IF IT IS A DIRECTORY
                if (file.isDirectory()) {

                    // SCAN SUBDIRECTORY
                    scanDownloadDirectory(file);

                }

                // IF IT IS A FILE
                else if (file.isFile()) {

                    // IGNORE EMPTY / INVALID FILES
                    if (file.exists() && file.length() >= 0) {

                        downloadFiles.add(file);
                    }
                }

            } catch (SecurityException ignored) {

                // IGNORE FILES WHICH CANNOT BE ACCESSED
            }
        }
    }

    /**
     * DISPLAY ALL DOWNLOADED FILES INSIDE
     * downloadFileContainer.
     */
    private void displayFiles() {

        for (File file : downloadFiles) {

            // CREATE ONE ROW FOR ONE FILE
            LinearLayout fileRow = createFileRow(file);

            // ADD ROW TO SCREEN
            download_Container.addView(fileRow);
        }
    }

    /**
     * CREATE UI ROW FOR EACH FILE.
     */
    private LinearLayout createFileRow(File file) {

        // MAIN ROW
        LinearLayout row = new LinearLayout(this);

        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.setPadding(
                dpToPx(12),
                dpToPx(8),
                dpToPx(12),
                dpToPx(8)
        );

        // WIDTH = MATCH_PARENT
        // HEIGHT = WRAP_CONTENT
        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        row.setLayoutParams(rowParams);

        // MAKE ROW CLICKABLE
        row.setClickable(true);
        row.setFocusable(true);

        // FILE ICON
        TextView icon = new TextView(this);

        icon.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dpToPx(55),
                        dpToPx(55)
                )
        );

        icon.setGravity(Gravity.CENTER);

        icon.setText(
                getFileIcon(file.getName())
        );

        icon.setTextSize(27);

        icon.setTextColor(
                Color.parseColor("#1558D6")
        );

        // FILE INFORMATION CONTAINER
        LinearLayout information = new LinearLayout(this);

        information.setOrientation(
                LinearLayout.VERTICAL
        );

        information.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams informationParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        informationParams.setMargins(
                dpToPx(12),
                0,
                dpToPx(8),
                0
        );

        information.setLayoutParams(
                informationParams
        );

        // FILE NAME
        TextView fileName = new TextView(this);

        fileName.setText(
                file.getName()
        );

        fileName.setTextSize(16);

        fileName.setTextColor(
                Color.parseColor("#171717")
        );

        fileName.setMaxLines(2);

        // FILE DETAILS
        TextView fileDetails = new TextView(this);

        String details =
                getFileType(file.getName())
                        + " • "
                        + formatFileSize(file.length());

        fileDetails.setText(details);

        fileDetails.setTextSize(13);

        fileDetails.setTextColor(
                Color.parseColor("#777777")
        );

        fileDetails.setMaxLines(1);

        // ADD TEXT VIEWS
        information.addView(fileName);

        information.addView(fileDetails);

        // OPEN BUTTON / ARROW
        TextView openButton = new TextView(this);

        openButton.setText("›");

        openButton.setTextSize(30);

        openButton.setTextColor(
                Color.parseColor("#777777")
        );

        openButton.setGravity(Gravity.CENTER);

        openButton.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dpToPx(35),
                        dpToPx(55)
                )
        );

        // ADD EVERYTHING TO ROW
        row.addView(icon);

        row.addView(information);

        row.addView(openButton);

        // CLICK FILE
        row.setOnClickListener(view -> {

            openFile(file);
        });

        // LONG PRESS FILE
        row.setOnLongClickListener(view -> {

            showFileInformation(file);

            return true;
        });

        return row;
    }

    /**
     * OPEN SELECTED FILE.
     *
     * Android decides which application should open
     * the selected file.
     */
    private void openFile(File file) {

        if (file == null || !file.exists()) {

            Toast.makeText(
                    this,
                    "File no longer exists",
                    Toast.LENGTH_SHORT
            ).show();

            // REFRESH LIST
            loadDownloadFiles();

            return;
        }

        try {

            Uri fileUri = Uri.fromFile(file);

            Intent intent = new Intent(
                    Intent.ACTION_VIEW
            );

            intent.setDataAndType(
                    fileUri,
                    getMimeType(file.getName())
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "No application found to open this file",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * SHOW FILE INFORMATION WHEN USER LONG PRESSES A FILE.
     */
    private void showFileInformation(File file) {

        String information =
                "Name: "
                        + file.getName()
                        + "\n\n"
                        + "Type: "
                        + getFileType(file.getName())
                        + "\n\n"
                        + "Size: "
                        + formatFileSize(file.length())
                        + "\n\n"
                        + "Location: "
                        + file.getAbsolutePath();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("File Information")
                .setMessage(information)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * UPDATE TOTAL FILE COUNT AND TOTAL SIZE.
     */
    private void updateDownloadSummary() {

        TextView totalFiles =
                findViewById(R.id.totalDownloadFiles);

        TextView totalSize =
                findViewById(R.id.totalDownloadSize);

        long totalBytes = 0;

        for (File file : downloadFiles) {

            try {

                totalBytes += file.length();

            } catch (SecurityException ignored) {
            }
        }

        totalFiles.setText(
                downloadFiles.size() + " files"
        );

        totalSize.setText(
                formatFileSize(totalBytes)
        );
    }

    /**
     * SHOW NO FILES MESSAGE.
     */
    private void showNoFilesMessage() {

        noDownloadsText.setVisibility(
                View.VISIBLE
        );

        noDownloadsText.setText(
                "No downloaded files found"
        );

        updateDownloadSummary();
    }

    /**
     * SHOW PERMISSION MESSAGE.
     *
     * Android 11+ requires special All Files Access
     * for direct access to arbitrary files.
     */
    private void showPermissionMessage() {

        noDownloadsText.setVisibility(
                View.VISIBLE
        );

        noDownloadsText.setText(
                "Storage access is required.\n\n" +
                        "Allow \"All files access\" for AutoFileManager."
        );

        // SET COUNT TO ZERO
        TextView totalFiles =
                findViewById(R.id.totalDownloadFiles);

        TextView totalSize =
                findViewById(R.id.totalDownloadSize);

        totalFiles.setText("0 files");

        totalSize.setText("0 B");

        // OPEN SETTINGS
        noDownloadsText.setOnClickListener(view -> {

            openAllFilesAccessSettings();
        });
    }

    /**
     * OPEN ANDROID ALL FILES ACCESS SETTINGS.
     */
    private void openAllFilesAccessSettings() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            try {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                );

                intent.setData(
                        Uri.parse(
                                "package:" + getPackageName()
                        )
                );

                startActivity(intent);

            } catch (Exception e) {

                try {

                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    );

                    startActivity(intent);

                } catch (Exception ignored) {

                    Toast.makeText(
                            this,
                            "Please allow storage access from Settings",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        }
    }

    /**
     * GET FILE TYPE FROM FILE EXTENSION.
     */
    private String getFileType(String fileName) {

        String extension = getFileExtension(fileName);

        if (extension.isEmpty()) {
            return "File";
        }

        switch (extension) {

            // IMAGES
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "webp":
            case "bmp":
            case "heic":
            case "heif":
                return "Image";

            // VIDEOS
            case "mp4":
            case "mkv":
            case "avi":
            case "mov":
            case "3gp":
            case "webm":
                return "Video";

            // AUDIO
            case "mp3":
            case "wav":
            case "aac":
            case "m4a":
            case "ogg":
            case "flac":
                return "Audio";

            // PDF
            case "pdf":
                return "PDF";

            // WORD
            case "doc":
            case "docx":
                return "Word Document";

            // POWERPOINT
            case "ppt":
            case "pptx":
                return "PowerPoint";

            // EXCEL
            case "xls":
            case "xlsx":
                return "Excel";

            // TEXT
            case "txt":
            case "csv":
                return "Text File";

            // ARCHIVE
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return "Archive";

            // APK
            case "apk":
            case "xapk":
                return "Application";

            default:
                return extension.toUpperCase() + " File";
        }
    }

    /**
     * GET FILE ICON.
     */
    private String getFileIcon(String fileName) {

        String extension =
                getFileExtension(fileName);

        switch (extension) {

            // IMAGE
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "webp":
            case "bmp":
            case "heic":
            case "heif":
                return "▧";

            // VIDEO
            case "mp4":
            case "mkv":
            case "avi":
            case "mov":
            case "3gp":
            case "webm":
                return "▶";

            // AUDIO
            case "mp3":
            case "wav":
            case "aac":
            case "m4a":
            case "ogg":
            case "flac":
                return "♫";

            // PDF
            case "pdf":
                return "PDF";

            // WORD
            case "doc":
            case "docx":
                return "W";

            // POWERPOINT
            case "ppt":
            case "pptx":
                return "P";

            // EXCEL
            case "xls":
            case "xlsx":
                return "X";

            // ZIP
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return "ZIP";

            // APK
            case "apk":
            case "xapk":
                return "APK";

            // DEFAULT
            default:
                return "▤";
        }
    }

    /**
     * GET FILE EXTENSION.
     */
    private String getFileExtension(String fileName) {

        if (fileName == null) {
            return "";
        }

        int dotPosition =
                fileName.lastIndexOf(".");

        if (
                dotPosition == -1 ||
                        dotPosition == fileName.length() - 1
        ) {
            return "";
        }

        return fileName
                .substring(dotPosition + 1)
                .toLowerCase(Locale.ROOT);
    }

    /**
     * GET MIME TYPE.
     */
    private String getMimeType(String fileName) {

        String extension =
                getFileExtension(fileName);

        switch (extension) {

            case "pdf":
                return "application/pdf";

            case "doc":
                return "application/msword";

            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

            case "ppt":
                return "application/vnd.ms-powerpoint";

            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";

            case "xls":
                return "application/vnd.ms-excel";

            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            case "txt":
                return "text/plain";

            case "csv":
                return "text/csv";

            case "jpg":
            case "jpeg":
                return "image/jpeg";

            case "png":
                return "image/png";

            case "gif":
                return "image/gif";

            case "webp":
                return "image/webp";

            case "mp4":
                return "video/mp4";

            case "mkv":
                return "video/x-matroska";

            case "mp3":
                return "audio/mpeg";

            case "wav":
                return "audio/wav";

            case "zip":
                return "application/zip";

            case "apk":
                return "application/vnd.android.package-archive";

            default:
                return "*/*";
        }
    }

    /**
     * CONVERT BYTES TO READABLE SIZE.
     */
    private String formatFileSize(long bytes) {

        if (bytes <= 0) {
            return "0 B";
        }

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.1f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {

            return String.format(
                    Locale.getDefault(),
                    "%.1f MB",
                    bytes / (1024.0 * 1024.0)
            );
        }

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
        );
    }

    /**
     * DP TO PIXELS.
     */
    private int dpToPx(int dp) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(dp * density);
    }
}