package com.example.autofilemanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor; //Reads rows returned from Android's storage database
import android.net.Uri; // Identifies the actual file
import android.provider.MediaStore; //Android API used to access media/files
import android.view.View; //Allows us to show/hide views
import android.widget.TextView; // Used for "No files found" message
import androidx.recyclerview.widget.LinearLayoutManager; //Arranges files vertically
import androidx.recyclerview.widget.RecyclerView; //Displays many files efficiently

import java.util.ArrayList; //Stores the scanned files
import java.util.List;//Allows us to work with the file collection


public class DownloadFiles extends AppCompatActivity {

    //VARIABLE
    TextView backbtn;
    private RecyclerView recyclerView;
    private TextView emptyText;

    private DownloadFileAdaptor fileAdapter;

    private final List<FileItem> fileList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_download_files);

        //CONNET TO ID
        backbtn = findViewById(R.id.BackButton);
        recyclerView = findViewById(R.id.downloadRecyclerView);
        emptyText = findViewById(R.id.noDownloadsText);





        //BACK TO HOME DASHBOARD
        backbtn.setOnClickListener(view->{
            Intent intent = new Intent(DownloadFiles.this , MainActivity.class);
            startActivity(intent);
        });


        //DISPLAY THE DOWNLOADED FILES INSIDE DEVICE
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 🟩 CONNECT THE ADAPTER
        fileAdapter = new DownloadFileAdaptor(this, fileList);
        recyclerView.setAdapter(fileAdapter);
        loadDownloadFiles();


    }



    //FUNCTION TO LOAD DOWNLOADED FILES

    private void loadDownloadFiles() {

        fileList.clear();

        Uri collection = MediaStore.Files.getContentUri(
                MediaStore.VOLUME_EXTERNAL
        );

        String[] projection = {

                MediaStore.Files.FileColumns._ID,

                MediaStore.Files.FileColumns.DISPLAY_NAME,

                MediaStore.Files.FileColumns.SIZE,

                MediaStore.Files.FileColumns.MIME_TYPE,

                MediaStore.Files.FileColumns.DATE_MODIFIED,

                MediaStore.Files.FileColumns.RELATIVE_PATH
        };

        String selection =
                MediaStore.Files.FileColumns.RELATIVE_PATH + " LIKE ?";

        String[] selectionArgs = {
                "Download/%"
        };

        Cursor cursor = getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        );

        if (cursor != null) {

            int idColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns._ID
            );

            int nameColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.DISPLAY_NAME
            );

            int sizeColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.SIZE
            );

            int mimeColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.MIME_TYPE
            );

            int dateColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.DATE_MODIFIED
            );

            int pathColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.RELATIVE_PATH
            );

            while (cursor.moveToNext()) {

                long id = cursor.getLong(idColumn);

                String name = cursor.getString(nameColumn);

                long size = cursor.getLong(sizeColumn);

                String mimeType = cursor.getString(mimeColumn);

                long dateModified = cursor.getLong(dateColumn);

                String relativePath = cursor.getString(pathColumn);

                Uri fileUri = Uri.withAppendedPath(
                        collection,
                        String.valueOf(id)
                );

                FileItem fileItem = new FileItem(
                        name,
                        size,
                        mimeType,
                        dateModified,
                        relativePath,
                        fileUri
                );

                fileList.add(fileItem);
            }

            cursor.close();
        }

        fileAdapter.notifyDataSetChanged();

        if (fileList.isEmpty()) {

            recyclerView.setVisibility(View.GONE);

            emptyText.setVisibility(View.VISIBLE);

        } else {

            recyclerView.setVisibility(View.VISIBLE);

            emptyText.setVisibility(View.GONE);
        }
    }

}