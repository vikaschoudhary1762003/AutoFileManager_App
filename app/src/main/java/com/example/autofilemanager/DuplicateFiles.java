package com.example.autofilemanager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DuplicateFiles extends AppCompatActivity {


    // ==============================
    // VARIABLES
    // ==============================

    // BACK BUTTON
    TextView backbtn;

    // CONTAINER WHERE DUPLICATE FILES WILL BE DISPLAYED
    LinearLayout duplicateFileContainer;

    // TEXTVIEW TO SHOW TOTAL DUPLICATE STORAGE
    TextView duplicateStorageText;

    // TEXTVIEW TO SHOW TOTAL DUPLICATE FILE COUNT
    TextView duplicateCountText;

    // BUTTON TO DELETE SELECTED FILES
    Button deleteSelectedButton;

    // LIST TO STORE FILES SELECTED USING CHECKBOX
    List<File> selectedFiles = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // CONNECT XML FILE
        setContentView(R.layout.activity_duplicate_files);


        // ==============================
        // CONNECT VARIABLES TO XML IDs
        // ==============================

        // CONNECT BACK BUTTON
        backbtn =
                findViewById(R.id.BackButton);


        // CONNECT DUPLICATE FILE CONTAINER
        duplicateFileContainer =
                findViewById(R.id.duplicateFilesContainer);


        // CONNECT DUPLICATE STORAGE TEXT
        duplicateStorageText =
                findViewById(R.id.duplicateStorageText);


        // CONNECT DUPLICATE COUNT TEXT
        duplicateCountText =
                findViewById(R.id.duplicateCountText);


        // CONNECT DELETE BUTTON
        deleteSelectedButton =
                findViewById(R.id.deleteSelectedButton);


        // ==============================
        // FIND DUPLICATE FILES
        // ==============================

        findDuplicateFiles();


        // ==============================
        // BACK BUTTON
        // ==============================

        backbtn.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            DuplicateFiles.this,
                            MainActivity.class
                    );

            startActivity(intent);

            finish();
        });


        // ==============================
        // DELETE BUTTON
        // ==============================

        deleteSelectedButton.setOnClickListener(view -> {

            deleteSelectedFiles();

        });
    }


    // =========================================================
    // FUNCTION TO FIND DUPLICATE FILES
    // =========================================================

    private void findDuplicateFiles() {

        // CLEAR OLD FILES FROM SCREEN
        duplicateFileContainer.removeAllViews();

        // CLEAR PREVIOUS SELECTION
        selectedFiles.clear();

        // VARIABLE TO COUNT TOTAL DUPLICATE FILES
        int duplicateFileCount = 0;

        // VARIABLE TO COUNT TOTAL STORAGE
        // USED BY DUPLICATE FILES
        long duplicateStorage = 0;

        // ==============================
        // GET MAIN STORAGE DIRECTORY
        // ==============================

        File storage = Environment.getExternalStorageDirectory();


        // ==============================
        // CREATE MAP
        // ==============================

        // MAP WILL STORE:
        //
        // FILE HASH -> LIST OF FILES
        //
        // Example:
        //
        // ABC123 -> photo1.jpg, photo2.jpg
        //
        // Same hash means same file content.

        Map<String, List<File>> fileMap =
                new HashMap<>();


        // ==============================
        // SCAN ALL FILES
        // ==============================

        scanFiles(
                storage,
                fileMap
        );


        // ==============================
        // CHECK DUPLICATES
        // ==============================

        boolean duplicateFound = false;


        // LOOP THROUGH EVERY HASH
        for (Map.Entry<String, List<File>> entry :
                fileMap.entrySet()) {


            // GET FILES HAVING SAME HASH
            List<File> duplicateList =
                    entry.getValue();


            // MORE THAN ONE FILE
            // MEANS DUPLICATE FILES
            if (duplicateList.size() > 1) {


                // DUPLICATE FOUND
                duplicateFound = true;


                // COUNT ALL FILES IN THIS GROUP
                duplicateFileCount +=
                        duplicateList.size();


                // ==============================
                // DISPLAY EACH DUPLICATE FILE
                // ==============================

                for (File file :
                        duplicateList) {


                    // ADD FILE SIZE
                    // TO TOTAL DUPLICATE STORAGE
                    duplicateStorage +=
                            file.length();


                    // ==============================
                    // CREATE CHECKBOX
                    // ==============================

                    CheckBox fileCheckBox =
                            new CheckBox(this);


                    // ==============================
                    // DISPLAY FILE INFORMATION
                    // ==============================

                    fileCheckBox.setText(
                            "📄 " + file.getName() +
                                    "\nPath: " +
                                    file.getAbsolutePath() +
                                    "\nSize: " +
                                    convertSize(
                                            file.length()
                                    )
                    );


                    // TEXT SIZE
                    fileCheckBox.setTextSize(16);


                    // SPACE AROUND CHECKBOX
                    fileCheckBox.setPadding(
                            30,
                            25,
                            30,
                            25
                    );


                    // ==============================
                    // CHECKBOX CLICK
                    // ==============================

                    fileCheckBox.setOnCheckedChangeListener(
                            (buttonView, isChecked) -> {


                                // IF CHECKED
                                if (isChecked) {


                                    // ADD FILE TO SELECTED LIST
                                    if (!selectedFiles.contains(file)) {

                                        selectedFiles.add(file);
                                    }

                                }

                                // IF UNCHECKED
                                else {

                                    // REMOVE FILE
                                    // FROM SELECTED LIST
                                    selectedFiles.remove(file);
                                }
                            }
                    );


                    // ==============================
                    // ADD CHECKBOX TO SCREEN
                    // ==============================

                    duplicateFileContainer.addView(
                            fileCheckBox
                    );
                }
            }
        }


        // ==============================
        // DISPLAY DUPLICATE FILE COUNT
        // ==============================

        duplicateCountText.setText(
                String.valueOf(
                        duplicateFileCount
                )
        );


        // ==============================
        // DISPLAY DUPLICATE STORAGE
        // ==============================

        duplicateStorageText.setText(
                convertSize(
                        duplicateStorage
                )
        );


        // ==============================
        // IF NO DUPLICATES
        // ==============================

        if (!duplicateFound) {

            showNoDuplicateMessage();
        }
    }


    // =========================================================
    // FUNCTION TO DELETE SELECTED FILES
    // =========================================================

    private void deleteSelectedFiles() {


        // ==============================
        // CHECK WHETHER FILE IS SELECTED
        // ==============================

        if (selectedFiles.isEmpty()) {


            Toast.makeText(
                    this,
                    "Please select files to delete",
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        // ==============================
        // COUNT DELETED FILES
        // ==============================

        int deletedCount = 0;


        // ==============================
        // DELETE SELECTED FILES
        // ==============================

        for (File file :
                new ArrayList<>(selectedFiles)) {


            // CHECK FILE EXISTS
            if (file.exists()) {


                // DELETE FILE
                if (file.delete()) {

                    deletedCount++;
                }
            }
        }


        // ==============================
        // CLEAR SELECTED FILES
        // ==============================

        selectedFiles.clear();


        // ==============================
        // SHOW DELETE RESULT
        // ==============================

        Toast.makeText(
                this,
                deletedCount +
                        " file(s) deleted",
                Toast.LENGTH_SHORT
        ).show();


        // ==============================
        // SCAN AGAIN
        // ==============================

        findDuplicateFiles();
    }


    // =========================================================
    // FUNCTION TO SCAN FILES RECURSIVELY
    // =========================================================

    private void scanFiles(
            File directory,
            Map<String, List<File>> fileMap) {


        // ==============================
        // CHECK DIRECTORY
        // ==============================

        if (directory == null ||
                !directory.exists()) {

            return;
        }


        // ==============================
        // GET FILES AND FOLDERS
        // ==============================

        File[] files =
                directory.listFiles();


        // ==============================
        // NOTHING FOUND
        // ==============================

        if (files == null) {

            return;
        }


        // ==============================
        // LOOP THROUGH FILES
        // ==============================

        for (File file :
                files) {


            // ==============================
            // IF DIRECTORY
            // ==============================

            if (file.isDirectory()) {


                // SCAN DIRECTORY AGAIN
                scanFiles(
                        file,
                        fileMap
                );
            }


            // ==============================
            // IF FILE
            // ==============================

            else if (file.isFile()) {


                try {


                    // ==============================
                    // CALCULATE FILE HASH
                    // ==============================

                    String hash =
                            calculateHash(file);


                    // ==============================
                    // CREATE NEW LIST
                    // IF HASH DOES NOT EXIST
                    // ==============================

                    if (!fileMap.containsKey(hash)) {

                        fileMap.put(
                                hash,
                                new ArrayList<>()
                        );
                    }


                    // ==============================
                    // ADD FILE TO HASH GROUP
                    // ==============================

                    fileMap.get(hash).add(file);


                } catch (Exception e) {


                    // IGNORE FILES
                    // THAT CANNOT BE READ

                    e.printStackTrace();
                }
            }
        }
    }


    // =========================================================
    // FUNCTION TO CALCULATE SHA-256 HASH
    // =========================================================

    private String calculateHash(File file)
            throws Exception {


        // ==============================
        // CREATE SHA-256 DIGEST
        // ==============================

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );


        // ==============================
        // OPEN FILE
        // ==============================

        FileInputStream inputStream =
                new FileInputStream(file);


        // ==============================
        // CREATE BUFFER
        // ==============================

        byte[] buffer =
                new byte[8192];


        int bytesRead;


        // ==============================
        // READ FILE CONTENT
        // ==============================

        while ((bytesRead =
                inputStream.read(buffer)) != -1) {


            // UPDATE HASH
            digest.update(
                    buffer,
                    0,
                    bytesRead
            );
        }


        // ==============================
        // CLOSE FILE
        // ==============================

        inputStream.close();


        // ==============================
        // GET FINAL HASH
        // ==============================

        byte[] hashBytes =
                digest.digest();


        // ==============================
        // CONVERT HASH TO STRING
        // ==============================

        StringBuilder hashString =
                new StringBuilder();


        for (byte b :
                hashBytes) {


            hashString.append(
                    String.format(
                            "%02x",
                            b
                    )
            );
        }


        return hashString.toString();
    }


    // =========================================================
    // FUNCTION TO DISPLAY NO DUPLICATE MESSAGE
    // =========================================================

    private void showNoDuplicateMessage() {


        TextView message =
                new TextView(this);


        message.setText(
                "No Duplicate Files Found"
        );


        message.setTextSize(16);


        message.setGravity(
                Gravity.CENTER
        );


        message.setPadding(
                20,
                50,
                20,
                50
        );


        duplicateFileContainer.addView(
                message
        );
    }


    // =========================================================
    // FUNCTION TO CONVERT BYTES INTO KB / MB / GB
    // =========================================================

    private String convertSize(long bytes) {


        // ==============================
        // BYTES
        // ==============================

        if (bytes < 1024) {

            return bytes + " B";
        }


        // ==============================
        // KB
        // ==============================

        if (bytes < 1024 * 1024) {

            return String.format(
                    "%.2f KB",
                    bytes / 1024.0
            );
        }


        // ==============================
        // MB
        // ==============================

        if (bytes <
                1024L * 1024L * 1024L) {

            return String.format(
                    "%.2f MB",
                    bytes /
                            (1024.0 * 1024.0)
            );
        }


        // ==============================
        // GB
        // ==============================

        return String.format(
                "%.2f GB",
                bytes /
                        (1024.0 *
                                1024.0 *
                                1024.0)
        );
    }
}