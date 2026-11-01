package com.example.autofilemanager;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;


/**
 * ScanStorage
 *
 * This Activity scans shared internal storage for:
 *
 * 1. Empty files
 * 2. Empty folders
 * 3. .tmp files
 * 4. .temp files
 * 5. .cache files
 * 6. .log files
 * 7. .nomedia files
 * 8. Thumbnail files
 * 9. Files inside cache/temp/tmp/.thumbnails folders
 *
 * The user can select detected items and delete them directly.
 *
 * IMPORTANT:
 * This class intentionally does NOT use the Android folder picker.
 * It uses File.delete() directly.
 */
public class ScanStorage extends AppCompatActivity {


    // =========================================================
    // UI VARIABLES
    // =========================================================

    private TextView backButton;
    private TextView scanStatus;
    private TextView junkTotalSize;
    private TextView junkTotalCount;
    private TextView selectedFileText;

    private Button deleteButton;

    private LinearLayout junkFileContainer;


    // =========================================================
    // FILE LISTS
    // =========================================================

    /*
     * Every junk item found during scanning.
     *
     * This can contain both:
     *
     * File
     *
     * and
     *
     * empty Directory
     */
    private final ArrayList<File> junkFiles =
            new ArrayList<>();


    /*
     * Items selected by the user.
     */
    private final ArrayList<File> selectedFiles =
            new ArrayList<>();


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        // -----------------------------------------------------
        // ENABLE EDGE TO EDGE
        // -----------------------------------------------------

        EdgeToEdge.enable(this);


        // -----------------------------------------------------
        // LOAD XML
        // -----------------------------------------------------

        setContentView(
                R.layout.activity_scan_storage
        );


        // -----------------------------------------------------
        // CONNECT XML VIEWS
        // -----------------------------------------------------

        backButton =
                findViewById(R.id.BackButton);

        scanStatus =
                findViewById(R.id.scanStatus);

        junkTotalSize =
                findViewById(R.id.junkTotalSize);

        junkTotalCount =
                findViewById(R.id.junkTotalCount);

        selectedFileText =
                findViewById(R.id.selectedFileText);

        deleteButton =
                findViewById(R.id.deleteButton);

        junkFileContainer =
                findViewById(R.id.junkFileContainer);


        // -----------------------------------------------------
        // BACK BUTTON
        // -----------------------------------------------------

        backButton.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            ScanStorage.this,
                            MainActivity.class
                    );

            startActivity(intent);

            finish();
        });


        // -----------------------------------------------------
        // DELETE BUTTON
        // -----------------------------------------------------

        deleteButton.setOnClickListener(view -> {

            deleteSelectedFiles();

        });


        // -----------------------------------------------------
        // CHECK STORAGE ACCESS
        // -----------------------------------------------------

        checkStorageAccess();
    }


    // =========================================================
    // CHECK STORAGE ACCESS
    // =========================================================

    private void checkStorageAccess() {

        /*
         * Android 11+ requires MANAGE_EXTERNAL_STORAGE
         * for broad filesystem access.
         */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (!Environment.isExternalStorageManager()) {

                scanStatus.setText(
                        "Storage access required"
                );


                Toast.makeText(
                        this,
                        "Allow All Files Access to scan storage",
                        Toast.LENGTH_LONG
                ).show();


                try {

                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                            );

                    intent.setData(
                            android.net.Uri.parse(
                                    "package:" +
                                            getPackageName()
                            )
                    );

                    startActivity(intent);

                } catch (Exception e) {

                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            );

                    startActivity(intent);
                }

                return;
            }
        }


        // -----------------------------------------------------
        // START SCANNING
        // -----------------------------------------------------

        scanStorage();
    }


    // =========================================================
    // ON RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        /*
         * When returning from Android Settings,
         * start scanning again.
         */

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                Environment.isExternalStorageManager()) {

            if (junkFileContainer != null) {

                scanStorage();
            }
        }
    }


    // =========================================================
    // SCAN STORAGE
    // =========================================================

    private void scanStorage() {


        // -----------------------------------------------------
        // CLEAR PREVIOUS RESULTS
        // -----------------------------------------------------

        junkFiles.clear();

        selectedFiles.clear();

        junkFileContainer.removeAllViews();


        // -----------------------------------------------------
        // RESET SELECTION TEXT
        // -----------------------------------------------------

        updateSelectedText();


        // -----------------------------------------------------
        // UPDATE STATUS
        // -----------------------------------------------------

        scanStatus.setText(
                "Scanning storage..."
        );


        // -----------------------------------------------------
        // GET INTERNAL STORAGE ROOT
        // -----------------------------------------------------

        File root =
                Environment.getExternalStorageDirectory();


        // -----------------------------------------------------
        // SCAN
        // -----------------------------------------------------

        scanDirectory(root);


        // -----------------------------------------------------
        // SORT
        // -----------------------------------------------------

        Collections.sort(
                junkFiles,
                new Comparator<File>() {

                    @Override
                    public int compare(
                            File first,
                            File second) {

                        /*
                         * Empty folders have no file size,
                         * therefore their length() is normally 0.
                         *
                         * Larger junk files are shown first.
                         */

                        return Long.compare(
                                getItemSize(second),
                                getItemSize(first)
                        );
                    }
                }
        );


        // -----------------------------------------------------
        // CALCULATE TOTAL SIZE
        // -----------------------------------------------------

        long totalSize = 0;


        for (File file : junkFiles) {

            totalSize +=
                    getItemSize(file);
        }


        // -----------------------------------------------------
        // UPDATE SUMMARY
        // -----------------------------------------------------

        junkTotalSize.setText(
                formatSize(totalSize)
        );


        junkTotalCount.setText(
                junkFiles.size() +
                        " items found"
        );


        // -----------------------------------------------------
        // SCAN COMPLETE
        // -----------------------------------------------------

        scanStatus.setText(
                "Scan completed"
        );


        // -----------------------------------------------------
        // DISPLAY RESULTS
        // -----------------------------------------------------

        if (junkFiles.isEmpty()) {

            showNoFiles();

        } else {

            for (File file : junkFiles) {

                addFileRow(file);
            }
        }
    }


    // =========================================================
    // RECURSIVE STORAGE SCANNER
    // =========================================================

    private void scanDirectory(File directory) {


        // -----------------------------------------------------
        // VALIDATE DIRECTORY
        // -----------------------------------------------------

        if (directory == null ||
                !directory.exists() ||
                !directory.isDirectory()) {

            return;
        }


        // -----------------------------------------------------
        // NEVER SCAN ANDROID/DATA
        // -----------------------------------------------------

        String directoryPath =
                directory.getAbsolutePath();


        if (isRestrictedAndroidFolder(
                directoryPath
        )) {

            return;
        }


        // -----------------------------------------------------
        // GET CHILDREN
        // -----------------------------------------------------

        File[] children;


        try {

            children =
                    directory.listFiles();

        } catch (SecurityException e) {

            return;
        }


        // -----------------------------------------------------
        // NO ACCESS
        // -----------------------------------------------------

        if (children == null) {

            return;
        }


        // -----------------------------------------------------
        // EMPTY DIRECTORY
        // -----------------------------------------------------

        if (children.length == 0) {


            /*
             * Do not add the root storage itself.
             */

            File root =
                    Environment.getExternalStorageDirectory();


            if (!directory.equals(root)) {

                junkFiles.add(directory);
            }


            return;
        }


        // -----------------------------------------------------
        // SCAN CHILDREN
        // -----------------------------------------------------

        for (File child : children) {


            try {


                // =================================================
                // DIRECTORY
                // =================================================

                if (child.isDirectory()) {


                    String childPath =
                            child.getAbsolutePath();


                    /*
                     * Never enter Android/data or Android/obb.
                     */

                    if (isRestrictedAndroidFolder(
                            childPath
                    )) {

                        continue;
                    }


                    /*
                     * Recursively scan the directory.
                     *
                     * If it is empty, scanDirectory()
                     * will add it to junkFiles.
                     */

                    scanDirectory(child);
                }


                // =================================================
                // FILE
                // =================================================

                else if (child.isFile()) {


                    if (isPotentialJunk(child)) {

                        junkFiles.add(child);
                    }
                }


            } catch (SecurityException ignored) {

                /*
                 * Some storage locations may be inaccessible.
                 * Simply skip those locations.
                 */
            }
        }
    }


    // =========================================================
    // RESTRICTED ANDROID FOLDER CHECK
    // =========================================================

    private boolean isRestrictedAndroidFolder(
            String path
    ) {


        if (path == null) {

            return true;
        }


        String lowerPath =
                path.toLowerCase(
                        Locale.getDefault()
                );


        /*
         * Android/data
         */

        if (lowerPath.contains(
                "/android/data"
        )) {

            return true;
        }


        /*
         * Android/obb
         */

        if (lowerPath.contains(
                "/android/obb"
        )) {

            return true;
        }


        return false;
    }


    // =========================================================
    // CHECK POTENTIAL JUNK FILE
    // =========================================================

    private boolean isPotentialJunk(File file) {


        // -----------------------------------------------------
        // FILE NAME
        // -----------------------------------------------------

        String name =
                file.getName()
                        .toLowerCase(
                                Locale.getDefault()
                        );


        // -----------------------------------------------------
        // FILE PATH
        // -----------------------------------------------------

        String path =
                file.getAbsolutePath()
                        .toLowerCase(
                                Locale.getDefault()
                        );


        // =====================================================
        // EMPTY FILE
        // =====================================================

        if (file.length() == 0) {

            return true;
        }


        // =====================================================
        // TEMPORARY FILE
        // =====================================================

        if (name.endsWith(".tmp")) {

            return true;
        }


        // =====================================================
        // TEMP FILE
        // =====================================================

        if (name.endsWith(".temp")) {

            return true;
        }


        // =====================================================
        // CACHE FILE
        // =====================================================

        if (name.endsWith(".cache")) {

            return true;
        }


        // =====================================================
        // LOG FILE
        // =====================================================

        if (name.endsWith(".log")) {

            return true;
        }


        // =====================================================
        // NOMEDIA
        // =====================================================

        if (name.equals(".nomedia")) {

            return true;
        }


        // =====================================================
        // THUMBNAIL FILE
        // =====================================================

        if (name.contains("thumb") ||
                name.contains("thumbnail")) {

            return true;
        }


        // =====================================================
        // FILE INSIDE CACHE DIRECTORY
        // =====================================================

        if (path.contains("/cache/")) {

            return true;
        }


        // =====================================================
        // FILE INSIDE TEMP DIRECTORY
        // =====================================================

        if (path.contains("/temp/")) {

            return true;
        }


        // =====================================================
        // FILE INSIDE TMP DIRECTORY
        // =====================================================

        if (path.contains("/tmp/")) {

            return true;
        }


        // =====================================================
        // FILE INSIDE THUMBNAIL DIRECTORY
        // =====================================================

        if (path.contains("/.thumbnails/")) {

            return true;
        }


        // =====================================================
        // NOT JUNK
        // =====================================================

        return false;
    }


    // =========================================================
    // GET ITEM SIZE
    // =========================================================

    private long getItemSize(File file) {


        /*
         * For a normal file, length() gives its size.
         *
         * For an empty folder, the size shown by File.length()
         * is not useful, so we display 0.
         */

        if (file.isFile()) {

            return file.length();
        }


        return 0;
    }


    // =========================================================
    // CREATE FILE/FOLDER ROW
    // =========================================================

    private void addFileRow(File file) {


        // =====================================================
        // CREATE ROW
        // =====================================================

        LinearLayout row =
                new LinearLayout(this);


        row.setOrientation(
                LinearLayout.HORIZONTAL
        );


        row.setGravity(
                Gravity.CENTER_VERTICAL
        );


        row.setPadding(
                4,
                12,
                4,
                12
        );


        // =====================================================
        // CHECKBOX
        // =====================================================

        CheckBox checkBox =
                new CheckBox(this);


        checkBox.setButtonTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#009B55")
                )
        );


        LinearLayout.LayoutParams checkParams =
                new LinearLayout.LayoutParams(
                        48,
                        65
                );


        row.addView(
                checkBox,
                checkParams
        );


        // =====================================================
        // ICON
        // =====================================================

        TextView icon =
                new TextView(this);


        if (file.isDirectory()) {

            icon.setText("📁");

        } else {

            icon.setText("📄");
        }


        icon.setTextSize(26);


        icon.setGravity(
                Gravity.CENTER
        );


        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        45,
                        65
                );


        row.addView(
                icon,
                iconParams
        );


        // =====================================================
        // INFORMATION CONTAINER
        // =====================================================

        LinearLayout information =
                new LinearLayout(this);


        information.setOrientation(
                LinearLayout.VERTICAL
        );


        information.setPadding(
                8,
                0,
                4,
                0
        );


        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );


        // =====================================================
        // NAME
        // =====================================================

        TextView fileName =
                new TextView(this);


        fileName.setText(
                file.getName()
        );


        fileName.setTextSize(15);


        fileName.setTextColor(
                Color.BLACK
        );


        fileName.setTypeface(
                null,
                Typeface.BOLD
        );


        fileName.setMaxLines(2);


        information.addView(
                fileName
        );


        // =====================================================
        // TYPE
        // =====================================================

        TextView type =
                new TextView(this);


        if (file.isDirectory()) {


            type.setText(
                    "Empty folder"
            );


        } else if (file.length() == 0) {


            type.setText(
                    "Empty file • 0 B"
            );


        } else {


            String name =
                    file.getName()
                            .toLowerCase(
                                    Locale.getDefault()
                            );


            if (name.endsWith(".tmp") ||
                    name.endsWith(".temp")) {


                type.setText(
                        "Temporary file • " +
                                formatSize(
                                        file.length()
                                )
                );


            } else if (
                    name.endsWith(".cache")
            ) {


                type.setText(
                        "Cache file • " +
                                formatSize(
                                        file.length()
                                )
                );


            } else if (
                    name.endsWith(".log")
            ) {


                type.setText(
                        "Log file • " +
                                formatSize(
                                        file.length()
                                )
                );


            } else {


                type.setText(
                        "Junk file • " +
                                formatSize(
                                        file.length()
                                )
                );
            }
        }


        type.setTextSize(12);


        type.setTextColor(
                Color.DKGRAY
        );


        information.addView(
                type
        );


        // =====================================================
        // PATH
        // =====================================================

        TextView filePath =
                new TextView(this);


        filePath.setText(
                file.getAbsolutePath()
        );


        filePath.setTextSize(10);


        filePath.setTextColor(
                Color.GRAY
        );


        filePath.setMaxLines(2);


        information.addView(
                filePath
        );


        // =====================================================
        // SIZE
        // =====================================================

        if (file.isFile()) {


            TextView size =
                    new TextView(this);


            size.setText(
                    "Size: " +
                            formatSize(
                                    file.length()
                            )
            );


            size.setTextSize(12);


            size.setTextColor(
                    Color.DKGRAY
            );


            information.addView(
                    size
            );
        }


        // =====================================================
        // MODIFIED DATE
        // =====================================================

        String modified =
                new SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                ).format(
                        new Date(
                                file.lastModified()
                        )
                );


        TextView date =
                new TextView(this);


        date.setText(
                "Modified " +
                        modified
        );


        date.setTextSize(11);


        date.setTextColor(
                Color.GRAY
        );


        information.addView(
                date
        );


        // =====================================================
        // ADD INFORMATION
        // =====================================================

        row.addView(
                information,
                infoParams
        );


        // =====================================================
        // CHECKBOX LISTENER
        // =====================================================

        checkBox.setOnCheckedChangeListener(
                (buttonView, checked) -> {


                    if (checked) {


                        if (!selectedFiles.contains(
                                file
                        )) {

                            selectedFiles.add(file);
                        }


                    } else {


                        selectedFiles.remove(
                                file
                        );
                    }


                    updateSelectedText();
                }
        );


        // =====================================================
        // ADD ROW
        // =====================================================

        junkFileContainer.addView(
                row
        );


        // =====================================================
        // SEPARATOR
        // =====================================================

        View separator =
                new View(this);


        separator.setBackgroundColor(
                Color.parseColor("#EEEEEE")
        );


        LinearLayout.LayoutParams separatorParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                );


        junkFileContainer.addView(
                separator,
                separatorParams
        );
    }


    // =========================================================
    // UPDATE SELECTED FILE TEXT
    // =========================================================

    private void updateSelectedText() {


        long selectedSize = 0;


        for (File file : selectedFiles) {


            if (file.exists() &&
                    file.isFile()) {


                selectedSize +=
                        file.length();
            }
        }


        selectedFileText.setText(
                selectedFiles.size() +
                        " Files Selected (" +
                        formatSize(selectedSize) +
                        ")"
        );
    }


    // =========================================================
    // DELETE SELECTED FILES/FOLDERS DIRECTLY
    // =========================================================

    // =========================================================
// DELETE SELECTED FILES / FOLDERS
// =========================================================

    private void deleteSelectedFiles() {

        // ---------------------------------------------------------
        // CHECK WHETHER USER SELECTED ANYTHING
        // ---------------------------------------------------------

        if (selectedFiles.isEmpty()) {

            Toast.makeText(
                    this,
                    "Select files to delete",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // ---------------------------------------------------------
        // CHECK ALL FILES ACCESS AGAIN
        // ---------------------------------------------------------

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (!Environment.isExternalStorageManager()) {

                Toast.makeText(
                        this,
                        "Allow All Files Access before deleting files",
                        Toast.LENGTH_LONG
                ).show();

                try {

                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                    );

                    intent.setData(
                            android.net.Uri.parse(
                                    "package:" + getPackageName()
                            )
                    );

                    startActivity(intent);

                } catch (Exception e) {

                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    );

                    startActivity(intent);
                }

                return;
            }
        }


        // ---------------------------------------------------------
        // COUNTERS
        // ---------------------------------------------------------

        int deleted = 0;
        int failed = 0;


        // ---------------------------------------------------------
        // COPY SELECTED FILES
        // ---------------------------------------------------------

        ArrayList<File> filesToDelete =
                new ArrayList<>(selectedFiles);


        // ---------------------------------------------------------
        // DELETE EACH SELECTED ITEM
        // ---------------------------------------------------------

        for (File file : filesToDelete) {

            if (file == null) {
                failed++;
                continue;
            }


            try {

                // -------------------------------------------------
                // FILE ALREADY DOES NOT EXIST
                // -------------------------------------------------

                if (!file.exists()) {

                    // It is already gone from storage.
                    deleted++;
                    continue;
                }


                // -------------------------------------------------
                // DIRECTORY
                // -------------------------------------------------

                if (file.isDirectory()) {

                    /*
                     * Your scanner only adds EMPTY folders.
                     *
                     * File.delete() can delete an empty directory.
                     */

                    File[] children = file.listFiles();

                    if (children != null && children.length > 0) {

                        failed++;
                        continue;
                    }
                }


                // -------------------------------------------------
                // TRY DELETE
                // -------------------------------------------------

                boolean deletedSuccessfully =
                        file.delete();


                // -------------------------------------------------
                // VERIFY ACTUAL DELETION
                // -------------------------------------------------

                if (deletedSuccessfully && !file.exists()) {

                    deleted++;

                } else {

                    /*
                     * delete() returned false or the file still exists.
                     */
                    failed++;
                }


            } catch (SecurityException e) {

                // Android denied the deletion.
                failed++;

            } catch (Exception e) {

                failed++;
            }
        }


        // ---------------------------------------------------------
        // CLEAR OLD SELECTION
        // ---------------------------------------------------------

        selectedFiles.clear();


        // ---------------------------------------------------------
        // UPDATE SELECTED TEXT
        // ---------------------------------------------------------

        updateSelectedText();


        // ---------------------------------------------------------
        // SHOW RESULT
        // ---------------------------------------------------------

        if (deleted > 0 && failed == 0) {

            Toast.makeText(
                    this,
                    deleted + " junk item(s) deleted successfully",
                    Toast.LENGTH_SHORT
            ).show();

        } else if (deleted > 0) {

            Toast.makeText(
                    this,
                    deleted + " deleted, " +
                            failed +
                            " could not be deleted",
                    Toast.LENGTH_LONG
            ).show();

        } else {

            Toast.makeText(
                    this,
                    "No files were deleted. Check All Files Access permission.",
                    Toast.LENGTH_LONG
            ).show();
        }


        // ---------------------------------------------------------
        // SCAN AGAIN
        // ---------------------------------------------------------

        scanStorage();
    }


    // =========================================================
    // NO JUNK FOUND
    // =========================================================

    private void showNoFiles() {


        TextView message =
                new TextView(this);


        message.setText(
                "No potential junk files found."
        );


        message.setTextSize(16);


        message.setTextColor(
                Color.GRAY
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


        junkFileContainer.addView(
                message
        );
    }


    // =========================================================
    // FORMAT FILE SIZE
    // =========================================================

    private String formatSize(long size) {


        // -----------------------------------------------------
        // BYTES
        // -----------------------------------------------------

        if (size < 1024) {

            return size + " B";
        }


        // -----------------------------------------------------
        // KB
        // -----------------------------------------------------

        double kb =
                size / 1024.0;


        if (kb < 1024) {


            return String.format(
                    Locale.getDefault(),
                    "%.2f KB",
                    kb
            );
        }


        // -----------------------------------------------------
        // MB
        // -----------------------------------------------------

        double mb =
                kb / 1024.0;


        if (mb < 1024) {


            return String.format(
                    Locale.getDefault(),
                    "%.2f MB",
                    mb
            );
        }


        // -----------------------------------------------------
        // GB
        // -----------------------------------------------------

        double gb =
                mb / 1024.0;


        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                gb
        );
    }
}