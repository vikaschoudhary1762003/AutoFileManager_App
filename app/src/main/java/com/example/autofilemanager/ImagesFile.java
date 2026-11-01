package com.example.autofilemanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


// IMAGES FILE ACTIVITY
public class ImagesFile extends AppCompatActivity {

    // Back button from XML.
    private TextView backbtn;

    // Container where image items will be added.
    private LinearLayout image_Container;

    // Image permission request number.
    private static final int IMAGE_PERMISSION = 101;

    // ON CREATE
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_images_file);

        // CONNECT XML IDs
        backbtn = findViewById(R.id.BackButton);
        image_Container = findViewById(R.id.imageFileContainer);

        // CHECK IMAGE PERMISSION
        checkPermission();
        // BACK BUTTON

        backbtn.setOnClickListener(view -> {
            Intent intent = new Intent(ImagesFile.this, MainActivity.class);
            startActivity(intent);
            // Close this Activity.
            finish();
        });
    }


    // CHECK IMAGE PERMISSION

    private void checkPermission() {

        String permission;

        /*
         * Android 13 / API 33 and above:
         *
         * READ_MEDIA_IMAGES is used instead of
         * READ_EXTERNAL_STORAGE.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            /*
             * Android 12 and below:
             *
             * READ_EXTERNAL_STORAGE is used.
             */
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        // CHECK WHETHER PERMISSION ALREADY EXISTS
        if (ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted.
            loadImageFiles();
        } else {
            // Permission not granted.
            ActivityCompat.requestPermissions(this, new String[]{permission}, IMAGE_PERMISSION);
        }
    }

    // PERMISSION RESULT
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Make sure this result belongs to our request.
        if (requestCode == IMAGE_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                loadImageFiles();
            } else {
                // Permission denied.
                Toast.makeText(this, "Image Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // LOAD IMAGE FILES INTO IMAGE PAGE
    private void loadImageFiles() {
        /*
         * Clear the container first.
         *
         * Your XML already contains some separator Views.
         * Removing them prevents duplicate/old content.
         */
        image_Container.removeAllViews();
        // MEDIASTORE COLUMNS
        String[] configurations = {
                // Image ID.
                MediaStore.Images.Media._ID,
                // Image filename.
                MediaStore.Images.Media.DISPLAY_NAME,
                // Image size.
                MediaStore.Images.Media.SIZE
        };

        // QUERY MEDIASTORE
        Cursor cursor = getContentResolver().query(
                //We want images stored in external/shared storage.
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                 // Information we want.
                configurations,
                 // No WHERE condition.
                null,
                 // No selection arguments.
                 null,
                //Newest images first.
                MediaStore.Images.Media.DATE_ADDED + " DESC");

        // If MediaStore returned nothing.
        if (cursor == null) {
            showMessage();
            return;
        }

        // VARIABLES FOR SUMMARY
        int imageCount = 0;
        long totalSize = 0;
        // READ EVERY IMAGE

        while (cursor.moveToNext()) {
            imageCount++;
            // GET IMAGE ID
            long imageId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));

            // GET IMAGE NAME
            String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));

            // GET IMAGE SIZE

            long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
            // Add size to total.
            totalSize += size;

            // CREATE IMAGE URI

            /*
             * This is the important part.
             *
             * We create the URI of the actual image so that
             * ImageView can load its thumbnail.
             */
            Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(imageId));

            // CREATE IMAGE ITEM
            LinearLayout imageItem = new LinearLayout(this);
            imageItem.setOrientation(LinearLayout.HORIZONTAL);
            imageItem.setGravity(Gravity.CENTER_VERTICAL);
            imageItem.setPadding(10, 15, 10, 15);

            // IMAGE THUMBNAIL
            ImageView imageView = new ImageView(this);

            // Set the thumbnail size.
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(80, 80);
            imageParams.setMargins(0,0, 20, 0);
            imageView.setLayoutParams(imageParams);

             //CENTER_CROP makes the thumbnail fill the
             //ImageView nicely.
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

             //Load the actual image from MediaStore.
            imageView.setImageURI(imageUri);
            // Add ImageView to item.
            imageItem.addView(imageView);

            // TEXT INFORMATION
            LinearLayout textContainer = new LinearLayout(this);
            textContainer.setOrientation(LinearLayout.VERTICAL);

             //Text container takes remaining width.
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            textContainer.setLayoutParams(textParams);

            // IMAGE NAME
            TextView nameText = new TextView(this);
            nameText.setText(name);
            nameText.setTextSize(16);
            nameText.setTextColor(Color.rgb(30, 30, 30));
            // Add filename.
            textContainer.addView(nameText);
            // IMAGE SIZE
            TextView sizeText = new TextView(this);
            sizeText.setText("Size: " + ConvertSize(size));
            sizeText.setTextSize(13);
            sizeText.setTextColor(Color.GRAY);

            // Add size.
            textContainer.addView(sizeText);
            // Add text container to item.
            imageItem.addView(textContainer);

            // ADD COMPLETE ITEM TO SCREE
            image_Container.addView(imageItem);

            // SEPARATOR
            TextView separator = new TextView(this);
            separator.setBackgroundColor(Color.rgb(238, 238, 238));
            LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            separator.setLayoutParams(separatorParams);
            image_Container.addView(separator);
        }

        // Close cursor.
        cursor.close();

        // UPDATE SUMMARY
        updateSummary(imageCount, totalSize);

        // NO IMAGES
        if (imageCount == 0) {
            showMessage();
        }
    }


    // UPDATE IMAGE SUMMARY
    private void updateSummary(int count, long totalSize) {

         // Get the count TextView from XML.
        TextView imageCountText = findViewById(R.id.imageCount);

         // Get total size TextView from XML.
        TextView imageTotalSize = findViewById(R.id.imageTotalSize);

         //Bottom count TextView.
        TextView bottomCount = findViewById(R.id.imageFileCountBottom);
        // Update count.
        imageCountText.setText(count + " Files");

        // Update total size.
        imageTotalSize.setText(ConvertSize(totalSize));

        // Update bottom count.
        bottomCount.setText(count + " files");
    }


    // NO IMAGE MESSAGE

    private void showMessage() {
        TextView noFile = new TextView(this);
        noFile.setText("No Image File Found !");
        noFile.setTextSize(16);
        noFile.setTextColor(Color.GRAY);
        noFile.setGravity(Gravity.CENTER);
        noFile.setPadding(20, 50, 20, 50);
        image_Container.addView(noFile);
    }


    // CONVERT BYTES TO KB / MB / GB
    private String ConvertSize(long bytes) {

        // Less than 1 KB.
        if (bytes < 1024) {
            return bytes + " B";
        }

        // Less than 1 MB.
        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }

        // Less than 1 GB.
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }

        // 1 GB or larger.
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}